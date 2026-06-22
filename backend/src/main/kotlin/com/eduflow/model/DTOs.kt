package com.eduflow.model

import kotlinx.serialization.Serializable

// --- EP5: Autenticación ---
@Serializable
data class RegisterRequest(
    val matricula: String,
    val correo: String,
    val password: String,
    val nombre: String
)

@Serializable
data class LoginRequest(
    val correo: String,
    val password: String
)

// --- EP6: Materias y exámenes ---
@Serializable
data class MateriaRequest(val nombre: String, val dificultad: Int)

@Serializable
data class MateriaDto(val id: Int, val nombre: String, val dificultad: Int)

@Serializable
data class ExamenRequest(val nombre: String, val fecha: String)

// --- EP7: Tarjetas y podcasts con IA ---
@Serializable
data class GenerarTarjetasRequest(
    val materiaId: Int,
    val examenId: Int,
    val materia: String,
    val texto: String? = null,
    val imagenBase64: String? = null,
    // Nombre corto de la subcarpeta/tema (ej. "Matrices"). Si no llega,
    // el backend usa "General" para no romper clientes viejos.
    val tema: String? = null
)

@Serializable
data class GenerarPodcastRequest(
    val materiaId: Int,
    val examenId: Int,
    val materia: String,
    val tema: String
)

// --- DTOs de respuesta (antes eran mapOf sin tipo) ---
@Serializable
data class TarjetaDto(
    val id: Int,
    val pregunta: String,
    val respuesta: String,
    val tema: String = "General",
    val completado: Boolean = false
)

@Serializable
data class GenerarTarjetasResponse(val tarjetas: List<TarjetaDto>, val total: Int)

// Marca todas las tarjetas de una subcarpeta (materiaId + tema) como estudiadas.
@Serializable
data class CompletarTemaRequest(val materiaId: Int, val tema: String)

@Serializable
data class PodcastDto(
    val id: Int,
    val titulo: String,
    val guion: String,
    val audioUrl: String,  // ruta relativa, ej: /podcasts/audio/7 -- ya NO es base64
    val tema: String = "General",
    val completado: Boolean = false
)
