package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.NotificacionService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Routing.sesionRoutes() {
    // POST /sesiones — registra una sesión únicamente sobre una materia del usuario autenticado.
    post("/sesiones") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<GuardarSesionEstudioRequest>()
        val fecha = runCatching { LocalDate.parse(req.fecha) }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Fecha inválida"))
            return@post
        }
        if (req.duracionSegundos !in 1..43_200) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Duración inválida"))
            return@post
        }

        val sesion = transaction {
            val materia = Materias.selectAll()
                .where {
                    (Materias.id eq req.materiaId) and
                        (Materias.usuarioId eq userId)
                }
                .firstOrNull() ?: return@transaction null

            val id = SesionesEstudio.insertAndGetId {
                it[usuarioId] = userId
                it[materiaId] = req.materiaId
                it[SesionesEstudio.fecha] = fecha
                it[duracionSegundos] = req.duracionSegundos
                it[metaMinutos] = req.metaMinutos.coerceAtLeast(0)
            }.value

            val dto = SesionEstudioDto(
                id = id,
                materiaId = req.materiaId,
                materiaNombre = materia[Materias.nombre],
                fecha = fecha.toString(),
                duracionSegundos = req.duracionSegundos,
                metaMinutos = req.metaMinutos.coerceAtLeast(0),
                createdAt = java.time.Instant.now().toString()
            )
            val minutos = req.duracionSegundos / 60
            val segundos = req.duracionSegundos % 60
            NotificacionService.crear(
                usuarioId = userId,
                titulo = "Sesión de estudio guardada",
                contenido = "Registraste ${minutos}m ${segundos}s en ${materia[Materias.nombre]}.",
                tipo = "SESION_ESTUDIO",
                referenciaId = id,
                clave = "SESION_ESTUDIO_$id"
            )
            dto
        }

        if (sesion == null) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "La materia no pertenece al usuario"))
        } else {
            call.respond(HttpStatusCode.Created, sesion)
        }
    }

    // GET /sesiones/materia/{id} — historial y tiempo acumulado de una materia.
    get("/sesiones/materia/{id}") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val materiaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }

        val historial = transaction {
            val materia = Materias.selectAll()
                .where { (Materias.id eq materiaId) and (Materias.usuarioId eq userId) }
                .firstOrNull() ?: return@transaction null
            val rows = SesionesEstudio.selectAll()
                .where {
                    (SesionesEstudio.usuarioId eq userId) and
                        (SesionesEstudio.materiaId eq materiaId)
                }
                .orderBy(SesionesEstudio.createdAt, SortOrder.DESC)
                .toList()
            HistorialSesionesDto(
                sesiones = rows.take(30).map { row ->
                    SesionEstudioDto(
                        id = row[SesionesEstudio.id].value,
                        materiaId = materiaId,
                        materiaNombre = materia[Materias.nombre],
                        fecha = row[SesionesEstudio.fecha].toString(),
                        duracionSegundos = row[SesionesEstudio.duracionSegundos],
                        metaMinutos = row[SesionesEstudio.metaMinutos],
                        createdAt = row[SesionesEstudio.createdAt].toString()
                    )
                },
                totalSegundos = rows.sumOf { it[SesionesEstudio.duracionSegundos].toLong() }
            )
        }

        if (historial == null) call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Materia no disponible"))
        else call.respond(historial)
    }
}
