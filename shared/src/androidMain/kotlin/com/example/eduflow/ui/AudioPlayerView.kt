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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

// Refleja exactamente el PodcastDto del backend. audioUrl ahora es una
// ruta corta (ej: /podcasts/audio/7) en vez de un string base64 gigante.
@Serializable
private data class PodcastApiDto(
    val id: Int,
    val titulo: String,
    val guion: String,
    val audioUrl: String
)

private val jsonParser = Json { ignoreUnknownKeys = true }

@Composable
actual fun AudioPlayerView(materia: MateriaUI?, token: String) {
    val scope   = rememberCoroutineScope()
    val client  = remember { HttpClient() }
    val context = LocalContext.current
    var titulo    by remember { mutableStateOf("") }
    var guion     by remember { mutableStateOf("") }
    var hayAudio  by remember { mutableStateOf(false) }
    var generando by remember { mutableStateOf(false) }
    var tema      by remember { mutableStateOf("") }
    var errorMsg  by remember { mutableStateOf("") }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var reproduciendo by remember { mutableStateOf(false) }

    suspend fun cargarDesdeDto(dto: PodcastApiDto) {
        titulo = dto.titulo
        guion  = dto.guion
        try {
            // Se descargan los bytes del WAV directo desde el backend
            // (GET /podcasts/audio/{id}). Ya no hay base64 ni JSON de por
            // medio: la respuesta es el archivo de audio crudo.
            val bytes = client.get("${ApiConfig.BASE_URL}${dto.audioUrl}") {
                header("Authorization", "Bearer $token")
            }.readBytes()
            val file = File(context.cacheDir, "podcast_${materia?.id}.mp3")
            file.writeBytes(bytes)
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }
            hayAudio = true
        } catch (e: Exception) {
            errorMsg = "El audio guardado no es válido. Genera el podcast de nuevo."
            hayAudio = false
        }
    }

    // Cargar podcasts existentes
    LaunchedEffect(materia?.id) {
        if (materia == null) return@LaunchedEffect
        try {
            val resp = client.get(
                "${ApiConfig.BASE_URL}/podcasts/${materia.id}"
            ) {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val lista = jsonParser.decodeFromString<List<PodcastApiDto>>(resp)
            val primero = lista.firstOrNull()
            if (primero != null) cargarDesdeDto(primero)
        } catch (e: Exception) {}
    }

    if (materia == null) {
        Text("Selecciona una materia primero",
            fontSize = 14.sp, color = TextoSecundario,
            modifier = Modifier.padding(20.dp))
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (hayAudio) {
            // Reproductor
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VerdePrimario)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.18f)) {
                        Text("STUDYCAST PODCAST", fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, color = Color.White,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(titulo, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    if (guion.isNotEmpty())
                        Text(guion.take(120) + "...", fontSize = 12.sp,
                            color = Color.White.copy(0.72f),
                            modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(20.dp))
                    // Boton play/pause
                    Row(horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()) {
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
                                    fontSize = 20.sp, color = VerdePrimario,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Generar nuevo podcast
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Generar podcast de estudio",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = TextoPrimario)
                    Text("Un episodio divertido con datos curiosos de tu materia",
                        fontSize = 13.sp, color = TextoSecundario,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                    CampoTexto("Tema especifico", tema, "Ej: Ciclos for, recursividad...") {
                        tema = it
                    }
                    if (errorMsg.isNotEmpty())
                        Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (tema.isEmpty()) { errorMsg = "Escribe el tema"; return@Button }
                            generando = true; errorMsg = ""
                            scope.launch {
                                try {
                                    val respuesta = client.post(
                                        "${ApiConfig.BASE_URL}/podcasts/generar"
                                    ) {
                                        header("Authorization", "Bearer $token")
                                        contentType(ContentType.Application.Json)
                                        setBody("""{"materiaId":${materia.id},"examenId":0,""" +
                                            """"materia":"${materia.nombre}","tema":"$tema"}""")
                                    }
                                    val resp = respuesta.bodyAsText()

                                    when (respuesta.status) {
                                        HttpStatusCode.UnprocessableEntity ->
                                            errorMsg = "La IA no pudo generar el podcast. Intenta de nuevo."
                                        HttpStatusCode.ServiceUnavailable -> {
                                            // El backend ya distingue este caso: el guion se
                                            // genero bien pero Groq no pudo crear el audio
                                            // (ej. modelo de voz sin terminos aceptados).
                                            val err = Regex(""""error":"([^"]+)"""")
                                                .find(resp)?.groupValues?.get(1)
                                            errorMsg = err ?: "No se pudo generar el audio."
                                        }
                                        else -> {
                                            val dto = jsonParser.decodeFromString<PodcastApiDto>(resp)
                                            cargarDesdeDto(dto)
                                        }
                                    }
                                } catch (e: Exception) { errorMsg = "Error al generar" }
                                generando = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario),
                        enabled = !generando
                    ) {
                        if (generando)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else
                            Text("Generar podcast con IA", color = Color.White,
                                fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
