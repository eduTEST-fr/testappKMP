package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.GroqService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Routing.tarjetaRoutes() {
    // POST /tarjetas/generar — genera tarjetas solo para una materia propia y habilitada.
    post("/tarjetas/generar") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<GenerarTarjetasRequest>()
        if (!validarAccesoMateriales(call, userId, req.materiaId)) return@post

        val materiaReal = transaction {
            Materias.selectAll()
                .where { (Materias.id eq req.materiaId) and (Materias.usuarioId eq userId) }
                .singleOrNull()?.get(Materias.nombre)
        } ?: run {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Materia no disponible"))
            return@post
        }

        val examenValido = if (req.examenId > 0) transaction {
            Examenes.selectAll()
                .where { (Examenes.id eq req.examenId) and (Examenes.materiaId eq req.materiaId) }
                .firstOrNull() != null
        } else true
        if (!examenValido) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "El examen no pertenece a la materia"))
            return@post
        }

        val tarjetasTexto = if (req.imagenBase64 != null) {
            GroqService.generarTarjetasDesdeImagen(req.imagenBase64, req.texto ?: "", materiaReal)
        } else {
            GroqService.generarTarjetasDesdeTexto(req.texto ?: "", materiaReal)
        }

        val pares = GroqService.extraerTarjetas(tarjetasTexto)
        if (pares.isEmpty()) {
            call.respond(HttpStatusCode.UnprocessableEntity, mapOf(
                "error" to "La IA no devolvió tarjetas en un formato válido. Intenta de nuevo."
            ))
            return@post
        }

        val temaFinal = req.tema?.trim()?.takeIf { it.isNotEmpty() }?.take(150) ?: "General"
        val tarjetasGuardadas = pares.map { (pregunta, respuesta) ->
            val id = transaction {
                Tarjetas.insertAndGetId {
                    it[Tarjetas.materiaId] = req.materiaId
                    it[Tarjetas.examenId] = if (req.examenId > 0) req.examenId else null
                    it[Tarjetas.pregunta] = pregunta
                    it[Tarjetas.respuesta] = respuesta
                    it[Tarjetas.tema] = temaFinal
                }.value
            }
            TarjetaDto(id, pregunta, respuesta, temaFinal, false)
        }

        call.respond(HttpStatusCode.Created,
            GenerarTarjetasResponse(tarjetasGuardadas, tarjetasGuardadas.size))
    }

    // GET /tarjetas/{materiaId} - lista tarjetas únicamente si el mes está configurado
    // y no es el día del examen.
    get("/tarjetas/{materiaId}") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val materiaId = call.parameters["materiaId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        if (!validarAccesoMateriales(call, userId, materiaId)) return@get

        val lista = transaction {
            Tarjetas.selectAll().where { Tarjetas.materiaId eq materiaId }
                .orderBy(Tarjetas.createdAt, SortOrder.DESC)
                .map { TarjetaDto(
                    it[Tarjetas.id].value,
                    it[Tarjetas.pregunta],
                    it[Tarjetas.respuesta],
                    it[Tarjetas.tema],
                    it[Tarjetas.completado]
                )}
        }
        call.respond(lista)
    }

    // PUT /tarjetas/completar - marca como estudiada una subcarpeta propia y habilitada.
    put("/tarjetas/completar") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@put
        }
        val req = call.receive<CompletarTemaRequest>()
        if (!validarAccesoMateriales(call, userId, req.materiaId)) return@put

        transaction {
            Tarjetas.update({
                (Tarjetas.materiaId eq req.materiaId) and (Tarjetas.tema eq req.tema)
            }) {
                it[Tarjetas.completado] = true
            }
        }
        call.respond(HttpStatusCode.OK, mapOf("ok" to true))
    }
}
