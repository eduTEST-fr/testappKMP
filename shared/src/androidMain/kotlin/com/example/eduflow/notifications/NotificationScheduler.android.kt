package com.example.eduflow.notifications

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

actual object NotificationScheduler {
    private val restricciones
        get() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    actual fun iniciar() {
        sincronizarAhora()
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

    actual fun sincronizarAhora() {
        val chequeo = OneTimeWorkRequestBuilder<NotificacionesSyncWorker>()
            .setConstraints(restricciones)
            .build()
        WorkManager.getInstance(AppContextHolder.appContext)
            .enqueueUniqueWork(
                "${NotificacionesSyncWorker.NOMBRE_TRABAJO}_inmediato",
                ExistingWorkPolicy.REPLACE,
                chequeo
            )
    }

    actual fun detener() {
        WorkManager.getInstance(AppContextHolder.appContext).apply {
            cancelUniqueWork(NotificacionesSyncWorker.NOMBRE_TRABAJO)
            cancelUniqueWork("${NotificacionesSyncWorker.NOMBRE_TRABAJO}_inmediato")
        }
    }
}
