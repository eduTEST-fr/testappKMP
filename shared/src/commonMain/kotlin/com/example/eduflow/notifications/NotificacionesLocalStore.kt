package com.example.eduflow.notifications

import com.russhwolf.settings.Settings

// El último id se guarda por sesión/token. Así un segundo usuario del mismo
// teléfono no pierde avisos porque el usuario anterior tenía ids más altos.
object NotificacionesLocalStore {
    private val settings = Settings()

    private fun clave(token: String): String {
        val sufijo = token.takeLast(16).filter { it.isLetterOrDigit() }
        return "ultimo_notif_id_$sufijo"
    }

    fun obtenerUltimoIdMostrado(token: String): Int = settings.getInt(clave(token), 0)

    fun guardarUltimoIdMostrado(token: String, id: Int) {
        val key = clave(token)
        if (id > settings.getInt(key, 0)) settings.putInt(key, id)
    }
}
