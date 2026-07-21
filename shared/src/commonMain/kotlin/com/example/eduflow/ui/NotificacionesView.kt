package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.config.ApiConfig
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class NotificacionApiDto(
    val id: Int, val titulo: String, val contenido: String,
    val leida: Boolean, val createdAt: String = ""
)

private val jsonNotificaciones = Json { ignoreUnknownKeys = true }

// Bandeja de notificaciones del Dashboard: avisos cuando un asesor acepta o
// cancela una asesoría. Tocar una la marca como leída.
@Composable
fun NotificacionesView(onVolver: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var notificaciones by remember { mutableStateOf<List<NotificacionApiDto>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/notificaciones") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            notificaciones = jsonNotificaciones.decodeFromString(resp)
        } catch (e: Exception) { errorMsg = "No se pudieron cargar tus notificaciones." }
        cargando = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                BotonVolver(onClick = onVolver)
                Spacer(Modifier.weight(1f))
                Text("Notificaciones", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdePrimario)
                }
                return@Column
            }

            Column(modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp)) {

                if (errorMsg.isNotEmpty())
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp))

                if (notificaciones.isEmpty()) {
                    Text("No tienes notificaciones por ahora.", fontSize = 13.sp, color = TextoSecundario)
                } else {
                    notificaciones.forEach { n ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                .clickable {
                                    if (!n.leida) {
                                        scope.launch {
                                            try {
                                                client.put("${ApiConfig.BASE_URL}/notificaciones/${n.id}/leer") {
                                                    header("Authorization", "Bearer $token")
                                                }
                                                notificaciones = notificaciones.map {
                                                    if (it.id == n.id) it.copy(leida = true) else it
                                                }
                                            } catch (e: Exception) { /* se reintentará al volver a abrir */ }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (n.leida) Color.White else Color(0xFFEFF5F0)
                            ),
                            elevation = CardDefaults.cardElevation(if (n.leida) 0.dp else 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                if (!n.leida) {
                                    Box(modifier = Modifier.padding(top = 5.dp).size(8.dp)
                                        .background(VerdePrimario, shape = androidx.compose.foundation.shape.CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(n.titulo, fontSize = 13.sp,
                                        fontWeight = if (n.leida) FontWeight.Medium else FontWeight.Bold,
                                        color = TextoPrimario)
                                    Text(n.contenido, fontSize = 12.sp, color = TextoSecundario,
                                        modifier = Modifier.padding(top = 4.dp), lineHeight = 17.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
