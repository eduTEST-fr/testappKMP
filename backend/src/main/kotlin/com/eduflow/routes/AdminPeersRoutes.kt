package com.eduflow.routes

import com.eduflow.model.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Routing.adminPeersRoutes() {

    // GET /admin/peers/solicitudes - todas las solicitudes (abiertas y cerradas)
    get("/admin/peers/solicitudes") {
        if (obtenerRol(call) != "ADMIN") {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acceso denegado"))
            return@get
        }
        val lista = transaction {
            PeersSolicitudes
                .innerJoin(Usuarios, { PeersSolicitudes.autorId }, { Usuarios.id })
                .selectAll()
                .orderBy(PeersSolicitudes.createdAt, SortOrder.DESC)
                .map {
                    SolicitudAdminDto(
                        id = it[PeersSolicitudes.id].value,
                        titulo = it[PeersSolicitudes.titulo],
                        estado = it[PeersSolicitudes.estado],
                        materia = it[PeersSolicitudes.materia] ?: "",
                        createdAt = it[PeersSolicitudes.createdAt].toString(),
                        autor = it[Usuarios.nombre]
                    )
                }
        }
        call.respond(lista)
    }

    // DELETE /admin/peers/solicitudes/{id} - eliminar cualquier solicitud (moderación)
    delete("/admin/peers/solicitudes/{id}") {
        if (obtenerRol(call) != "ADMIN") {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acceso denegado"))
            return@delete
        }
        val solicitudId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@delete
        }
        transaction { PeersSolicitudes.deleteWhere { PeersSolicitudes.id eq solicitudId } }
        call.respond(mapOf("mensaje" to "Solicitud eliminada por administrador"))
    }

    // DELETE /admin/peers/respuestas/{id} - eliminar cualquier respuesta (moderación)
    delete("/admin/peers/respuestas/{id}") {
        if (obtenerRol(call) != "ADMIN") {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acceso denegado"))
            return@delete
        }
        val respuestaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@delete
        }
        transaction { PeersRespuestas.deleteWhere { PeersRespuestas.id eq respuestaId } }
        call.respond(mapOf("mensaje" to "Respuesta eliminada por administrador"))
    }

    // POST /admin/peers/solicitudes/{id}/cerrar - cerrar cualquier solicitud
    post("/admin/peers/solicitudes/{id}/cerrar") {
        if (obtenerRol(call) != "ADMIN") {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acceso denegado"))
            return@post
        }
        val solicitudId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@post
        }
        transaction {
            PeersSolicitudes.update({ PeersSolicitudes.id eq solicitudId }) {
                it[estado] = "CERRADA"
            }
        }
        call.respond(mapOf("mensaje" to "Solicitud cerrada por administrador"))
    }
}
