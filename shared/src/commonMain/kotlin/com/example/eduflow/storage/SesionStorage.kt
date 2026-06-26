package com.example.eduflow.storage

import com.russhwolf.settings.Settings

object SesionStorage {
    private val settings = Settings()

    fun guardarToken(token: String, nombre: String, rol: String = "ALUMNO") {
        settings.putString("jwt_token", token)
        settings.putString("usuario_nombre", nombre)
        settings.putString("usuario_rol", rol)
    }

    fun obtenerToken(): String? = settings.getStringOrNull("jwt_token")
    fun obtenerNombre(): String = settings.getString("usuario_nombre", "Estudiante")
    fun obtenerRol(): String   = settings.getString("usuario_rol", "ALUMNO")

    fun cerrarSesion() {
        settings.remove("jwt_token")
        settings.remove("usuario_nombre")
        settings.remove("usuario_rol")
    }

    fun haySesion(): Boolean = obtenerToken() != null
}
