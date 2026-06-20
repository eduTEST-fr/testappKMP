package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

enum class TabStudyCast { CONSEJOS, TARJETAS, PODCAST }

@Serializable
private data class MateriaApiDtoSC(val id: Int, val nombre: String, val dificultad: Int)

private val jsonParserSC = Json { ignoreUnknownKeys = true }

@Composable
fun StudyCastView(onVolver: () -> Unit, onVerPeers: () -> Unit) {
    val scope    = rememberCoroutineScope()
    val client   = remember { HttpClient() }
    val token    = SesionStorage.obtenerToken() ?: ""

    var materias by remember { mutableStateOf<List<MateriaUI>>(emptyList()) }
    var cargandoMaterias by remember { mutableStateOf(true) }
    var materiaSeleccionada by remember { mutableStateOf<MateriaUI?>(null) }
    var consejo  by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var tabActual by remember { mutableStateOf(TabStudyCast.CONSEJOS) }
    // Fechas de examen por materiaId, igual que en el Dashboard, para
    // bloquear la generacion de contenido el dia del examen.
    var fechasPorMateria by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/materias") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val listaDto = jsonParserSC.decodeFromString<List<MateriaApiDtoSC>>(resp)
            val lista = listaDto.map { MateriaUI(it.id, it.nombre, it.dificultad) }
            materias = lista

            val mapa = mutableMapOf<Int, List<String>>()
            lista.forEach { m ->
                try {
                    val respEx = client.get("${ApiConfig.BASE_URL}/materias/${m.id}/examenes") {
                        header("Authorization", "Bearer $token")
                    }.bodyAsText()
                    val rFecha = Regex(""""fecha":"([^"]+)"""")
                    mapa[m.id] = rFecha.findAll(respEx).map { it.groupValues[1] }.toList()
                } catch (e: Exception) {
                    mapa[m.id] = emptyList()
                }
            }
            fechasPorMateria = mapa
        } catch (e: Exception) {}
        cargandoMaterias = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onVolver,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("StudyFlow", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp)) // balance
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 90.dp)
            ) {
                Text("StudyCast", fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario)
                Text("Selecciona una materia y estudia\ncon ayuda de IA.",
                    fontSize = 13.sp, color = TextoSecundario, lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

                if (cargandoMaterias) {
                    Box(Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VerdePrimario)
                    }
                } else if (materias.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFDDE8E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("SF", fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("Sin materias registradas",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = TextoPrimario)
                            Text("Regresa al Dashboard y agrega\nuna materia primero.",
                                fontSize = 13.sp, color = TextoSecundario,
                                modifier = Modifier.padding(top = 6.dp),
                                textAlign = TextAlign.Center, lineHeight = 19.sp)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onVolver,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VerdePrimario)
                            ) {
                                Text("Ir al Dashboard", color = Color.White,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    Text("Tu biblioteca", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = TextoPrimario,
                        modifier = Modifier.padding(bottom = 10.dp))

                    materias.forEach { materia ->
                        val seleccionada = materiaSeleccionada?.id == materia.id
                        val bloqueada = tieneExamenHoy(fechasPorMateria[materia.id] ?: emptyList())
                        val simbolo = when {
                            materia.nombre.contains("mat", ignoreCase = true) ||
                                    materia.nombre.contains("calc", ignoreCase = true) ||
                                    materia.nombre.contains("alg", ignoreCase = true)  -> "∑"
                            materia.nombre.contains("prog", ignoreCase = true) ||
                                    materia.nombre.contains("cod", ignoreCase = true)  -> "<>"
                            materia.nombre.contains("fis", ignoreCase = true)  -> "λ"
                            else -> "◈"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable(enabled = !bloqueada) {
                                    materiaSeleccionada = materia
                                    consejo = ""
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    bloqueada    -> Color(0xFFF0F0F0)
                                    seleccionada -> VerdePrimario
                                    else         -> Color.White
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                if (seleccionada) 4.dp else 2.dp
                            )
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (seleccionada)
                                                    Color.White.copy(alpha = 0.18f)
                                                else Color(0xFFDDE8E0)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(simbolo, fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (seleccionada) Color.White
                                            else VerdePrimario)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(materia.nombre, fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (seleccionada) Color.White
                                            else TextoPrimario)
                                        Text("Dificultad: ${materia.dificultad}/10",
                                            fontSize = 12.sp,
                                            color = if (seleccionada)
                                                Color.White.copy(0.72f)
                                            else TextoSecundario)
                                    }
                                    if (seleccionada) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.18f)
                                        ) {
                                            Text("LISTO", fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                letterSpacing = 0.5.sp,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                                if (bloqueada) {
                                    Row(modifier = Modifier.padding(start = 14.dp, bottom = 10.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF8B6914)
                                        ) {
                                            Text("EXAMEN HOY · BLOQUEADO", fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Etiqueta de seccion: separa visualmente Consejos+Tarjetas (repaso
                    // rapido en texto) del grupo de Podcast (audio), como se pidio.
                    Text("Repaso rápido", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = TextoSecundario, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(TabStudyCast.CONSEJOS, TabStudyCast.TARJETAS).forEach { tab ->
                            val label = if (tab == TabStudyCast.CONSEJOS) "Consejo" else "Tarjetas"
                            Surface(
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                                    .clickable { tabActual = tab },
                                shape = RoundedCornerShape(10.dp),
                                color = if (tabActual == tab) VerdePrimario else Color(0xFFEEEEEE)
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (tabActual == tab) Color.White else TextoSecundario,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text("Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = TextoSecundario, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { tabActual = TabStudyCast.PODCAST },
                        shape = RoundedCornerShape(10.dp),
                        color = if (tabActual == TabStudyCast.PODCAST) VerdePrimario
                                else Color(0xFFEEEEEE)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("♪  ", fontSize = 13.sp,
                                color = if (tabActual == TabStudyCast.PODCAST) Color.White
                                        else TextoSecundario)
                            Text("Podcast con IA", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (tabActual == TabStudyCast.PODCAST) Color.White
                                        else TextoSecundario)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val materiaActualBloqueada = materiaSeleccionada?.let {
                        tieneExamenHoy(fechasPorMateria[it.id] ?: emptyList())
                    } ?: false

                    if (materiaActualBloqueada) {
                        // Si por alguna razon queda seleccionada una materia con
                        // examen hoy, se bloquea tambien la generacion de contenido.
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Contenido bloqueado", fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF8B6914))
                                Text("Hoy es el examen de ${materiaSeleccionada?.nombre}. " +
                                    "El contenido de IA se bloquea para que repases con lo " +
                                    "que ya preparaste.",
                                    fontSize = 13.sp, color = TextoSecundario,
                                    modifier = Modifier.padding(top = 6.dp), lineHeight = 19.sp)
                            }
                        }
                    } else {
                    when (tabActual) {
                        TabStudyCast.CONSEJOS -> {
                            Button(
                                onClick = {
                                    val materia = materiaSeleccionada ?: return@Button
                                    cargando = true
                                    consejo  = ""
                                    scope.launch {
                                        try {
                                            val resp = client.post(
                                                "${ApiConfig.BASE_URL}/consejos/generar"
                                            ) {
                                                header("Authorization", "Bearer $token")
                                                contentType(ContentType.Application.Json)
                                                setBody("""{"materia":"${materia.nombre}"}""")
                                            }.bodyAsText()
                                            val rCon = Regex(""""consejo":"([^"]+)"""")
                                            consejo = rCon.find(resp)?.groupValues?.get(1)
                                                ?: "Sin respuesta del servidor."
                                        } catch (e: Exception) {
                                            consejo = "Sin conexión al servidor."
                                        }
                                        cargando = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = materiaSeleccionada != null && !cargando,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VerdePrimario,
                                    disabledContainerColor = Color(0xFFCCCCCC)
                                )
                            ) {
                                if (cargando) {
                                    CircularProgressIndicator(color = Color.White,
                                        modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        if (materiaSeleccionada == null)
                                            "Selecciona una materia primero"
                                        else
                                            "Pedir consejo — ${materiaSeleccionada?.nombre}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (consejo.isNotEmpty()) {
                                Spacer(Modifier.height(20.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = VerdePrimario),
                                    elevation = CardDefaults.cardElevation(6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.18f)
                                        ) {
                                            Text("STUDYCAST",
                                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                color = Color.White, letterSpacing = 1.sp,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp, vertical = 4.dp))
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text("Consejo para ${materiaSeleccionada?.nombre}",
                                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(0.8f))
                                        Spacer(Modifier.height(8.dp))
                                        Text(consejo, color = Color.White,
                                            fontSize = 14.sp, lineHeight = 22.sp)
                                    }
                                }
                            }
                        }
                        TabStudyCast.TARJETAS -> TarjetasView(materia = materiaSeleccionada, token = token)
                        TabStudyCast.PODCAST  -> AudioPlayerView(materia = materiaSeleccionada, token = token)
                    }
                    }
                }
            }
        }

        // Bottom nav
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Beige,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem(label = "Dashboard", selected = false,
                    symbol = "⊞", onClick = onVolver)
                BottomNavItem(label = "StudyCast", selected = true, symbol = "▶")
                BottomNavItem(label = "Audio", selected = false, symbol = "♪",
                    onClick = { tabActual = TabStudyCast.PODCAST })
                BottomNavItem(label = "Peers", selected = false, symbol = "⊙",
                    onClick = onVerPeers)
            }
        }
    }
}
