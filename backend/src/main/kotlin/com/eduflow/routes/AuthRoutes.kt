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

private const val CODIGO_ASESOR = "uptEduFlowAsesor"

fun Routing.authRoutes() {

    // POST /auth/register
    post("/auth/register") {
        val req = call.receive<RegisterRequest>()

        if (!req.correo.endsWith("@upt.edu.mx")) {
            call.respond(HttpStatusCode.BadRequest,
                mapOf("error" to "El correo debe ser @upt.edu.mx"))
            return@post
        }

        val existe = transaction {
            Usuarios.selectAll()
                .where { Usuarios.correo eq req.correo }
                .count() > 0
        }
        if (existe) {
            call.respond(HttpStatusCode.Conflict,
                mapOf("error" to "El correo ya está registrado"))
            return@post
        }

        // Si el usuario provee el código correcto se registra como ASESOR,
        // de lo contrario entra como ALUMNO. ADMIN solo se asigna por BD.
        val rolAsignado = if (req.codigoAsesor == CODIGO_ASESOR) "ASESOR" else "ALUMNO"

        val id = transaction {
            Usuarios.insertAndGetId {
                it[matricula]  = req.matricula
                it[correo]     = req.correo
                it[password]   = AuthService.hashPassword(req.password)
                it[nombre]     = req.nombre
                it[rol]        = rolAsignado
            }.value
        }

        val token = AuthService.generarToken(id, rolAsignado)
        call.respond(HttpStatusCode.Created,
            mapOf("token" to token, "nombre" to req.nombre, "rol" to rolAsignado))
    }

    // POST /auth/login
    post("/auth/login") {
        val req = call.receive<LoginRequest>()
        val usuario = transaction {
            Usuarios.selectAll().where { Usuarios.correo eq req.correo }.singleOrNull()
        }

        if (usuario == null || !AuthService.verificarPassword(req.password, usuario[Usuarios.password])) {
            call.respond(HttpStatusCode.Unauthorized,
                mapOf("error" to "Correo o contraseña incorrectos"))
            return@post
        }

        val rolUsuario = usuario[Usuarios.rol]
        val token = AuthService.generarToken(usuario[Usuarios.id].value, rolUsuario)
        call.respond(mapOf(
            "token"  to token,
            "nombre" to usuario[Usuarios.nombre],
            "rol"    to rolUsuario
        ))
    }

    // GET /auth/me — devuelve el perfil mínimo del usuario logueado
    get("/auth/me") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val usuario = transaction {
            Usuarios.selectAll().where { Usuarios.id eq userId }.firstOrNull()
        } ?: run {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
            return@get
        }
        call.respond(mapOf(
            "id"     to usuario[Usuarios.id].value,
            "nombre" to usuario[Usuarios.nombre],
            "rol"    to usuario[Usuarios.rol]
        ))
    }
}
