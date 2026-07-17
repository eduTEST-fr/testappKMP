package com.example.eduflow.notifications

// Prende, apaga o fuerza una revisión de notificaciones. Android usa
// WorkManager; las versiones Web no muestran avisos en la barra del sistema.
expect object NotificationScheduler {
    fun iniciar()
    fun sincronizarAhora()
    fun detener()
}
