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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class MentorListaApiDto(
    val id: Int, val nombre: String, val carrera: String, val cuatrimestre: Int,
    val materiasDestaca: String, val rol: String, val avatarId: String,
    val promedio: Double, val totalCalif: Int, val permiteAsesoria: Boolean
)

private val jsonAsesoresLista = Json { ignoreUnknownKeys = true }

// Lista completa de asesores (GET /peers/asesores), ordenados ya por el
// backend según calificación promedio. Al tocar uno se navega a su perfil.
@Composable
fun AsesoresListaView(onVolver: () -> Unit, onVerPerfilAsesor: (Int) -> Unit) {
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var asesores  by remember { mutableStateOf<List<MentorListaApiDto>>(emptyList()) }
    var cargando  by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/peers/asesores") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            asesores = jsonAsesoresLista.decodeFromString(resp)
        } catch (e: Exception) { errorMsg = "No se pudieron cargar los asesores." }
        cargando = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("Todos los Asesores", fontSize = 16.sp,
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

                if (asesores.isEmpty())
                    Text("Aún no hay asesores registrados.", fontSize = 13.sp, color = TextoSecundario)
                else
                    asesores.forEach { asesor ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                .clickable { onVerPerfilAsesor(asesor.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                AvatarIcono(avatarId = asesor.avatarId, sizeDp = 48)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(asesor.nombre, fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold, color = TextoPrimario)
                                    Text("${asesor.carrera} · ${asesor.cuatrimestre}° cuatrimestre",
                                        fontSize = 12.sp, color = TextoSecundario,
                                        modifier = Modifier.padding(top = 2.dp))
                                    if (asesor.materiasDestaca.isNotBlank())
                                        Text(asesor.materiasDestaca.split(",").take(2).joinToString(" · ") { it.trim() },
                                            fontSize = 11.sp, color = VerdePrimario,
                                            modifier = Modifier.padding(top = 4.dp))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (asesor.totalCalif > 0)
                                        Text("★ ${"%.1f".format(asesor.promedio)}", fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold, color = Color(0xFF8B6914))
                                    if (asesor.permiteAsesoria) {
                                        Spacer(Modifier.height(4.dp))
                                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDDE8E0)) {
                                            Text("Disponible", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                                color = VerdePrimario,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}
