package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.authRoutes() {
    // POST /auth/register
    post("/auth/register") {
        val req = call.receive<RegisterRequest>()

        // Validar dominio institucional
        if (!req.correo.endsWith("@upt.edu.mx")) {
            call.respond(HttpStatusCode.BadRequest,
                mapOf("error" to "El correo debe ser @upt.edu.mx"))
            return@post
        }

        // Verificar que el correo no exista ya
        val existe = transaction {
            Usuarios.select { Usuarios.correo eq req.correo }.count() > 0
        }
        if (existe) {
            call.respond(HttpStatusCode.Conflict,
                mapOf("error" to "El correo ya está registrado"))
            return@post
        }

        // Insertar usuario con contraseña hasheada
        val id = transaction {
            Usuarios.insertAndGetId {
                it[matricula] = req.matricula
                it[correo]    = req.correo
                it[password]  = AuthService.hashPassword(req.password)
                it[nombre]    = req.nombre
            }.value
        }

        val token = AuthService.generarToken(id)
        call.respond(HttpStatusCode.Created, mapOf("token" to token, "nombre" to req.nombre))
    }

    // POST /auth/login
    post("/auth/login") {
        val req = call.receive<LoginRequest>()
        val usuario = transaction {
            Usuarios.select { Usuarios.correo eq req.correo }.singleOrNull()
        }

        if (usuario == null || !AuthService.verificarPassword(req.password, usuario[Usuarios.password])) {
            call.respond(HttpStatusCode.Unauthorized,
                mapOf("error" to "Correo o contraseña incorrectos"))
            return@post
        }

        val token = AuthService.generarToken(usuario[Usuarios.id].value)
        call.respond(mapOf("token" to token, "nombre" to usuario[Usuarios.nombre]))
    }
}
