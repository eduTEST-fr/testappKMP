package com.example.eduflow.api

import com.example.eduflow.config.ApiConfig
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MateriaEstudioDto(val id: Int, val nombre: String, val dificultad: Int)

@Serializable
data class ExamenEstudioDto(val id: Int, val nombre: String, val fecha: String)

@Serializable
data class GuardarSesionEstudioRequest(
    val materiaId: Int,
    val fecha: String,
    val duracionSegundos: Int,
    val metaMinutos: Int
)

@Serializable
data class SesionEstudioDto(
    val id: Int,
    val materiaId: Int,
    val materiaNombre: String,
    val fecha: String,
    val duracionSegundos: Int,
    val metaMinutos: Int,
    val createdAt: String = ""
)

@Serializable
data class HistorialSesionesDto(
    val sesiones: List<SesionEstudioDto>,
    val totalSegundos: Long
)

class SesionesEstudioApi(
    private val client: HttpClient = HttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private fun token(): String = SesionStorage.obtenerToken()
        ?: error("No hay una sesión autenticada")

    suspend fun cargarMaterias(): List<MateriaEstudioDto> {
        val body = client.get("${ApiConfig.BASE_URL}/materias") {
            header("Authorization", "Bearer ${token()}")
        }.bodyAsText()
        return json.decodeFromString(body)
    }

    suspend fun cargarExamenes(materiaId: Int): List<ExamenEstudioDto> {
        val body = client.get("${ApiConfig.BASE_URL}/materias/$materiaId/examenes") {
            header("Authorization", "Bearer ${token()}")
        }.bodyAsText()
        return json.decodeFromString(body)
    }

    suspend fun cargarHistorial(materiaId: Int): HistorialSesionesDto {
        val body = client.get("${ApiConfig.BASE_URL}/sesiones/materia/$materiaId") {
            header("Authorization", "Bearer ${token()}")
        }.bodyAsText()
        return json.decodeFromString(body)
    }

    suspend fun guardarSesion(request: GuardarSesionEstudioRequest): SesionEstudioDto {
        val response = client.post("${ApiConfig.BASE_URL}/sesiones") {
            header("Authorization", "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        if (response.status.value !in 200..299) {
            error("El servidor rechazó la sesión (${response.status.value})")
        }
        return json.decodeFromString(response.bodyAsText())
    }

    fun close() = client.close()
}
