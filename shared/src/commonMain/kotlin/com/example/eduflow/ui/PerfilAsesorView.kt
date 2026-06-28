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
private data class PerfilAsesorApiDto(
    val id: Int,
    val nombre: String,
    val carrera: String = "",
    val cuatrimestre: Int = 1,
    val sobreMi: String = "",
    val materiasDestaca: String = "",
    val rol: String = "ASESOR",
    val avatarId: String = "avatar_1",
    val grado: String = "",
    val especialidad: String = "",
    val permiteAsesoria: Boolean = false
)

@Serializable
private data class MentorPromedioApiDto(
    val id: Int, val promedio: Double = 0.0, val totalCalif: Int = 0
)

private val jsonPerfilAsesor = Json { ignoreUnknownKeys = true }

// Perfil público de un Asesor visto desde un Alumno: datos académicos,
// materias, promedio de estrellas y botón para agendar asesoría si el
// asesor tiene permiteAsesoria activo.
@Composable
fun PerfilAsesorView(asesorId: Int, onVolver: () -> Unit, onAgendar: (Int) -> Unit) {
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var perfil    by remember { mutableStateOf<PerfilAsesorApiDto?>(null) }
    var promedio  by remember { mutableStateOf(0.0) }
    var totalCalif by remember { mutableStateOf(0) }
    var cargando  by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf("") }

    LaunchedEffect(asesorId) {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/peers/perfil/$asesorId") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            perfil = jsonPerfilAsesor.decodeFromString(resp)

            try {
                val respLista = client.get("${ApiConfig.BASE_URL}/peers/asesores") {
                    header("Authorization", "Bearer $token")
                }.bodyAsText()
                val lista = jsonPerfilAsesor.decodeFromString<List<MentorPromedioApiDto>>(respLista)
                lista.find { it.id == asesorId }?.let {
                    promedio = it.promedio; totalCalif = it.totalCalif
                }
            } catch (e: Exception) { /* el perfil principal ya cargó, esto es secundario */ }
        } catch (e: Exception) { errorMsg = "No se pudo cargar el perfil del asesor." }
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
                Text("Perfil del Asesor", fontSize = 16.sp,
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

            if (errorMsg.isNotEmpty() || perfil == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(errorMsg.ifBlank { "No se encontró este perfil." },
                        color = Color(0xFFB00020), fontSize = 13.sp)
                }
                return@Column
            }

            val p = perfil!!

            Column(modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    AvatarIcono(avatarId = p.avatarId, sizeDp = 90)
                    Spacer(Modifier.height(12.dp))
                    Text(p.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                    Text("${p.carrera} · ${p.cuatrimestre}° cuatrimestre",
                        fontSize = 13.sp, color = TextoSecundario, modifier = Modifier.padding(top = 4.dp))
                    if (totalCalif > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("★ ${"%.1f".format(promedio)}  ($totalCalif calificaciones)",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B6914))
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (p.grado.isNotBlank() || p.especialidad.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (p.grado.isNotBlank())
                                Text("Grado académico: ${p.grado}", fontSize = 13.sp, color = TextoSecundario)
                            if (p.especialidad.isNotBlank())
                                Text("Especialidad: ${p.especialidad}", fontSize = 13.sp, color = TextoSecundario,
                                    modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("Sobre mí", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = TextoPrimario, modifier = Modifier.padding(bottom = 8.dp))
                Text(p.sobreMi.ifBlank { "Este asesor aún no agrega una descripción." },
                    fontSize = 13.sp, color = TextoSecundario, lineHeight = 19.sp)

                Spacer(Modifier.height(20.dp))

                val materias = p.materiasDestaca.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                Text("Materias en las que destaca", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = TextoPrimario, modifier = Modifier.padding(bottom = 10.dp))
                if (materias.isEmpty())
                    Text("Aún no agrega materias destacadas.", fontSize = 12.sp, color = TextoSecundario)
                else
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        materias.forEach { materia ->
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFDDE8E0),
                                modifier = Modifier.padding(end = 8.dp)) {
                                Text(materia, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = VerdePrimario,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                            }
                        }
                    }

                Spacer(Modifier.height(28.dp))

                if (p.permiteAsesoria) {
                    Button(
                        onClick = { onAgendar(p.id) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                    ) {
                        Text("Agendar asesoría", color = Color.White,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEAE0))) {
                        Text("Este asesor no está aceptando asesorías por el momento.",
                            fontSize = 12.sp, color = TextoSecundario,
                            modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
