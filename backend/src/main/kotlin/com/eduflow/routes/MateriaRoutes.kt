package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.AuthService
import com.eduflow.util.AppClock
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate

fun Routing.materiaRoutes() {
    // GET /materias - lista las materias del usuario autenticado
    get("/materias") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val lista = transaction {
            Materias.selectAll().where { Materias.usuarioId eq userId }
                .orderBy(Materias.createdAt, SortOrder.ASC)
                .map { MateriaDto(
                    it[Materias.id].value,
                    it[Materias.nombre],
                    it[Materias.dificultad]
                )}
        }
        call.respond(lista)
    }

    // POST /materias - crea una materia y su primera fecha de examen del mes.
    // Se hace en una sola transacción para no dejar materias incompletas.
    post("/materias") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<MateriaRequest>()
        val nombreLimpio = req.nombre.trim().take(100)
        val examenNombre = req.examenNombre?.trim()?.take(50).orEmpty()
        val fechaExamen = try {
            req.examenFecha?.let { LocalDate.parse(it) }
        } catch (_: Exception) { null }
        val hoy = AppClock.hoy()

        when {
            nombreLimpio.isBlank() || req.dificultad !in 1..10 -> {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nombre o dificultad inválidos"))
                return@post
            }
            examenNombre.isBlank() || fechaExamen == null -> {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Selecciona la evaluación y su fecha"))
                return@post
            }
            fechaExamen.isBefore(hoy) -> {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La fecha del examen no puede estar en el pasado"))
                return@post
            }
            fechaExamen.year != hoy.year || fechaExamen.month != hoy.month -> {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La primera evaluación debe pertenecer al mes actual"))
                return@post
            }
        }

        val fechaConfirmada = fechaExamen!!

        val id = transaction {
            val materiaId = Materias.insertAndGetId {
                it[usuarioId]  = userId
                it[nombre]     = nombreLimpio
                it[dificultad] = req.dificultad
            }.value
            Examenes.insert {
                it[Examenes.materiaId] = materiaId
                it[Examenes.nombre] = examenNombre
                it[Examenes.fecha] = fechaConfirmada
            }
            materiaId
        }
        call.respond(HttpStatusCode.Created, MateriaDto(id, nombreLimpio, req.dificultad))
    }

    // DELETE /materias/{id}
    delete("/materias/{id}") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@delete
        }
        val materiaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@delete
        }
        transaction {
            Materias.deleteWhere {
                (Materias.id eq materiaId) and (Materias.usuarioId eq userId)
            }
        }
        call.respond(HttpStatusCode.NoContent)
    }
}

fun obtenerUserId(call: io.ktor.server.application.ApplicationCall): Int? {
    val token = call.request.headers["Authorization"]
        ?.removePrefix("Bearer ") ?: return null
    return AuthService.verificarToken(token)
}

fun obtenerRol(call: io.ktor.server.application.ApplicationCall): String {
    val token = call.request.headers["Authorization"]
        ?.removePrefix("Bearer ") ?: return "ALUMNO"
    return AuthService.obtenerRol(token)
}
