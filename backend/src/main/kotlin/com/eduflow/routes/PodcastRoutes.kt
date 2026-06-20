package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.GroqService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Base64
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*

fun Routing.podcastRoutes() {
    // POST /podcasts/generar
    // Body: { "materiaId": 1, "examenId": 2, "materia": "Algebra", "tema": "Matrices" }
    post("/podcasts/generar") {
        val req = call.receive<GenerarPodcastRequest>()

        // Paso 1: generar guion con llama-3.1-8b-instant
        val guion = GroqService.generarGuionPodcast(req.materia, req.tema)

        if (guion.isBlank() || guion == "Sin respuesta") {
            call.respond(HttpStatusCode.UnprocessableEntity, mapOf(
                "error" to "La IA no devolvió un guión válido. Intenta de nuevo."
            ))
            return@post
        }

        // Paso 2: convertir guion a audio WAV con Orpheus TTS
        val audioBytes = GroqService.generarAudio(guion)

        if (audioBytes == null) {
            // Groq no entrego audio real (por ejemplo: terminos del modelo
            // no aceptados en console.groq.com). No se guarda nada invalido
            // en MySQL; se avisa con un mensaje claro para que se revise
            // la cuenta de Groq en vez de fallar en silencio en la app.
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                "error" to "No se pudo generar el audio. Verifica que el modelo de " +
                    "voz esté habilitado en tu cuenta de Groq (console.groq.com)."
            ))
            return@post
        }

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

        // Antes esto era un mapOf que el frontend leia con regex; al ser
        // un guion con saltos de linea y comillas, el regex casi siempre
        // fallaba. Con @Serializable, kotlinx.serialization escapa el
        // JSON correctamente y el cliente lo lee sin perder texto.
        call.respond(HttpStatusCode.Created, PodcastDto(id, titulo, guion, audioBase64))
    }

    // GET /podcasts/{materiaId}
    get("/podcasts/{materiaId}") {
        val materiaId = call.parameters["materiaId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val lista = transaction {
            Podcasts.selectAll().where { Podcasts.materiaId eq materiaId }
                .map { PodcastDto(
                    it[Podcasts.id].value,
                    it[Podcasts.titulo],
                    it[Podcasts.guion] ?: "",
                    it[Podcasts.audioUrl] ?: ""
                )}
        }
        call.respond(lista)
    }
}
