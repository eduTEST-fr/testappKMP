package com.example.eduflow.notifications

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

actual object NotificationScheduler {

    // 15 minutos es el mínimo que WorkManager permite para trabajo periódico.
    actual fun iniciar() {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val solicitud = PeriodicWorkRequestBuilder<NotificacionesSyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(restricciones).build()

        WorkManager.getInstance(AppContextHolder.appContext)
            .enqueueUniquePeriodicWork(
                NotificacionesSyncWorker.NOMBRE_TRABAJO,
                ExistingPeriodicWorkPolicy.KEEP,
                solicitud
            )
    }

    actual fun detener() {
        WorkManager.getInstance(AppContextHolder.appContext)
            .cancelUniqueWork(NotificacionesSyncWorker.NOMBRE_TRABAJO)
    }
}
