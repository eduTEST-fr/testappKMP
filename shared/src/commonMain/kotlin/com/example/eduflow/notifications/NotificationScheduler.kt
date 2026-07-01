package com.example.eduflow.notifications

// Prende o apaga la revisión periódica de notificaciones en segundo plano.
// Cada plataforma decide cómo hacerlo (en Android usamos WorkManager); en
// Web no aplica, así que ahí no hace nada.
expect object NotificationScheduler {
    fun iniciar()
    fun detener()
}
