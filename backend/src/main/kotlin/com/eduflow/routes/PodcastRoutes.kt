package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.GroqService
import com.eduflow.service.TtsService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

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

        // Paso 2: convertir guion a audio en ESPAÑOL.
        // Groq/Orpheus solo soporta voz en ingles y arabe -- por eso esta
        // parte usa TtsService (Google Translate TTS), que si entiende
        // español de forma nativa y no requiere API key.
        val audioBytes = TtsService.generarAudioEnEspanol(guion)

        if (audioBytes == null) {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                "error" to "No se pudo generar el audio en este momento. Intenta de nuevo en unos segundos."
            ))
            return@post
        }

        // Paso 3: guardar en MySQL como BLOB binario real (ya NO base64/texto).
        // Esto evita los logs gigantes y el truncamiento que daba MySQL antes,
        // porque ya no viaja como un string JSON enorme en cada insert/select.
        val titulo = "Podcast: ${req.tema} - ${req.materia}"

        val id = transaction {
            Podcasts.insertAndGetId {
                it[Podcasts.materiaId]   = req.materiaId
                it[Podcasts.examenId]    = if (req.examenId > 0) req.examenId else null
                it[Podcasts.titulo]      = titulo
                it[Podcasts.guion]       = guion
                it[Podcasts.audioBytes]  = ExposedBlob(audioBytes)
            }.value
        }

        // El cliente ya no recibe el audio en el JSON: recibe una ruta corta
        // que apunta al endpoint binario de abajo (GET /podcasts/audio/{id}).
        call.respond(HttpStatusCode.Created,
            PodcastDto(id, titulo, guion, "/podcasts/audio/$id"))
    }

    // GET /podcasts/{materiaId} - lista los podcasts de una materia (sin el audio pesado)
    get("/podcasts/{materiaId}") {
        val materiaId = call.parameters["materiaId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val lista = transaction {
            Podcasts.selectAll().where { Podcasts.materiaId eq materiaId }
                .map { row -> PodcastDto(
                    row[Podcasts.id].value,
                    row[Podcasts.titulo],
                    row[Podcasts.guion] ?: "",
                    "/podcasts/audio/${row[Podcasts.id].value}"
                )}
        }
        call.respond(lista)
    }

    // GET /podcasts/audio/{id} - sirve el WAV real como bytes binarios.
    // El cliente apunta su reproductor (MediaPlayer / <audio>) directo a esta URL,
    // sin pasar por JSON ni base64 en ningun momento.
    get("/podcasts/audio/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "ID inválido")
            return@get
        }
        val bytes = transaction {
            Podcasts.selectAll().where { Podcasts.id eq id }
                .singleOrNull()
                ?.get(Podcasts.audioBytes)?.bytes
        }
        if (bytes == null) {
            call.respond(HttpStatusCode.NotFound, "Audio no encontrado")
            return@get
        }
        call.respondBytes(bytes, ContentType.parse("audio/mpeg"))
    }
}
