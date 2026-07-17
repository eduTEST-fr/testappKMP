package com.example.eduflow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.config.ApiConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File

@Composable
actual fun AudioPlayerWidget(audioUrl: String, token: String) {
    val scope   = rememberCoroutineScope()
    val client  = remember { HttpClient() }
    val context = LocalContext.current

    var estado by remember(audioUrl) { mutableStateOf("cargando") } // cargando | listo | error
    var reproduciendo by remember(audioUrl) { mutableStateOf(false) }
    var mediaPlayer by remember(audioUrl) { mutableStateOf<android.media.MediaPlayer?>(null) }

    LaunchedEffect(audioUrl) {
        estado = "cargando"
        try {
            val bytes = client.get("${ApiConfig.BASE_URL}$audioUrl") {
                header("Authorization", "Bearer $token")
            }.readBytes()
            val nombreArchivo = "podcast_" + audioUrl.substringAfterLast("/") + ".mp3"
            val file = File(context.cacheDir, nombreArchivo)
            file.writeBytes(bytes)
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { reproduciendo = false }
                prepare()
            }
            estado = "listo"
        } catch (e: Exception) {
            estado = "error"
        }
    }

    DisposableEffect(audioUrl) {
        onDispose { mediaPlayer?.release() }
    }

    when (estado) {
        "cargando" -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Cargando audio...", fontSize = 13.sp, color = Color.White.copy(0.8f))
        }
        "error" -> Text("No se pudo cargar el audio.", fontSize = 13.sp, color = Color.White)
        else -> Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.size(56.dp).clickable {
                    mediaPlayer?.let { mp ->
                        if (reproduciendo) { mp.pause(); reproduciendo = false }
                        else { mp.start(); reproduciendo = true }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (reproduciendo) "II" else "▶",
                        fontSize = 20.sp, color = VerdePrimario, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
