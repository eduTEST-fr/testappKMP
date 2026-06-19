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
import java.util.Base64

fun Routing.podcastRoutes() {
    // POST /podcasts/generar
    // Body: { "materiaId": 1, "examenId": 2, "materia": "Algebra", "tema": "Matrices" }
    post("/podcasts/generar") {
        val req = call.receive<GenerarPodcastRequest>()

        // Paso 1: generar guion con llama-3.1-8b-instant
        val guion = GroqService.generarGuionPodcast(req.materia, req.tema)

        // Paso 2: convertir guion a audio WAV con Orpheus TTS
        val audioBytes = GroqService.generarAudio(guion)

        // Paso 3: guardar en MySQL como base64
        val audioBase64 = Base64.getEncoder().encodeToString(audioBytes)
        val titulo = "Podcast: ${req.tema} - ${req.materia}"

        val id = transaction {
            Podcasts.insertAndGetId {
                it[Podcasts.materiaId] = req.materiaId
                it[Podcasts.examenId]  = if (req.examenId > 0) req.examenId else null
                it[Podcasts.titulo]    = titulo
                it[Podcasts.guion]     = guion
                it[Podcasts.audioUrl]  = audioBase64
            }.value
        }

        call.respond(HttpStatusCode.Created, mapOf(
            "id"          to id,
            "titulo"      to titulo,
            "guion"       to guion,
            "audioBase64" to audioBase64
        ))
    }

    // GET /podcasts/{materiaId}
    get("/podcasts/{materiaId}") {
        val materiaId = call.parameters["materiaId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val lista = transaction {
            Podcasts.select { Podcasts.materiaId eq materiaId }
                .map { mapOf(
                    "id"          to it[Podcasts.id].value,
                    "titulo"      to it[Podcasts.titulo],
                    "guion"       to (it[Podcasts.guion] ?: ""),
                    "audioBase64" to (it[Podcasts.audioUrl] ?: "")
                )}
        }
        call.respond(lista)
    }
}
