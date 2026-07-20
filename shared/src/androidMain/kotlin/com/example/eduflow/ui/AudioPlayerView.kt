package com.example.eduflow.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class EstadoAudio { CARGANDO, LISTO, ERROR }

private object AudioPlaybackCoordinator {
    private var activo: MediaPlayer? = null

    fun activar(player: MediaPlayer) {
        if (activo !== player) {
            runCatching { activo?.pause() }
            activo = player
        }
    }

    fun liberar(player: MediaPlayer?) {
        if (activo === player) activo = null
    }
}

@Composable
actual fun AudioPlayerWidget(
    audioUrl: String,
    token: String,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val client = remember { HttpClient() }
    val callbackCompletado by rememberUpdatedState(onCompleted)

    var estado by remember(audioUrl) { mutableStateOf(EstadoAudio.CARGANDO) }
    var reproduciendo by remember(audioUrl) { mutableStateOf(false) }
    var posicionMs by remember(audioUrl) { mutableIntStateOf(0) }
    var duracionMs by remember(audioUrl) { mutableIntStateOf(0) }
    var mediaPlayer by remember(audioUrl) { mutableStateOf<MediaPlayer?>(null) }
    var intento by remember(audioUrl) { mutableIntStateOf(0) }

    LaunchedEffect(audioUrl, intento) {
        estado = EstadoAudio.CARGANDO
        reproduciendo = false
        posicionMs = 0
        duracionMs = 0
        mediaPlayer?.release()
        mediaPlayer = null

        var nuevoPlayer: MediaPlayer? = null
        try {
            val response = client.get("${ApiConfig.BASE_URL}$audioUrl") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status.value !in 200..299) {
                error("El servidor respondió ${response.status.value}")
            }

            val bytes = response.readBytes()
            if (bytes.isEmpty()) error("El audio recibido está vacío")

            val archivo = withContext(Dispatchers.IO) {
                val id = audioUrl.substringAfterLast('/').ifBlank { "actual" }
                File(context.cacheDir, "eduflow_podcast_$id.mp3").apply {
                    writeBytes(bytes)
                }
            }

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            }
            nuevoPlayer = player

            withContext(Dispatchers.IO) {
                player.setDataSource(archivo.absolutePath)
                player.prepare()
            }

            player.setOnCompletionListener {
                AudioPlaybackCoordinator.liberar(player)
                reproduciendo = false
                posicionMs = duracionMs
                callbackCompletado()
            }
            player.setOnErrorListener { _, _, _ ->
                reproduciendo = false
                estado = EstadoAudio.ERROR
                true
            }

            duracionMs = player.duration.coerceAtLeast(0)
            mediaPlayer = player
            estado = EstadoAudio.LISTO
        } catch (_: Exception) {
            nuevoPlayer?.release()
            mediaPlayer = null
            estado = EstadoAudio.ERROR
        }
    }

    LaunchedEffect(mediaPlayer, estado, reproduciendo) {
        val player = mediaPlayer ?: return@LaunchedEffect
        while (estado == EstadoAudio.LISTO && mediaPlayer === player) {
            posicionMs = runCatching { player.currentPosition }.getOrDefault(posicionMs)
            val sigueReproduciendo = runCatching { player.isPlaying }.getOrDefault(false)
            if (reproduciendo && !sigueReproduciendo && posicionMs < duracionMs) {
                reproduciendo = false
            }
            delay(if (reproduciendo) 250L else 700L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioPlaybackCoordinator.liberar(mediaPlayer)
            mediaPlayer?.release()
            client.close()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.13f)
    ) {
        when (estado) {
            EstadoAudio.CARGANDO -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Preparando episodio",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "Descargando el audio de forma segura…",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            EstadoAudio.ERROR -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No se pudo cargar el episodio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        "Comprueba tu conexión e inténtalo nuevamente.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 3.dp, bottom = 8.dp)
                    )
                    TextButton(onClick = { intento++ }) {
                        Text("Reintentar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            EstadoAudio.LISTO -> {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Reproductor de audio",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.78f)
                    )

                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (duracionMs > 0) posicionMs.toFloat() / duracionMs.toFloat() else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.20f)
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                        Text(
                            formatearTiempoAudio(posicionMs),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatearTiempoAudio(duracionMs),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BotonAudioSecundario(texto = "−10", descripcion = "Retroceder 10 segundos") {
                            mediaPlayer?.let { player ->
                                val destino = (player.currentPosition - 10_000).coerceAtLeast(0)
                                player.seekTo(destino.toLong(), MediaPlayer.SEEK_CLOSEST)
                                posicionMs = destino
                            }
                        }

                        Spacer(Modifier.width(18.dp))
                        Surface(
                            modifier = Modifier
                                .size(58.dp)
                                .clickable {
                                    mediaPlayer?.let { player ->
                                        if (reproduciendo) {
                                            player.pause()
                                            reproduciendo = false
                                        } else {
                                            if (duracionMs > 0 && player.currentPosition >= duracionMs - 250) {
                                                player.seekTo(0L, MediaPlayer.SEEK_CLOSEST)
                                                posicionMs = 0
                                            }
                                            AudioPlaybackCoordinator.activar(player)
                                            player.start()
                                            reproduciendo = true
                                        }
                                    }
                                },
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    if (reproduciendo) "Ⅱ" else "▶",
                                    fontSize = if (reproduciendo) 20.sp else 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerdePrimario
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))

                        BotonAudioSecundario(texto = "+10", descripcion = "Adelantar 10 segundos") {
                            mediaPlayer?.let { player ->
                                val limite = duracionMs.coerceAtLeast(0)
                                val destino = (player.currentPosition + 10_000).coerceAtMost(limite)
                                player.seekTo(destino.toLong(), MediaPlayer.SEEK_CLOSEST)
                                posicionMs = destino
                            }
                        }
                    }

                    Text(
                        if (reproduciendo) "Reproduciendo" else "En pausa",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.68f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonAudioSecundario(
    texto: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text(
            descripcion,
            fontSize = 8.sp,
            color = Color.White.copy(alpha = 0.64f),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(70.dp).padding(top = 3.dp)
        )
    }
}

private fun formatearTiempoAudio(milisegundos: Int): String {
    val totalSegundos = (milisegundos.coerceAtLeast(0) / 1000)
    val minutos = totalSegundos / 60
    val segundos = totalSegundos % 60
    return "$minutos:${segundos.toString().padStart(2, '0')}"
}
