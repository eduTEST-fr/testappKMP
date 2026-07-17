package com.eduflow.routes

import com.eduflow.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*

@Serializable
data class RedApoyoRequest(
    val tipo: String,
    val materia: String,
    val mensaje: String
)

fun Routing.redApoyoRoutes() {
    // GET /red-apoyo - lista mensajes activos
    get("/red-apoyo") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val lista = transaction {
            RedApoyo.selectAll().where { RedApoyo.activo eq true }
                .map { RedApoyoDto(
                    id      = it[RedApoyo.id].value,
                    tipo    = it[RedApoyo.tipo],
                    materia = it[RedApoyo.materia],
                    mensaje = it[RedApoyo.mensaje]
                )}
        }
        call.respond(lista)
    }

    // POST /red-apoyo - publica mensaje
    post("/red-apoyo") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<RedApoyoRequest>()
        transaction {
            RedApoyo.insert {
                it[RedApoyo.usuarioId] = userId
                it[RedApoyo.tipo]      = req.tipo
                it[RedApoyo.materia]   = req.materia
                it[RedApoyo.mensaje]   = req.mensaje
            }
        }
        call.respond(HttpStatusCode.Created, mapOf("ok" to true))
    }
}
