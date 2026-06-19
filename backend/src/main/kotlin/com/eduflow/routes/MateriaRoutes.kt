package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Routing.materiaRoutes() {
    // GET /materias - lista las materias del usuario autenticado
    get("/materias") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val lista = transaction {
            Materias.selectAll().where { Materias.usuarioId eq userId }
                .map { MateriaResponse(
                    id         = it[Materias.id].value,
                    nombre     = it[Materias.nombre],
                    dificultad = it[Materias.dificultad]
                )}
        }
        call.respond(lista)
    }

    // POST /materias - crea una nueva materia
    post("/materias") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<MateriaRequest>()
        val id = transaction {
            Materias.insertAndGetId {
                it[usuarioId]  = userId
                it[nombre]     = req.nombre
                it[dificultad] = req.dificultad
            }.value
        }
        call.respond(HttpStatusCode.Created, mapOf("id" to id))
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

// Funcion auxiliar compartida: extrae el userId del JWT
fun obtenerUserId(call: io.ktor.server.application.ApplicationCall): Int? {
    val token = call.request.headers["Authorization"]
        ?.removePrefix("Bearer ") ?: return null
    return AuthService.verificarToken(token)
}
