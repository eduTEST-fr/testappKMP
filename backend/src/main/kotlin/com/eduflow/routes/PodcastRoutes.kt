package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.GroqService
import com.eduflow.service.TtsService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.podcastRoutes() {
    // POST /podcasts/generar
    // Genera y guarda un episodio únicamente para una materia del usuario autenticado.
    post("/podcasts/generar") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<GenerarPodcastRequest>()
        val temaLimpio = req.tema.trim().take(150)
        if (temaLimpio.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Escribe un tema para el episodio"))
            return@post
        }

        val datosMateria = transaction {
            val materia = Materias.selectAll()
                .where { (Materias.id eq req.materiaId) and (Materias.usuarioId eq userId) }
                .singleOrNull() ?: return@transaction null

            val examenValido = if (req.examenId > 0) {
                Examenes.selectAll()
                    .where { (Examenes.id eq req.examenId) and (Examenes.materiaId eq req.materiaId) }
                    .firstOrNull() != null
            } else true

            if (!examenValido) null else materia[Materias.nombre]
        }

        if (datosMateria == null) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "La materia o el examen no pertenecen al usuario"))
            return@post
        }

        // Se usa el nombre real guardado en MySQL, no el texto enviado por el cliente.
        val guion = GroqService.generarGuionPodcast(datosMateria, temaLimpio)
        if (guion.isBlank() || guion == "Sin respuesta") {
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                mapOf("error" to "La IA no devolvió un guion válido. Intenta de nuevo.")
            )
            return@post
        }

        val audioBytes = TtsService.generarAudioEnEspanol(guion)
        if (audioBytes == null || audioBytes.isEmpty()) {
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("error" to "No se pudo generar el audio en este momento. Intenta de nuevo en unos segundos.")
            )
            return@post
        }

        val titulo = "Podcast: $temaLimpio - $datosMateria"
        val id = transaction {
            Podcasts.insertAndGetId {
                it[Podcasts.materiaId] = req.materiaId
                it[Podcasts.examenId] = if (req.examenId > 0) req.examenId else null
                it[Podcasts.titulo] = titulo
                it[Podcasts.guion] = guion
                it[Podcasts.audioBytes] = ExposedBlob(audioBytes)
                it[Podcasts.tema] = temaLimpio
            }.value
        }

        call.respond(
            HttpStatusCode.Created,
            PodcastDto(id, titulo, guion, "/podcasts/audio/$id", temaLimpio, false)
        )
    }

    // GET /podcasts/{materiaId} - lista únicamente los podcasts de una materia propia.
    get("/podcasts/{materiaId}") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val materiaId = call.parameters["materiaId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }

        val lista = transaction {
            val pertenece = Materias.selectAll()
                .where { (Materias.id eq materiaId) and (Materias.usuarioId eq userId) }
                .firstOrNull() != null
            if (!pertenece) return@transaction null

            Podcasts.selectAll()
                .where { Podcasts.materiaId eq materiaId }
                .orderBy(Podcasts.createdAt, SortOrder.DESC)
                .map { row ->
                    PodcastDto(
                        row[Podcasts.id].value,
                        row[Podcasts.titulo],
                        row[Podcasts.guion] ?: "",
                        "/podcasts/audio/${row[Podcasts.id].value}",
                        row[Podcasts.tema],
                        row[Podcasts.completado]
                    )
                }
        }

        if (lista == null) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Materia no disponible"))
        } else {
            call.respond(lista)
        }
    }

    // PUT /podcasts/{id}/completar - marca como escuchado solo un episodio propio.
    put("/podcasts/{id}/completar") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@put
        }
        val id = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@put
        }

        val actualizado = transaction {
            val podcast = (Podcasts innerJoin Materias)
                .selectAll()
                .where { (Podcasts.id eq id) and (Materias.usuarioId eq userId) }
                .singleOrNull() ?: return@transaction false

            Podcasts.update({ Podcasts.id eq podcast[Podcasts.id].value }) {
                it[Podcasts.completado] = true
            }
            true
        }

        if (!actualizado) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Episodio no disponible"))
        } else {
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }
    }

    // GET /podcasts/audio/{id} - entrega el MP3 únicamente al propietario de la materia.
    get("/podcasts/audio/{id}") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, "Token inválido")
            return@get
        }
        val id = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "ID inválido")
            return@get
        }

        val bytes = transaction {
            (Podcasts innerJoin Materias)
                .selectAll()
                .where { (Podcasts.id eq id) and (Materias.usuarioId eq userId) }
                .singleOrNull()
                ?.get(Podcasts.audioBytes)
                ?.bytes
        }

        if (bytes == null) {
            call.respond(HttpStatusCode.NotFound, "Audio no encontrado")
            return@get
        }
        call.respondBytes(bytes, ContentType.parse("audio/mpeg"))
    }
}
