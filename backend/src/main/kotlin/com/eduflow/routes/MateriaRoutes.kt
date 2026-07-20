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
        // Antes esto era un mapOf generico; el serializador por defecto de
        // Ktor podia variar el espaciado del JSON resultante y eso rompia
        // el regex del frontend que esperaba el formato pegado sin espacios.
        // Con un DTO @Serializable el JSON siempre sale consistente.
        val lista = transaction {
            Materias.selectAll().where { Materias.usuarioId eq userId }
                .map { MateriaDto(
                    it[Materias.id].value,
                    it[Materias.nombre],
                    it[Materias.dificultad]
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
        val nombreLimpio = req.nombre.trim().take(100)
        if (nombreLimpio.isBlank() || req.dificultad !in 1..10) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nombre o dificultad inválidos"))
            return@post
        }
        val id = transaction {
            Materias.insertAndGetId {
                it[usuarioId]  = userId
                it[nombre]     = nombreLimpio
                it[dificultad] = req.dificultad
            }.value
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

// Funcion auxiliar compartida: extrae el userId del JWT
fun obtenerUserId(call: io.ktor.server.application.ApplicationCall): Int? {
    val token = call.request.headers["Authorization"]
        ?.removePrefix("Bearer ") ?: return null
    return AuthService.verificarToken(token)
}

// Funcion auxiliar compartida: extrae el rol del JWT (ALUMNO por defecto)
fun obtenerRol(call: io.ktor.server.application.ApplicationCall): String {
    val token = call.request.headers["Authorization"]
        ?.removePrefix("Bearer ") ?: return "ALUMNO"
    return AuthService.obtenerRol(token)
}
