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
import androidx.compose.ui.window.Dialog
import com.example.eduflow.config.ApiConfig
import com.example.eduflow.navigation.PlatformBackHandler
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Episodio(
    val id: Int, val titulo: String, val guion: String,
    val audioUrl: String, val tema: String, val completado: Boolean
)
data class SubcarpetaAudios(val tema: String, val episodios: List<Episodio>, val completado: Boolean)

@Serializable
private data class PodcastApiDto(
    val id: Int, val titulo: String, val guion: String, val audioUrl: String,
    val tema: String = "General", val completado: Boolean = false
)
@Serializable
private data class MateriaApiDtoAudios(val id: Int, val nombre: String, val dificultad: Int)

private val jsonParserAudios = Json { ignoreUnknownKeys = true }

// Biblioteca de Audios: materias > subcarpetas (temas) > episodios.
// Misma logica de carpetas que StudyCast pero aplicada a los podcasts con IA.
@Composable
fun AudiosView(onVolver: () -> Unit, onVerStudyCast: () -> Unit, onVerPeers: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var materias by remember { mutableStateOf<List<MateriaUI>>(emptyList()) }
    var cargandoMaterias by remember { mutableStateOf(true) }
    var materiaActiva by remember { mutableStateOf<MateriaUI?>(null) }
    var fechasPorMateria by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    var examenesCargados by remember { mutableStateOf<Set<Int>>(emptySet()) }

    PlatformBackHandler(enabled = materiaActiva != null) { materiaActiva = null }

    LaunchedEffect(Unit) {
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/materias") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val lista = jsonParserAudios.decodeFromString<List<MateriaApiDtoAudios>>(resp)
                .map { MateriaUI(it.id, it.nombre, it.dificultad) }
            materias = lista
            val mapa = mutableMapOf<Int, List<String>>()
            val cargados = mutableSetOf<Int>()
            lista.forEach { materia ->
                try {
                    val respuesta = client.get("${ApiConfig.BASE_URL}/materias/${materia.id}/examenes") {
                        header("Authorization", "Bearer $token")
                    }.bodyAsText()
                    val regex = Regex(""""fecha":"([^"]+)"""")
                    mapa[materia.id] = regex.findAll(respuesta).map { it.groupValues[1] }.toList()
                    cargados += materia.id
                } catch (_: Exception) { /* se mantiene como estado no validado */ }
            }
            fechasPorMateria = mapa
            examenesCargados = cargados
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
                BotonVolver(onClick = { if (materiaActiva != null) materiaActiva = null else onVolver() })
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
                    Text("Biblioteca de Audios", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                    Text("Tus StudyCasts generados con IA, organizados por materia.",
                        fontSize = 13.sp, color = TextoSecundario, lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 22.dp))

                    when {
                        cargandoMaterias -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VerdePrimario)
                        }
                        materias.isEmpty() -> EstadoVacioBiblioteca(onVolver)
                        else -> materias.forEach { m ->
                            val fechas = fechasPorMateria[m.id] ?: emptyList()
                            val bloqueo = if (m.id !in examenesCargados) {
                                BloqueoMateriaUI.NINGUNO
                            } else when {
                                tieneExamenHoy(fechas) -> BloqueoMateriaUI.EXAMEN_HOY
                                !tieneExamenMesActual(fechas) -> BloqueoMateriaUI.SIN_EXAMEN_MENSUAL
                                else -> BloqueoMateriaUI.NINGUNO
                            }
                            MateriaFolderCard(
                                nombre = m.nombre,
                                dificultad = m.dificultad,
                                icono = "♪",
                                bloqueo = bloqueo
                            ) { materiaActiva = m }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                } else {
                    val fechas = fechasPorMateria[materia.id] ?: emptyList()
                    val bloqueo = if (materia.id !in examenesCargados) {
                        BloqueoMateriaUI.NINGUNO
                    } else when {
                        tieneExamenHoy(fechas) -> BloqueoMateriaUI.EXAMEN_HOY
                        !tieneExamenMesActual(fechas) -> BloqueoMateriaUI.SIN_EXAMEN_MENSUAL
                        else -> BloqueoMateriaUI.NINGUNO
                    }
                    Text(materia.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                    Text("Dificultad ${materia.dificultad}/10 · Audios con IA",
                        fontSize = 12.sp, color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp, bottom = 18.dp))
                    if (bloqueo == BloqueoMateriaUI.NINGUNO) {
                        AudiosBibliotecaMateria(materia = materia, token = token)
                    } else {
                        MaterialBloqueadoAudio(bloqueo, materia.nombre)
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
                BottomNavItem("Inicio", false, NavIcono.INICIO, onVolver)
                BottomNavItem("Tarjetas", false, NavIcono.TARJETAS, onVerStudyCast)
                BottomNavItem("Audios", true, NavIcono.AUDIOS)
                BottomNavItem("Comunidad", false, NavIcono.COMUNIDAD, onVerPeers)
            }
        }
    }
}

@Composable
private fun MaterialBloqueadoAudio(bloqueo: BloqueoMateriaUI, materia: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bloqueo == BloqueoMateriaUI.EXAMEN_HOY) Color(0xFFFFF3E0) else Color(0xFFDDE8E0)
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                if (bloqueo == BloqueoMateriaUI.EXAMEN_HOY) "Audios bloqueados por examen" else "Configura el examen del mes",
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = if (bloqueo == BloqueoMateriaUI.EXAMEN_HOY) Color(0xFF8B6914) else VerdePrimario
            )
            Text(
                if (bloqueo == BloqueoMateriaUI.EXAMEN_HOY)
                    "Hoy es el examen de $materia. No se permite reproducir ni generar audios durante este día."
                else
                    "Registra en Inicio una evaluación del mes actual para habilitar esta biblioteca y recibir recordatorios de repaso.",
                fontSize = 13.sp, color = TextoSecundario, lineHeight = 19.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun EstadoVacioBiblioteca(onVolver: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sin materias registradas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
            Text("Regresa a Inicio y agrega una materia primero.",
                fontSize = 13.sp, color = TextoSecundario, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp), lineHeight = 19.sp)
            Button(onClick = onVolver, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)) {
                Text("Ir a Inicio", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MateriaFolderCard(
    nombre: String,
    dificultad: Int,
    icono: String,
    bloqueo: BloqueoMateriaUI = BloqueoMateriaUI.NINGUNO,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bloqueo == BloqueoMateriaUI.NINGUNO) Color.White else Color(0xFFF1F1ED)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            MateriaIlustracion(
                nombre = nombre,
                modifier = Modifier.fillMaxWidth().height(110.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                    Text(
                        when (bloqueo) {
                            BloqueoMateriaUI.EXAMEN_HOY -> "Examen hoy · materiales bloqueados"
                            BloqueoMateriaUI.SIN_EXAMEN_MENSUAL -> "Falta examen del mes"
                            BloqueoMateriaUI.NINGUNO -> "Dificultad $dificultad/10"
                        },
                        fontSize = 12.sp,
                        color = if (bloqueo == BloqueoMateriaUI.EXAMEN_HOY) Color(0xFF8B6914) else TextoSecundario
                    )
                }
                Text(if (bloqueo == BloqueoMateriaUI.NINGUNO) "›" else "•", fontSize = 20.sp, color = TextoSecundario)
            }
        }
    }
}

@Composable
private fun AudiosBibliotecaMateria(materia: MateriaUI, token: String) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }

    var episodios by remember { mutableStateOf<List<Episodio>>(emptyList()) }
    var cargando  by remember { mutableStateOf(true) }
    var temaActivo by remember { mutableStateOf<String?>(null) }
    var mostrarNuevo by remember { mutableStateOf(false) }
    var generando by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    PlatformBackHandler(enabled = temaActivo != null || mostrarNuevo) {
        if (mostrarNuevo) mostrarNuevo = false else temaActivo = null
    }

    suspend fun recargar() {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/podcasts/${materia.id}") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            episodios = jsonParserAudios.decodeFromString<List<PodcastApiDto>>(resp)
                .map { Episodio(it.id, it.titulo, it.guion, it.audioUrl, it.tema, it.completado) }
        } catch (e: Exception) {}
        cargando = false
    }

    LaunchedEffect(materia.id) { recargar() }

    val subcarpetas = remember(episodios) {
        episodios.groupBy { it.tema }
            .map { (tema, lista) -> SubcarpetaAudios(tema, lista, lista.all { it.completado }) }
            .sortedBy { it.tema }
    }
    val activa = subcarpetas.find { it.tema == temaActivo }

    if (mostrarNuevo) {
        NuevoAudioDialog(
            generando = generando, errorMsg = errorMsg,
            onCerrar = { mostrarNuevo = false; errorMsg = "" },
            onGuardar = { tema ->
                generando = true; errorMsg = ""
                scope.launch {
                    try {
                        val respuesta = client.post("${ApiConfig.BASE_URL}/podcasts/generar") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                "{\"materiaId\":${materia.id},\"examenId\":0," +
                                "\"materia\":\"${escaparJson(materia.nombre)}\",\"tema\":\"${escaparJson(tema)}\"}"
                            )
                        }
                        when (respuesta.status) {
                            HttpStatusCode.UnprocessableEntity -> errorMsg = "La IA no pudo generar el guión. Intenta de nuevo."
                            HttpStatusCode.ServiceUnavailable  -> errorMsg = "No se pudo generar el audio. Intenta en unos segundos."
                            else -> { recargar(); mostrarNuevo = false; temaActivo = tema }
                        }
                    } catch (e: Exception) { errorMsg = "Error al generar. Revisa tu conexión." }
                    generando = false
                }
            }
        )
        return
    }

    when {
        cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VerdePrimario)
        }
        activa != null -> SubcarpetaAudiosDetalle(
            materia = materia, subcarpeta = activa, token = token,
            onVolver = { temaActivo = null },
            onCompletado = { episodioId ->
                scope.launch {
                    try {
                        client.put("${ApiConfig.BASE_URL}/podcasts/$episodioId/completar") {
                            header("Authorization", "Bearer $token")
                        }
                        recargar()
                    } catch (e: Exception) {}
                }
            }
        )
        else -> Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subcarpetas de audio", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = TextoPrimario, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = VerdePrimario,
                    modifier = Modifier.clickable { mostrarNuevo = true }
                ) {
                    Text("+ Nuevo", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }

            if (subcarpetas.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aún no hay audios", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Genera tu primer StudyCast por tema para esta materia.",
                            fontSize = 12.sp, color = TextoSecundario, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                subcarpetas.forEach { sub ->
                    SubcarpetaAudioCard(sub) { temaActivo = sub.tema }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SubcarpetaAudioCard(sub: SubcarpetaAudios, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFDDE8E0)),
                contentAlignment = Alignment.Center
            ) { Text("♪", fontSize = 18.sp, color = VerdePrimario) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sub.tema, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                Text("${sub.episodios.size} episodio${if (sub.episodios.size != 1) "s" else ""}",
                    fontSize = 12.sp, color = TextoSecundario)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (sub.completado) Color(0xFFDDE8E0) else Color(0xFFFFF3E0)
            ) {
                Text(
                    if (sub.completado) "Escuchado" else "Pendiente",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = if (sub.completado) VerdePrimario else Color(0xFF8B6914),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SubcarpetaAudiosDetalle(
    materia: MateriaUI, subcarpeta: SubcarpetaAudios, token: String,
    onVolver: () -> Unit, onCompletado: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonVolver(onClick = onVolver)
            Spacer(Modifier.width(8.dp))
            Text("Subcarpetas", fontSize = 13.sp, color = VerdePrimario, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(subcarpeta.tema, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
        Text("${subcarpeta.episodios.size} episodio${if (subcarpeta.episodios.size != 1) "s" else ""} · ${materia.nombre}",
            fontSize = 12.sp, color = TextoSecundario, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

        subcarpeta.episodios.forEach { ep ->
            EpisodioCard(episodio = ep, token = token, onCompletado = { onCompletado(ep.id) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun EpisodioCard(episodio: Episodio, token: String, onCompletado: () -> Unit) {
    var mostrarReproductor by remember(episodio.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VerdePrimario),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.18f)) {
                Text("STUDYCAST PODCAST", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(episodio.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (episodio.guion.isNotEmpty())
                Text(episodio.guion.take(120) + "...", fontSize = 12.sp,
                    color = Color.White.copy(0.72f), modifier = Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrarReproductor = !mostrarReproductor },
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(VerdePrimario),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (mostrarReproductor) "—" else "▶", color = Color.White,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (mostrarReproductor) "Ocultar reproductor" else "Escuchar episodio",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VerdePrimario
                        )
                        Text(
                            if (mostrarReproductor) "Los controles permanecen disponibles aquí"
                            else "Reproducir, pausar o saltar 10 segundos",
                            fontSize = 10.sp,
                            color = TextoSecundario
                        )
                    }
                    Text(if (mostrarReproductor) "⌃" else "⌄", color = VerdePrimario, fontSize = 16.sp)
                }
            }

            if (mostrarReproductor) {
                Spacer(Modifier.height(10.dp))
                AudioPlayerWidget(
                    audioUrl = episodio.audioUrl,
                    token = token,
                    onCompleted = {
                        if (!episodio.completado) onCompletado()
                    }
                )
            }

            Spacer(Modifier.height(14.dp))
            if (episodio.completado) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text("✓ Escuchado por completo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onCompletado() },
                    shape = RoundedCornerShape(10.dp), color = Color.White
                ) {
                    Text("Marcar como escuchado", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = VerdePrimario, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun NuevoAudioDialog(
    generando: Boolean,
    errorMsg: String,
    onGuardar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var tema by remember { mutableStateOf("") }
    var validacion by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!generando) onCerrar() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(23.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFDDE8E0), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("♪", fontSize = 20.sp, color = VerdePrimario, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Crear StudyCast", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Audio breve para repasar un tema puntual", fontSize = 12.sp, color = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Tema del episodio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = tema,
                    onValueChange = { tema = it.take(120); validacion = "" },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Ej. Recursividad y casos base", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdePrimario,
                        unfocusedBorderColor = Color(0xFFE1E1DC),
                        focusedContainerColor = Color(0xFFFCFCFA),
                        unfocusedContainerColor = Color(0xFFFCFCFA)
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF4F6F1),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(
                        "Se generará un guion y su audio. Después podrás reproducirlo, pausarlo y saltar 10 segundos.",
                        fontSize = 11.sp, color = TextoSecundario, lineHeight = 17.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                val mensaje = validacion.ifBlank { errorMsg }
                if (mensaje.isNotBlank()) Text(mensaje, color = Color(0xFFB00020), fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCerrar,
                        enabled = !generando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, VerdePrimario)
                    ) { Text("Cancelar", color = VerdePrimario, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = {
                            if (tema.isBlank()) validacion = "Describe el tema del episodio."
                            else onGuardar(tema.trim())
                        },
                        enabled = !generando,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                    ) {
                        if (generando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Generar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
