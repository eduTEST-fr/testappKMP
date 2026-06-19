package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.GroqService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*

fun Routing.tarjetaRoutes() {
    // POST /tarjetas/generar
    // Body: { "materiaId": 1, "examenId": 2, "texto": "...", "imagenBase64": "..." }
    // imagenBase64 es opcional. Si viene, se manda a Llama 4 Scout con vision.
    // Si solo viene texto, se manda como prompt de texto normal.
    post("/tarjetas/generar") {
        val req = call.receive<GenerarTarjetasRequest>()

        // Llamar a Groq segun si hay imagen o no
        val tarjetasJson = if (req.imagenBase64 != null) {
            GroqService.generarTarjetasDesdeImagen(
                req.imagenBase64, req.texto ?: "", req.materia
            )
        } else {
            GroqService.generarTarjetasDesdeTexto(
                req.texto ?: "", req.materia
            )
        }

        // Parsear el JSON de tarjetas y guardar en MySQL
        val tarjetasGuardadas = mutableListOf<Map<String, Any>>()
        val regexPar = Regex(""""pregunta":"([^"]+)","respuesta":"([^"]+)"""")
        regexPar.findAll(tarjetasJson).forEach { match ->
            val pregunta  = match.groupValues[1]
            val respuesta = match.groupValues[2]
            val id = transaction {
                Tarjetas.insertAndGetId {
                    it[Tarjetas.materiaId] = req.materiaId
                    it[Tarjetas.examenId]  = if (req.examenId > 0) req.examenId else null
                    it[Tarjetas.pregunta]  = pregunta
                    it[Tarjetas.respuesta] = respuesta
                }.value
            }
            tarjetasGuardadas.add(mapOf("id" to id,
                "pregunta" to pregunta, "respuesta" to respuesta))
        }

        call.respond(HttpStatusCode.Created,
            mapOf("tarjetas" to tarjetasGuardadas,
                  "total" to tarjetasGuardadas.size))
    }

    // GET /tarjetas/{materiaId} - lista tarjetas activas de una materia
    get("/tarjetas/{materiaId}") {
        val materiaId = call.parameters["materiaId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val lista = transaction {
            Tarjetas.selectAll().where { Tarjetas.materiaId eq materiaId }
                .map { mapOf(
                    "id"        to it[Tarjetas.id].value,
                    "pregunta"  to it[Tarjetas.pregunta],
                    "respuesta" to it[Tarjetas.respuesta]
                )}
        }
        call.respond(lista)
    }
}
