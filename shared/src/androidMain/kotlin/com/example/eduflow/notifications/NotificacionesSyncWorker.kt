package com.example.eduflow.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.eduflow.config.ApiConfig
import com.example.eduflow.shared.R
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Tarea en segundo plano (WorkManager) que revisa periódicamente si hay
// notificaciones nuevas en el backend y, si las hay, las muestra en la
// barra del sistema — así el usuario se entera aunque tenga la app cerrada.
class NotificacionesSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Serializable
    private data class NotificacionRemota(
        val id: Int,
        val titulo: String,
        val contenido: String,
        val leida: Boolean
    )

    companion object {
        const val NOMBRE_TRABAJO = "eduflow_notificaciones_sync"
        const val ID_CANAL = "eduflow_notificaciones"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val token = SesionStorage.obtenerToken() ?: return@withContext Result.success()

        val client = HttpClient()
        try {
            val respuesta = client.get("${ApiConfig.BASE_URL}/notificaciones") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()

            val notificaciones = json.decodeFromString<List<NotificacionRemota>>(respuesta)
            if (notificaciones.isEmpty()) return@withContext Result.success()

            val ultimoIdMostrado = NotificacionesLocalStore.obtenerUltimoIdMostrado()
            val nuevas = notificaciones.filter { !it.leida && it.id > ultimoIdMostrado }

            crearCanalSiNoExiste()
            nuevas.forEach { mostrarNotificacionSistema(it) }

            val maxId = notificaciones.maxOf { it.id }
            NotificacionesLocalStore.guardarUltimoIdMostrado(maxId)

            Result.success()
        } catch (e: Exception) {
            // Sin conexión o backend caído: no reintentamos agresivo, la
            // siguiente corrida periódica lo intentará de nuevo.
            Result.success()
        } finally {
            client.close()
        }
    }

    private fun crearCanalSiNoExiste() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canalExistente = manager.getNotificationChannel(ID_CANAL)
        if (canalExistente != null) return

        val canal = NotificationChannel(
            ID_CANAL,
            "Notificaciones de EduFlow",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de asesorías, respuestas y calificaciones"
        }
        manager.createNotificationChannel(canal)
    }

    private fun mostrarNotificacionSistema(notificacion: NotificacionRemota) {
        // Se arma el intent por nombre de clase (en vez de importar MainActivity
        // directamente) porque el módulo shared no depende del módulo androidApp.
        val intent = try {
            Intent(applicationContext, Class.forName("com.example.eduflow.MainActivity"))
        } catch (e: ClassNotFoundException) {
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                ?: Intent()
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificacion.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(applicationContext, ID_CANAL)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(0xFF2D5A3D.toInt())
            .setContentTitle(notificacion.titulo)
            .setContentText(notificacion.contenido)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificacion.contenido))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // En Android 13+ se necesita permiso POST_NOTIFICATIONS concedido en tiempo
        // de ejecución; si el usuario lo negó, simplemente no se muestra el aviso.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(notificacion.id, notif)
        }
    }
}
