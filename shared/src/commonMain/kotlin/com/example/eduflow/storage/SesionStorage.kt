package com.example.eduflow.storage

import com.russhwolf.settings.Settings

object SesionStorage {
    private val settings = Settings()

    fun guardarToken(token: String, nombre: String) {
        settings.putString("jwt_token", token)
        settings.putString("usuario_nombre", nombre)
    }

    fun obtenerToken(): String? = settings.getStringOrNull("jwt_token")

    fun obtenerNombre(): String = settings.getString("usuario_nombre", "Estudiante")

    fun cerrarSesion() {
        settings.remove("jwt_token")
        settings.remove("usuario_nombre")
    }

    fun haySesion(): Boolean = obtenerToken() != null
}
