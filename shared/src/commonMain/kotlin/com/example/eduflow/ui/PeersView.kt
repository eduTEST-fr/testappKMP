package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AutorApiDto(
    val id: Int, val nombre: String, val carrera: String = "",
    val cuatrimestre: Int = 1, val rol: String = "ALUMNO"
)

@Serializable
internal data class SolicitudApiDto(
    val id: Int, val titulo: String, val descripcion: String, val materia: String,
    val estado: String, val tieneImagen: Boolean, val createdAt: String, val autor: AutorApiDto
)

@Serializable
private data class MentorApiDto(
    val id: Int, val nombre: String, val carrera: String, val cuatrimestre: Int,
    val materiasDestaca: String, val rol: String, val avatarId: String,
    val promedio: Double, val totalCalif: Int, val permiteAsesoria: Boolean
)

@Serializable
private data class AdminSolDto(
    val id: Int, val titulo: String, val estado: String,
    val materia: String = "", val createdAt: String = "", val autor: String = ""
)

private val jsonPeers = Json { ignoreUnknownKeys = true }

private fun filtrarPorFecha(lista: List<SolicitudApiDto>, filtro: String): List<SolicitudApiDto> {
    if (filtro == "Todas") return lista
    val tz    = TimeZone.currentSystemDefault()
    val ahora = Clock.System.now().toLocalDateTime(tz)
    val hoyStr = "${ahora.year}-${ahora.monthNumber.toString().padStart(2,'0')}-${ahora.dayOfMonth.toString().padStart(2,'0')}"
    val mesStr = "${ahora.year}-${ahora.monthNumber.toString().padStart(2,'0')}"
    val hace7epoch = Clock.System.now().toEpochMilliseconds() - 7L * 24 * 60 * 60 * 1000
    val hace7 = Instant.fromEpochMilliseconds(hace7epoch).toLocalDateTime(tz)
    val hace7Str = "${hace7.year}-${hace7.monthNumber.toString().padStart(2,'0')}-${hace7.dayOfMonth.toString().padStart(2,'0')}"
    return lista.filter { sol ->
        try {
            val f = sol.createdAt.substring(0, 10)
            when (filtro) {
                "Hoy"         -> f == hoyStr
                "Esta semana" -> f >= hace7Str
                "Este mes"    -> f.startsWith(mesStr)
                else -> true
            }
        } catch (e: Exception) { true }
    }
}

@Composable
fun PeersView(
    onVolver: () -> Unit,
    onVerStudyCast: () -> Unit,
    onVerAudios: () -> Unit,
    onVerPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
    onVerDetalle: (Int) -> Unit
) {
    val rol = SesionStorage.obtenerRol()
    when (rol) {
        "ADMIN"  -> PeersAdminView(onCerrarSesion = onCerrarSesion, onVerDetalle = onVerDetalle)
        "ASESOR" -> PeersAsesorView(onCerrarSesion = onCerrarSesion, onVerDetalle = onVerDetalle)
        else     -> PeersAlumnoView(
            onVolver = onVolver, onVerStudyCast = onVerStudyCast,
            onVerAudios = onVerAudios, onVerPerfil = onVerPerfil,
            onVerDetalle = onVerDetalle
        )
    }
}

// ── VISTA ALUMNO ──────────────────────────────────────────────────────
@Composable
private fun PeersAlumnoView(
    onVolver: () -> Unit, onVerStudyCast: () -> Unit,
    onVerAudios: () -> Unit, onVerPerfil: () -> Unit,
    onVerDetalle: (Int) -> Unit
) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    val filtros = listOf("Todas", "Hoy", "Esta semana", "Este mes")
    var filtroActivo  by remember { mutableStateOf("Todas") }
    var mentores      by remember { mutableStateOf<List<MentorApiDto>>(emptyList()) }
    var solicitudes   by remember { mutableStateOf<List<SolicitudApiDto>>(emptyList()) }
    var cargando      by remember { mutableStateOf(true) }
    var mostrarNueva  by remember { mutableStateOf(false) }
    var generando     by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf("") }

    suspend fun cargar() {
        cargando = true
        try {
            val rm = client.get("${ApiConfig.BASE_URL}/peers/asesores").bodyAsText()
            mentores = jsonPeers.decodeFromString(rm)
            val rs = client.get("${ApiConfig.BASE_URL}/peers/solicitudes") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            solicitudes = jsonPeers.decodeFromString(rs)
        } catch (e: Exception) { errorMsg = "No se pudo cargar la Red de Apoyo." }
        cargando = false
    }

    LaunchedEffect(Unit) { cargar() }

    if (mostrarNueva) {
        NuevaSolicitudDialog(
            generando = generando, errorMsg = errorMsg,
            onCerrar  = { mostrarNueva = false; errorMsg = "" },
            onGuardar = { titulo, descripcion, materia ->
                generando = true; errorMsg = ""
                scope.launch {
                    try {
                        client.post("${ApiConfig.BASE_URL}/peers/solicitudes") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody("{\"titulo\":\"${escaparJson(titulo)}\"," +
                                "\"descripcion\":\"${escaparJson(descripcion)}\"," +
                                "\"materia\":\"${escaparJson(materia)}\"}")
                        }
                        mostrarNueva = false; cargar()
                    } catch (e: Exception) { errorMsg = "Error al publicar." }
                    generando = false
                }
            }
        )
        return
    }

    val solicitudesFiltradas = filtrarPorFecha(solicitudes, filtroActivo)

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("EduFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onVerPerfil() },
                    contentAlignment = Alignment.Center) {
                    AvatarIcono(avatarId = "avatar_1", sizeDp = 36)
                }
            }

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdePrimario)
                }
                return@Column
            }

            Column(modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 90.dp)) {

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VerdePrimario)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tu Red de Apoyo Académico", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 26.sp)
                        Text("Conecta con mentores y resuelve tus dudas en comunidad.",
                            fontSize = 13.sp, color = Color.White.copy(0.85f),
                            lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
                        Surface(modifier = Modifier.fillMaxWidth().clickable { mostrarNueva = true },
                            shape = RoundedCornerShape(10.dp), color = Color.White) {
                            Text("Publicar Solicitud", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = VerdePrimario,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("Asesores Destacados", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario,
                    modifier = Modifier.padding(bottom = 12.dp))

                if (mentores.isEmpty()) {
                    Text("Aún no hay asesores registrados.", fontSize = 12.sp,
                        color = TextoSecundario, modifier = Modifier.padding(bottom = 8.dp))
                } else {
                    mentores.take(1).forEach { MentorDestacadoCard(it) }
                    Spacer(Modifier.height(10.dp))
                    mentores.drop(1).take(2).forEach { MentorCompactoCard(it); Spacer(Modifier.height(8.dp)) }
                    Spacer(Modifier.height(4.dp))
                    Surface(modifier = Modifier.fillMaxWidth().clickable { /* TODO EP10: navegar a AsesoresListaView */ },
                        shape = RoundedCornerShape(10.dp), color = Color(0xFFDDE8E0)) {
                        Text("Ver todos los asesores (${mentores.size})", fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = VerdePrimario,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("Filtrar por fecha", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = TextoSecundario,
                    modifier = Modifier.padding(bottom = 10.dp))

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    filtros.forEach { filtro ->
                        Surface(modifier = Modifier.padding(end = 8.dp).clickable { filtroActivo = filtro },
                            shape = RoundedCornerShape(20.dp),
                            color = if (filtroActivo == filtro) VerdePrimario else Color(0xFFEEEEEE)) {
                            Text(filtro, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (filtroActivo == filtro) Color.White else TextoSecundario,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Solicitudes Recientes", fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario,
                    modifier = Modifier.padding(bottom = 10.dp))

                if (errorMsg.isNotEmpty())
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp))

                if (solicitudesFiltradas.isEmpty())
                    Text("No hay solicitudes en este período.", fontSize = 12.sp, color = TextoSecundario)
                else
                    solicitudesFiltradas.forEach { sol ->
                        SolicitudCard(sol = sol, onVerDetalle = onVerDetalle)
                        Spacer(Modifier.height(10.dp))
                    }

                Spacer(Modifier.height(10.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEAE0))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¿Necesitas ayuda tú también?", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Publica tu duda y recibe apoyo de la comunidad.",
                            fontSize = 12.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                        Surface(modifier = Modifier.fillMaxWidth().clickable { mostrarNueva = true },
                            shape = RoundedCornerShape(10.dp), color = VerdePrimario) {
                            Text("Crear Solicitud", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        }
                    }
                }
            }
        }

        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = Beige, shadowElevation = 8.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                BottomNavItem("Inicio",     false, "⊞", onVolver)
                BottomNavItem("StudyCast",  false, "▶", onVerStudyCast)
                BottomNavItem("Audio",      false, "♪", onVerAudios)
                BottomNavItem("Pares",      true,  "⊙")
            }
        }
    }
}

// ── VISTA ASESOR ──────────────────────────────────────────────────────
@Composable
private fun PeersAsesorView(onCerrarSesion: () -> Unit, onVerDetalle: (Int) -> Unit) {
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var solicitudes by remember { mutableStateOf<List<SolicitudApiDto>>(emptyList()) }
    var cargando    by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf("") }

    suspend fun cargar() {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/peers/solicitudes") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            solicitudes = jsonPeers.decodeFromString(resp)
        } catch (e: Exception) { errorMsg = "Error al cargar solicitudes." }
        cargando = false
    }

    LaunchedEffect(Unit) { cargar() }

    var mostrarMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Barra superior con hamburger igual que Dashboard
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { mostrarMenu = true }) {
                    repeat(3) {
                        Box(Modifier.width(22.dp).height(2.dp)
                            .background(VerdePrimario, RoundedCornerShape(1.dp)))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("EduFlow — Asesor", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(22.dp))
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

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VerdePrimario)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Panel Asesor", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${solicitudes.size} solicitudes abiertas. Pulsa una para responder o cerrarla.",
                            fontSize = 12.sp, color = Color.White.copy(0.85f),
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (errorMsg.isNotEmpty())
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp))

                if (solicitudes.isEmpty())
                    Text("No hay solicitudes abiertas.", fontSize = 13.sp, color = TextoSecundario)
                else
                    solicitudes.forEach { sol ->
                        SolicitudCard(sol = sol, onVerDetalle = onVerDetalle)
                        Spacer(Modifier.height(10.dp))
                    }
            }
        }

        // Menú lateral (igual que Dashboard)
        if (mostrarMenu) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { mostrarMenu = false }) {
                Card(modifier = Modifier.fillMaxHeight().width(260.dp)
                    .align(Alignment.CenterStart).clickable(enabled = false) {},
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(16.dp)) {
                    Column(modifier = Modifier.fillMaxHeight().padding(24.dp)
                        .windowInsetsPadding(WindowInsets.systemBars)) {
                        Text("EduFlow", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = VerdePrimario)
                        Spacer(Modifier.height(24.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDDE8E0)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp))
                                    .background(VerdePrimario), contentAlignment = Alignment.Center) {
                                    Text(SesionStorage.obtenerNombre().take(1).uppercase(),
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Asesor", fontSize = 11.sp, color = TextoSecundario)
                                    Text(SesionStorage.obtenerNombre(), fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = { mostrarMenu = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdePrimario),
                            border = BorderStroke(1.dp, VerdePrimario)) {
                            Text("Mi perfil", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { mostrarMenu = false; onCerrarSesion() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdePrimario),
                            border = BorderStroke(1.dp, VerdePrimario)) {
                            Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── VISTA ADMIN ───────────────────────────────────────────────────────
@Composable
private fun PeersAdminView(onCerrarSesion: () -> Unit, onVerDetalle: (Int) -> Unit) {
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var adminSols by remember { mutableStateOf<List<AdminSolDto>>(emptyList()) }
    var cargando  by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf("") }

    suspend fun cargar() {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/admin/peers/solicitudes") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            adminSols = jsonPeers.decodeFromString(resp)
        } catch (e: Exception) { errorMsg = "Error al cargar." }
        cargando = false
    }

    LaunchedEffect(Unit) { cargar() }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("EduFlow — Administrador", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onCerrarSesion, contentPadding = PaddingValues(0.dp)) {
                    Text("Salir", fontSize = 13.sp, color = VerdePrimario)
                }
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

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VerdePrimario)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Panel de Moderación", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${adminSols.size} solicitudes en total. Pulsa una para gestionarla.",
                            fontSize = 12.sp, color = Color.White.copy(0.85f),
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (errorMsg.isNotEmpty())
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp))

                adminSols.forEach { sol ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        .clickable { onVerDetalle(sol.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sol.titulo, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                                Text("Autor: ${sol.autor}", fontSize = 11.sp,
                                    color = TextoSecundario, modifier = Modifier.padding(top = 2.dp))
                                if (sol.materia.isNotBlank())
                                    Text(sol.materia, fontSize = 11.sp, color = VerdePrimario)
                            }
                            Surface(shape = RoundedCornerShape(8.dp),
                                color = if (sol.estado == "ABIERTA") Color(0xFFDDE8E0)
                                        else Color(0xFFEEEEEE)) {
                                Text(sol.estado, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = if (sol.estado == "ABIERTA") VerdePrimario else TextoSecundario,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Componentes compartidos ───────────────────────────────────────────
@Composable
private fun MentorDestacadoCard(mentor: MentorApiDto) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarIcono(avatarId = mentor.avatarId, sizeDp = 48)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (mentor.materiasDestaca.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEFE3C2)) {
                            Text(mentor.materiasDestaca.split(",").first().trim(),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B6914),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Text(mentor.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = TextoPrimario, modifier = Modifier.padding(top = 6.dp))
                }
                if (mentor.totalCalif > 0)
                    Text("★ ${"%.1f".format(mentor.promedio)}", fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF8B6914))
            }
            Text("${mentor.carrera} · ${mentor.cuatrimestre}° cuatrimestre",
                fontSize = 12.sp, color = TextoSecundario, lineHeight = 18.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 14.dp))
            if (mentor.permiteAsesoria) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    color = VerdePrimario) {
                    Text("Disponible para Asesoría", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun MentorCompactoCard(mentor: MentorApiDto) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarIcono(avatarId = mentor.avatarId, sizeDp = 40)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mentor.materiasDestaca.split(",").firstOrNull()?.trim() ?: "",
                    fontSize = 11.sp, color = VerdePrimario, fontWeight = FontWeight.SemiBold)
                Text(mentor.nombre, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = TextoPrimario)
            }
            if (mentor.totalCalif > 0)
                Text("★ ${"%.1f".format(mentor.promedio)}", fontSize = 11.sp,
                    color = Color(0xFF8B6914), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun SolicitudCard(sol: SolicitudApiDto, onVerDetalle: (Int) -> Unit) {
    val icono = when {
        sol.materia.contains("mat", true) || sol.materia.contains("calc", true) -> "Σ"
        sol.materia.contains("prog", true) || sol.materia.contains("cod", true) -> "<>"
        sol.materia.contains("fis", true) -> "λ"
        else -> "◈"
    }
    Card(modifier = Modifier.fillMaxWidth().clickable { onVerDetalle(sol.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFDDE8E0)), contentAlignment = Alignment.Center) {
                    Text(icono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(sol.titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = TextoPrimario, lineHeight = 18.sp)
                    if (sol.materia.isNotBlank())
                        Text(sol.materia, fontSize = 11.sp, color = VerdePrimario,
                            modifier = Modifier.padding(top = 2.dp))
                }
            }
            Text(sol.descripcion, fontSize = 12.sp, color = TextoSecundario, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 42.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(sol.autor.nombre, fontSize = 11.sp,
                    color = TextoSecundario, fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(8.dp), color = VerdePrimario,
                    modifier = Modifier.clickable { onVerDetalle(sol.id) }) {
                    Text("Ver detalle", fontSize = 11.sp, color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun NuevaSolicitudDialog(
    generando: Boolean, errorMsg: String,
    onCerrar: () -> Unit,
    onGuardar: (titulo: String, descripcion: String, materia: String) -> Unit
) {
    var titulo      by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var materia     by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCerrar, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("Nueva Solicitud", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        CampoTexto("Título", titulo, "Ej: Duda con derivadas parciales") { titulo = it }
                        Spacer(Modifier.height(12.dp))
                        CampoTexto("Materia", materia, "Ej: Cálculo") { materia = it }
                        Spacer(Modifier.height(12.dp))
                        Text("Descripción", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = descripcion, onValueChange = { descripcion = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Explica tu duda con detalle...",
                                color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VerdePrimario,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                        if (errorMsg.isNotEmpty())
                            Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { if (titulo.isNotBlank() && descripcion.isNotBlank())
                                onGuardar(titulo.trim(), descripcion.trim(), materia.trim()) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp), enabled = !generando,
                            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                        ) {
                            if (generando)
                                CircularProgressIndicator(color = Color.White,
                                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                Text("Publicar Solicitud", color = Color.White,
                                    fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
