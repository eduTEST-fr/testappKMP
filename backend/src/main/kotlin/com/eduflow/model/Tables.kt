package com.eduflow.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object Usuarios : IntIdTable("usuarios") {
    val matricula = varchar("matricula", 20).uniqueIndex()
    val correo = varchar("correo", 100).uniqueIndex()
    val password = varchar("password", 255)
    val nombre = varchar("nombre", 100)
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
}

object Materias : IntIdTable("materias") {
    val usuarioId = integer("usuario_id").references(Usuarios.id)
    val nombre = varchar("nombre", 100)
    val dificultad = integer("dificultad")
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
}

object Examenes : IntIdTable("examenes") {
    val materiaId = integer("materia_id").references(Materias.id)
    val nombre = varchar("nombre", 50)
    val fecha = date("fecha")
}

object Tarjetas : IntIdTable("tarjetas") {
    val materiaId = integer("materia_id").references(Materias.id)
    val examenId = integer("examen_id").references(Examenes.id).nullable()
    val pregunta = text("pregunta")
    val respuesta = text("respuesta")
    // Subcarpeta/tema al que pertenece el set de tarjetas (ej: "Matrices").
    // Nullable+default para no romper filas viejas ya guardadas en MySQL.
    val tema = varchar("tema", 150).default("General")
    val completado = bool("completado").clientDefault { false }
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
}

object Podcasts : IntIdTable("podcasts") {
    val materiaId = integer("materia_id").references(Materias.id)
    val examenId = integer("examen_id").references(Examenes.id).nullable()
    val titulo = varchar("titulo", 200)
    val audioBytes = blob("audio_bytes").nullable() // MP3 en español (Google Translate TTS)
    val guion = text("guion").nullable()
    // Subcarpeta/tema del episodio (ya viaja en GenerarPodcastRequest.tema,
    // ahora tambien se persiste como columna para poder agrupar/filtrar).
    val tema = varchar("tema", 150).default("General")
    val completado = bool("completado").clientDefault { false }
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
}

object RedApoyo : IntIdTable("red_apoyo") {
    val usuarioId = integer("usuario_id").references(Usuarios.id)
    val tipo = varchar("tipo", 20)
    val materia = varchar("materia", 100)
    val mensaje = text("mensaje")
    val activo = bool("activo").clientDefault { true }
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
}
