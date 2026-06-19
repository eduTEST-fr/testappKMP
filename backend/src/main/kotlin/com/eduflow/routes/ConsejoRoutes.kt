package com.eduflow.routes

import com.eduflow.service.GroqService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class ConsejoRequest(val materia: String)

fun Routing.consejoRoutes() {
    // POST /consejos/generar
    post("/consejos/generar") {
        val req = call.receive<ConsejoRequest>()
        val consejo = GroqService.generarConsejo(req.materia)
        call.respond(mapOf("consejo" to consejo))
    }
}
