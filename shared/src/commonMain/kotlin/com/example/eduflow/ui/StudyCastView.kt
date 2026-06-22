package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.config.ApiConfig
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.ui.graphics.Color

@Serializable
private data class MateriaApiDtoSC(val id: Int, val nombre: String, val dificultad: Int)

private val jsonParserSC = Json { ignoreUnknownKeys = true }

// Biblioteca de Estudio: materias > (consejo general + subcarpetas de tarjetas).
@Composable
fun StudyCastView(onVolver: () -> Unit, onVerAudios: () -> Unit, onVerPeers: () -> Unit) {
    val scope    = rememberCoroutineScope()
    val client   = remember { HttpClient() }
    val token    = SesionStorage.obtenerToken() ?: ""

    var materias by remember { mutableStateOf<List<MateriaUI>>(emptyList()) }
    var cargandoMaterias by remember { mutableStateOf(true) }
    var materiaActiva by remember { mutableStateOf<MateriaUI?>(null) }
    var fechasPorMateria by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/materias") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val lista = jsonParserSC.decodeFromString<List<MateriaApiDtoSC>>(resp)
                .map { MateriaUI(it.id, it.nombre, it.dificultad) }
            materias = lista

            val mapa = mutableMapOf<Int, List<String>>()
            lista.forEach { m ->
                try {
                    val respEx = client.get("${ApiConfig.BASE_URL}/materias/${m.id}/examenes") {
                        header("Authorization", "Bearer $token")
                    }.bodyAsText()
                    val rFecha = Regex(""""fecha":"([^"]+)"""")
                    mapa[m.id] = rFecha.findAll(respEx).map { it.groupValues[1] }.toList()
                } catch (e: Exception) { mapa[m.id] = emptyList() }
            }
            fechasPorMateria = mapa
        } catch (e: Exception) {}
        cargandoMaterias = false
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Beige).windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (materiaActiva != null) materiaActiva = null else onVolver() },
                    contentPadding = PaddingValues(0.dp)
                ) { Text("←", fontSize = 20.sp, color = VerdePrimario) }
                Spacer(Modifier.weight(1f))
                Text("EduFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 90.dp)
            ) {
                val materia = materiaActiva
                if (materia == null) {
                    Text("Biblioteca de Estudio", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                    Text("Selecciona una materia para ver tus consejos y tarjetas.",
                        fontSize = 13.sp, color = TextoSecundario, lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 22.dp))

                    when {
                        cargandoMaterias -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VerdePrimario)
                        }
                        materias.isEmpty() -> Card(
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Sin materias registradas", fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold, color = TextoPrimario)
                                Text("Regresa al Dashboard y agrega una materia primero.",
                                    fontSize = 13.sp, color = TextoSecundario, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp), lineHeight = 19.sp)
                                Button(onClick = onVolver, shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)) {
                                    Text("Ir al Dashboard", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        else -> materias.forEach { m ->
                            val bloqueada = tieneExamenHoy(fechasPorMateria[m.id] ?: emptyList())
                            MateriaFolderCard(
                                nombre = m.nombre, dificultad = m.dificultad,
                                icono = if (bloqueada) "⏵" else "▶"
                            ) { if (!bloqueada) materiaActiva = m }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                } else {
                    val bloqueada = tieneExamenHoy(fechasPorMateria[materia.id] ?: emptyList())
                    Text(materia.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                    Text("Dificultad ${materia.dificultad}/10", fontSize = 12.sp, color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp, bottom = 18.dp))

                    if (bloqueada) {
                        Card(
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Contenido bloqueado", fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF8B6914))
                                Text("Hoy es el examen de ${materia.nombre}. El contenido de IA se " +
                                    "bloquea para que repases con lo que ya preparaste.",
                                    fontSize = 13.sp, color = TextoSecundario,
                                    modifier = Modifier.padding(top = 6.dp), lineHeight = 19.sp)
                            }
                        }
                    } else {
                        ConsejoMateriaCard(materia = materia, token = token)
                        Spacer(Modifier.height(22.dp))
                        TarjetasBibliotecaView(materia = materia, token = token)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = Beige, shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem(label = "Dashboard", selected = false, symbol = "⊞", onClick = onVolver)
                BottomNavItem(label = "StudyCast", selected = true, symbol = "▶")
                BottomNavItem(label = "Audio", selected = false, symbol = "♪", onClick = onVerAudios)
                BottomNavItem(label = "Peers", selected = false, symbol = "⊙", onClick = onVerPeers)
            }
        }
    }
}

@Composable
private fun ConsejoMateriaCard(materia: MateriaUI, token: String) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    var consejo  by remember(materia.id) { mutableStateOf("") }
    var cargando by remember(materia.id) { mutableStateOf(false) }

    Column {
        Text("Consejo de estudio", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = TextoPrimario, modifier = Modifier.padding(bottom = 10.dp))

        Button(
            onClick = {
                cargando = true; consejo = ""
                scope.launch {
                    try {
                        val resp = client.post("${ApiConfig.BASE_URL}/consejos/generar") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody("{\"materia\":\"${escaparJson(materia.nombre)}\"}")
                        }.bodyAsText()
                        val rCon = Regex(""""consejo":"([^"]+)"""")
                        consejo = rCon.find(resp)?.groupValues?.get(1) ?: "Sin respuesta del servidor."
                    } catch (e: Exception) { consejo = "Sin conexión al servidor." }
                    cargando = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !cargando,
            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
        ) {
            if (cargando)
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else
                Text(
                    if (consejo.isEmpty()) "Pedir consejo de estudio" else "Pedir otro consejo",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
        }

        if (consejo.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VerdePrimario)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.18f)) {
                        Text("STUDYCAST", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(consejo, color = Color.White, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}
