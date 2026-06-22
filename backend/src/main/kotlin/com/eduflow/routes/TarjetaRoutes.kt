package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.GroqService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*

fun Routing.tarjetaRoutes() {
    // POST /tarjetas/generar
    // Body: { "materiaId": 1, "examenId": 2, "texto": "...", "imagenBase64": "..." }
    // imagenBase64 es opcional. Si viene, se manda a Llama 4 Scout con vision.
    // Si solo viene texto, se manda como prompt de texto normal.
    post("/tarjetas/generar") {
        val req = call.receive<GenerarTarjetasRequest>()

        // Llamar a Groq segun si hay imagen o no
        val tarjetasTexto = if (req.imagenBase64 != null) {
            GroqService.generarTarjetasDesdeImagen(
                req.imagenBase64, req.texto ?: "", req.materia
            )
        } else {
            GroqService.generarTarjetasDesdeTexto(
                req.texto ?: "", req.materia
            )
        }

        // Antes esto se parseaba con un regex que exigia el JSON pegado
        // sin espacios; ahora usamos un parser real que tolera el formato
        // que de verdad devuelve el modelo (con saltos de linea, etc).
        val pares = GroqService.extraerTarjetas(tarjetasTexto)

        if (pares.isEmpty()) {
            call.respond(HttpStatusCode.UnprocessableEntity, mapOf(
                "error" to "La IA no devolvió tarjetas en un formato válido. Intenta de nuevo."
            ))
            return@post
        }

        val tarjetasGuardadas = pares.map { (pregunta, respuesta) ->
            val temaFinal = req.tema?.trim()?.takeIf { it.isNotEmpty() } ?: "General"
            val id = transaction {
                Tarjetas.insertAndGetId {
                    it[Tarjetas.materiaId] = req.materiaId
                    it[Tarjetas.examenId]  = if (req.examenId > 0) req.examenId else null
                    it[Tarjetas.pregunta]  = pregunta
                    it[Tarjetas.respuesta] = respuesta
                    it[Tarjetas.tema]      = temaFinal
                }.value
            }
            TarjetaDto(id, pregunta, respuesta, temaFinal, false)
        }

        call.respond(HttpStatusCode.Created,
            GenerarTarjetasResponse(tarjetasGuardadas, tarjetasGuardadas.size))
    }

    // GET /tarjetas/{materiaId} - lista tarjetas activas de una materia
    get("/tarjetas/{materiaId}") {
        val materiaId = call.parameters["materiaId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val lista = transaction {
            Tarjetas.selectAll().where { Tarjetas.materiaId eq materiaId }
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

    // PUT /tarjetas/completar - marca como estudiada toda una subcarpeta/tema
    // Body: { "materiaId": 1, "tema": "Matrices" }
    put("/tarjetas/completar") {
        val req = call.receive<CompletarTemaRequest>()
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
