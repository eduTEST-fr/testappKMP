package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.util.AppClock
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Routing.examenRoutes() {
    // POST /materias/{id}/examenes - agrega un examen únicamente a una materia propia.
    post("/materias/{id}/examenes") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val materiaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@post
        }
        val req = call.receive<ExamenRequest>()
        val nombre = req.nombre.trim().take(50)
        val fecha = try { LocalDate.parse(req.fecha) } catch (_: Exception) { null }

        if (nombre.isBlank() || fecha == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Selecciona tipo y fecha del examen"))
            return@post
        }
        if (fecha.isBefore(AppClock.hoy())) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La fecha no puede estar en el pasado"))
            return@post
        }

        val fechaConfirmada = fecha!!

        val resultado = transaction {
            val pertenece = Materias.selectAll()
                .where { (Materias.id eq materiaId) and (Materias.usuarioId eq userId) }
                .firstOrNull() != null
            if (!pertenece) return@transaction null

            val id = Examenes.insertAndGetId {
                it[Examenes.materiaId] = materiaId
                it[Examenes.nombre] = nombre
                it[Examenes.fecha] = fechaConfirmada
            }.value
            ExamenResponse(id, nombre, fechaConfirmada.toString())
        }

        if (resultado == null) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "La materia no pertenece al usuario"))
        } else {
            call.respond(HttpStatusCode.Created, resultado)
        }
    }

    // GET /materias/{id}/examenes - lista los exámenes de una materia propia.
    get("/materias/{id}/examenes") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val materiaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val lista = transaction {
            val pertenece = Materias.selectAll()
                .where { (Materias.id eq materiaId) and (Materias.usuarioId eq userId) }
                .firstOrNull() != null
            if (!pertenece) return@transaction null

            Examenes.selectAll().where { Examenes.materiaId eq materiaId }
                .orderBy(Examenes.fecha, SortOrder.ASC)
                .map {
                    ExamenResponse(
                        id = it[Examenes.id].value,
                        nombre = it[Examenes.nombre],
                        fecha = it[Examenes.fecha].toString()
                    )
                }
        }
        if (lista == null) call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Materia no disponible"))
        else call.respond(lista)
    }
}
