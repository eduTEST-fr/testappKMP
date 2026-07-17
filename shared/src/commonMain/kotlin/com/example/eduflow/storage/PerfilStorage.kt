// storage/PerfilStorage.kt
package com.example.eduflow.storage

import com.russhwolf.settings.Settings

// Guarda el perfil del usuario (carrera, cuatrimestre, materias en las que
// destaca) de forma local en el dispositivo. Por ahora la Red de Apoyo es
// solo visual, asi que no se manda nada al backend todavia.
object PerfilStorage {
    private val settings = Settings()
    private const val SEPARADOR = "||"

    fun guardarPerfil(carrera: String, cuatrimestre: String, ubicacion: String, bio: String) {
        settings.putString("perfil_carrera", carrera)
        settings.putString("perfil_cuatrimestre", cuatrimestre)
        settings.putString("perfil_ubicacion", ubicacion)
        settings.putString("perfil_bio", bio)
    }

    fun obtenerCarrera(): String =
        settings.getString("perfil_carrera", "Ingeniería en Sistemas Computacionales")

    fun obtenerCuatrimestre(): String =
        settings.getString("perfil_cuatrimestre", "5to Cuatrimestre")

    fun obtenerUbicacion(): String =
        settings.getString("perfil_ubicacion", "Tulancingo, Hidalgo")

    fun obtenerBio(): String =
        settings.getString("perfil_bio", "")

    fun guardarMateriasDestacadas(materias: List<String>) {
        settings.putString("perfil_materias", materias.joinToString(SEPARADOR))
    }

    fun obtenerMateriasDestacadas(): List<String> {
        val guardado = settings.getString("perfil_materias", "")
        return if (guardado.isBlank()) emptyList()
        else guardado.split(SEPARADOR).filter { it.isNotBlank() }
    }
}
