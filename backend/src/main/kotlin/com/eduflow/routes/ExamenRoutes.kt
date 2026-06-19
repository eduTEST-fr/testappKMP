package com.eduflow.routes

import com.eduflow.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Routing.examenRoutes() {
    // POST /materias/{id}/examenes - agrega examen a una materia
    post("/materias/{id}/examenes") {
        val materiaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@post
        }
        val req = call.receive<ExamenRequest>()
        transaction {
            Examenes.insert {
                it[Examenes.materiaId] = materiaId
                it[Examenes.nombre]    = req.nombre
                it[Examenes.fecha]     = LocalDate.parse(req.fecha)
            }
        }
        call.respond(HttpStatusCode.Created, mapOf("ok" to true))
    }

    // GET /materias/{id}/examenes - lista los examenes de una materia
    get("/materias/{id}/examenes") {
        val materiaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val lista = transaction {
            Examenes.select { Examenes.materiaId eq materiaId }
                .map { mapOf(
                    "id"     to it[Examenes.id].value,
                    "nombre" to it[Examenes.nombre],
                    "fecha"  to it[Examenes.fecha].toString()
                )}
        }
        call.respond(lista)
    }
}
