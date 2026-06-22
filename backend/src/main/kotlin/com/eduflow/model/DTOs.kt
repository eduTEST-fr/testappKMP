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

// --- EP8: Red de Apoyo entre Pares ---

@Serializable
data class ActualizarPerfilRequest(
    val carrera: String? = null,
    val cuatrimestre: Int? = null,
    val sobreMi: String? = null,
    val materiasDestaca: String? = null, // CSV: "Algebra,Calculo,POO"
    val avatarId: String? = null,
    // Solo aplica si el usuario es ASESOR:
    val grado: String? = null,
    val especialidad: String? = null,
    val permiteAsesoria: Boolean? = null
)

@Serializable
data class PerfilDto(
    val id: Int,
    val nombre: String,
    val carrera: String,
    val cuatrimestre: Int,
    val sobreMi: String,
    val materiasDestaca: String,
    val rol: String,
    val avatarId: String,
    val grado: String = "",
    val especialidad: String = "",
    val permiteAsesoria: Boolean = false
)

@Serializable
data class CrearSolicitudRequest(
    val titulo: String,
    val descripcion: String,
    val materia: String? = null,
    val imagenBase64: String? = null
)

@Serializable
data class ResponderSolicitudRequest(
    val contenido: String,
    val imagenBase64: String? = null
)

@Serializable
data class CalificarMentorRequest(
    val asesorId: Int,
    val solicitudId: Int,
    val estrellas: Int // 1..5
)

@Serializable
data class AutorDto(
    val id: Int,
    val nombre: String,
    val carrera: String = "",
    val cuatrimestre: Int = 1,
    val rol: String = "ALUMNO"
)

@Serializable
data class SolicitudDto(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val materia: String,
    val estado: String,
    val tieneImagen: Boolean,
    val createdAt: String,
    val autor: AutorDto
)

@Serializable
data class RespuestaDto(
    val id: Int,
    val contenido: String,
    val imagenBase64: String,
    val createdAt: String,
    val autor: AutorDto
)

@Serializable
data class SolicitudDetalleDto(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val imagenBase64: String,
    val materia: String,
    val estado: String,
    val createdAt: String,
    val autor: AutorDto,
    val respuestas: List<RespuestaDto>
)

@Serializable
data class MentorDto(
    val id: Int,
    val nombre: String,
    val carrera: String,
    val cuatrimestre: Int,
    val materiasDestaca: String,
    val rol: String,
    val avatarId: String,
    val promedio: Double,
    val totalCalif: Int,
    val permiteAsesoria: Boolean
)
