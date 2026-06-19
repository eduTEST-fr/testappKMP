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
data class ExamenRequest(val nombre: String, val fecha: String)

// --- EP7: Tarjetas y podcasts con IA ---
@Serializable
data class GenerarTarjetasRequest(
    val materiaId: Int,
    val examenId: Int,
    val materia: String,
    val texto: String? = null,
    val imagenBase64: String? = null
)

@Serializable
data class GenerarPodcastRequest(
    val materiaId: Int,
    val examenId: Int,
    val materia: String,
    val tema: String
)

// --- Respuestas tipadas ---
// IMPORTANTE: kotlinx.serialization no puede serializar mapOf(...) cuando mezcla
// tipos (Int + String se infiere como Map<String, Any>), eso provoca un 500
// silencioso en el cliente. Por eso todas las respuestas de listas usan DTOs.
@Serializable
data class MateriaResponse(val id: Int, val nombre: String, val dificultad: Int)

@Serializable
data class ExamenResponse(val id: Int, val nombre: String, val fecha: String)

@Serializable
data class TarjetaResponse(val id: Int, val pregunta: String, val respuesta: String)

@Serializable
data class TarjetasGeneradasResponse(val tarjetas: List<TarjetaResponse>, val total: Int)

@Serializable
data class PodcastResponse(
    val id: Int,
    val titulo: String,
    val guion: String,
    val audioBase64: String
)

@Serializable
data class RedApoyoResponse(val id: Int, val tipo: String, val materia: String, val mensaje: String)
