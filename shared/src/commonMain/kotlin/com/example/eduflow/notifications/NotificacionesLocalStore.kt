package com.example.eduflow.notifications

import com.russhwolf.settings.Settings

// Guarda el id de la última notificación que ya se mostró como notificación
// del sistema, para no repetirla en la siguiente revisión en segundo plano.
object NotificacionesLocalStore {
    private val settings = Settings()
    private const val CLAVE_ULTIMO_ID = "ultimo_notif_id_mostrado"

    fun obtenerUltimoIdMostrado(): Int = settings.getInt(CLAVE_ULTIMO_ID, 0)

    fun guardarUltimoIdMostrado(id: Int) {
        if (id > obtenerUltimoIdMostrado()) settings.putInt(CLAVE_ULTIMO_ID, id)
    }
}
