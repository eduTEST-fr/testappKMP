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
import com.example.eduflow.storage.PerfilStorage
import com.example.eduflow.util.formatUnDecimal
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AutorApiDto(
    val id: Int, val nombre: String, val carrera: String = "",
    val cuatrimestre: Int = 1, val rol: String = "ALUMNO",
    val avatarId: String = "student_buho"
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

@Serializable
private data class PerfilPeersDto(
    val avatarId: String = "student_buho",
    val rol: String = "ALUMNO",
    val grado: String = "LICENCIATURA",
    val especialidad: String = "",
    val permiteAsesoria: Boolean = false
)

@Serializable
private data class AsesoriaPanelDto(
    val id: Int,
    val estado: String = "PENDIENTE"
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
    onVerDetalle: (Int) -> Unit,
    onVerTodosAsesores: () -> Unit,
    onVerPerfilAsesor: (Int) -> Unit,
    onVerMisAsesorias: () -> Unit
) {
    val rol = SesionStorage.obtenerRol()
    when (rol) {
        "ADMIN"  -> PeersAdminView(onCerrarSesion = onCerrarSesion, onVerDetalle = onVerDetalle)
        "ASESOR" -> PeersAsesorView(
            onCerrarSesion = onCerrarSesion, onVerDetalle = onVerDetalle,
            onVerMisAsesorias = onVerMisAsesorias, onVerPerfil = onVerPerfil
        )
        else     -> PeersAlumnoView(
            onVolver = onVolver, onVerStudyCast = onVerStudyCast,
            onVerAudios = onVerAudios, onVerPerfil = onVerPerfil,
            onVerDetalle = onVerDetalle,
            onVerTodosAsesores = onVerTodosAsesores,
            onVerPerfilAsesor = onVerPerfilAsesor
        )
    }
}

// ── VISTA ALUMNO ──────────────────────────────────────────────────────
@Composable
private fun PeersAlumnoView(
    onVolver: () -> Unit, onVerStudyCast: () -> Unit,
    onVerAudios: () -> Unit, onVerPerfil: () -> Unit,
    onVerDetalle: (Int) -> Unit,
    onVerTodosAsesores: () -> Unit,
    onVerPerfilAsesor: (Int) -> Unit
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
    var avatarId      by remember { mutableStateOf(PerfilStorage.obtenerAvatar()) }

    suspend fun cargar() {
        cargando = true
        try {
            val rm = client.get("${ApiConfig.BASE_URL}/peers/asesores").bodyAsText()
            mentores = jsonPeers.decodeFromString(rm)
            val rs = client.get("${ApiConfig.BASE_URL}/peers/solicitudes") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            solicitudes = jsonPeers.decodeFromString(rs)
            try {
                val rp = client.get("${ApiConfig.BASE_URL}/peers/perfil") {
                    header("Authorization", "Bearer $token")
                }.bodyAsText()
                val perfil = jsonPeers.decodeFromString<PerfilPeersDto>(rp)
                avatarId = perfil.avatarId
                PerfilStorage.guardarAvatar(avatarId)
            } catch (_: Exception) { }
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
                BotonVolver(onClick = onVolver)
                Spacer(Modifier.weight(1f))
                Text("EduFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onVerPerfil() },
                    contentAlignment = Alignment.Center) {
                    AvatarIcono(avatarId = avatarId, sizeDp = 36)
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
                    mentores.take(1).forEach { MentorDestacadoCard(it) { onVerPerfilAsesor(it.id) } }
                    Spacer(Modifier.height(10.dp))
                    mentores.drop(1).take(2).forEach {
                        MentorCompactoCard(it) { onVerPerfilAsesor(it.id) }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(modifier = Modifier.fillMaxWidth().clickable { onVerTodosAsesores() },
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
                BottomNavItem("Inicio", false, NavIcono.INICIO, onVolver)
                BottomNavItem("Tarjetas", false, NavIcono.TARJETAS, onVerStudyCast)
                BottomNavItem("Audios", false, NavIcono.AUDIOS, onVerAudios)
                BottomNavItem("Comunidad", true, NavIcono.COMUNIDAD)
            }
        }
    }
}

// ── VISTA ASESOR ──────────────────────────────────────────────────────
@Composable
private fun PeersAsesorView(
    onCerrarSesion: () -> Unit,
    onVerDetalle: (Int) -> Unit,
    onVerMisAsesorias: () -> Unit,
    onVerPerfil: () -> Unit
) {
    val client = remember { HttpClient() }
    val token = SesionStorage.obtenerToken() ?: ""

    var solicitudes by remember { mutableStateOf<List<SolicitudApiDto>>(emptyList()) }
    var asesorias by remember { mutableStateOf<List<AsesoriaPanelDto>>(emptyList()) }
    var perfil by remember { mutableStateOf(PerfilPeersDto(rol = "ASESOR")) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var mostrarMenu by remember { mutableStateOf(false) }

    suspend fun cargar() {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/peers/solicitudes") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            solicitudes = jsonPeers.decodeFromString(resp)

            val perfilResp = client.get("${ApiConfig.BASE_URL}/peers/perfil") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            perfil = jsonPeers.decodeFromString(perfilResp)

            val asesoriasResp = client.get("${ApiConfig.BASE_URL}/asesorias/mis-asesorias") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            asesorias = jsonPeers.decodeFromString(asesoriasResp)
        } catch (_: Exception) {
            errorMsg = "No se pudo actualizar el panel del asesor."
        }
        cargando = false
    }

    LaunchedEffect(Unit) { cargar() }

    val pendientes = asesorias.count { it.estado == "PENDIENTE" }
    val aceptadas = asesorias.count { it.estado == "ACEPTADA" }
    val gestionadas = asesorias.count { it.estado != "PENDIENTE" }
    val totalActividad = (solicitudes.size + asesorias.size).coerceAtLeast(1)
    val avatarAsesor = avatarAsesorPorGrado(perfil.grado)

    Box(
        modifier = Modifier.fillMaxSize().background(Beige).windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { mostrarMenu = true }.padding(4.dp)
                ) {
                    repeat(3) { Box(Modifier.width(22.dp).height(2.dp).background(VerdePrimario, RoundedCornerShape(1.dp))) }
                }
                Spacer(Modifier.weight(1f))
                Text("Panel del asesor", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { onVerPerfil() }) {
                    AvatarIcono(avatarAsesor, 40)
                }
            }

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdePrimario)
                }
                return@Column
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 28.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = VerdePrimario)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarIcono(avatarAsesor, 68)
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Hola, ${SesionStorage.obtenerNombre()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "${etiquetaGrado(perfil.grado)}${perfil.especialidad.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
                                fontSize = 12.sp, color = Color.White.copy(alpha = .82f),
                                modifier = Modifier.padding(top = 3.dp)
                            )
                            Text(
                                "Aquí puedes responder dudas y administrar tus asesorías. Los materiales del alumno permanecen privados.",
                                fontSize = 11.sp, color = Color.White.copy(alpha = .78f), lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricaAsesor("Dudas", solicitudes.count { it.estado == "ABIERTA" }.toString(), "por atender", Modifier.weight(1f))
                    MetricaAsesor("Asesorías", pendientes.toString(), "pendientes", Modifier.weight(1f))
                    MetricaAsesor("Aceptadas", aceptadas.toString(), "confirmadas", Modifier.weight(1f))
                }

                Spacer(Modifier.height(18.dp))
                Text("Actividad académica", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                Text("Distribución de tus tareas recientes", fontSize = 11.sp, color = TextoSecundario, modifier = Modifier.padding(top = 2.dp, bottom = 11.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        BarraActividadAsesor("Solicitudes abiertas", solicitudes.count { it.estado == "ABIERTA" }, totalActividad)
                        BarraActividadAsesor("Asesorías aceptadas", aceptadas, totalActividad)
                        BarraActividadAsesor("Asesorías gestionadas", gestionadas, totalActividad)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccesoAsesor(
                        titulo = "Mis asesorías",
                        descripcion = "Aceptar, cancelar o consultar agenda",
                        modifier = Modifier.weight(1f),
                        onClick = onVerMisAsesorias
                    )
                    AccesoAsesor(
                        titulo = "Mi perfil",
                        descripcion = "Grado, especialidad y disponibilidad",
                        modifier = Modifier.weight(1f),
                        onClick = onVerPerfil
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text("Solicitudes de la comunidad", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                Text("Selecciona una para responder o cerrarla", fontSize = 11.sp, color = TextoSecundario, modifier = Modifier.padding(top = 2.dp, bottom = 11.dp))
                if (errorMsg.isNotBlank()) Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp, modifier = Modifier.padding(bottom = 9.dp))
                if (solicitudes.isEmpty()) {
                    Surface(shape = RoundedCornerShape(15.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Text("No hay solicitudes abiertas por el momento.", fontSize = 13.sp, color = TextoSecundario, modifier = Modifier.padding(18.dp))
                    }
                } else {
                    solicitudes.take(8).forEach { solicitud ->
                        SolicitudCard(sol = solicitud, onVerDetalle = onVerDetalle)
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
        }

        if (mostrarMenu) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)).clickable { mostrarMenu = false }
            ) {
                Card(
                    modifier = Modifier.fillMaxHeight().width(272.dp).align(Alignment.CenterStart).clickable(enabled = false) {},
                    shape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxHeight().padding(24.dp).windowInsetsPadding(WindowInsets.systemBars)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EduFlowMark(Modifier.size(40.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("EduFlow", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                        }
                        Spacer(Modifier.height(26.dp))
                        Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFDDE8E0)) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                AvatarIcono(avatarAsesor, 42)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(etiquetaGrado(perfil.grado), fontSize = 11.sp, color = TextoSecundario)
                                    Text(SesionStorage.obtenerNombre(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { mostrarMenu = false; onVerPerfil() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, VerdePrimario)
                        ) { Text("Mi perfil", color = VerdePrimario, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { mostrarMenu = false; onVerMisAsesorias() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, VerdePrimario)
                        ) { Text("Mis asesorías", color = VerdePrimario, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { mostrarMenu = false; onCerrarSesion() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, VerdePrimario)
                        ) { Text("Cerrar sesión", color = VerdePrimario, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricaAsesor(titulo: String, valor: String, detalle: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 14.dp)) {
            Text(titulo, fontSize = 10.sp, color = TextoSecundario, maxLines = 1)
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
            Text(detalle, fontSize = 9.sp, color = TextoSecundario, maxLines = 1)
        }
    }
}

@Composable
private fun BarraActividadAsesor(etiqueta: String, valor: Int, total: Int) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(etiqueta, fontSize = 11.sp, color = TextoPrimario, modifier = Modifier.weight(1f))
            Text(valor.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { (valor.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = VerdePrimario,
            trackColor = Color(0xFFE8ECE6)
        )
    }
}

@Composable
private fun AccesoAsesor(titulo: String, descripcion: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE8E0))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
            Text(descripcion, fontSize = 10.sp, color = TextoSecundario, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Text("Abrir  →", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = VerdePrimario, modifier = Modifier.padding(top = 10.dp))
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
private fun MentorDestacadoCard(mentor: MentorApiDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(18.dp),
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
                    Text("★ ${formatUnDecimal(mentor.promedio)}", fontSize = 12.sp,
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
private fun MentorCompactoCard(mentor: MentorApiDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp),
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
                Text("★ ${formatUnDecimal(mentor.promedio)}", fontSize = 11.sp,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarIcono(sol.autor.avatarId, 25)
                    Spacer(Modifier.width(7.dp))
                    Text(sol.autor.nombre, fontSize = 11.sp,
                        color = TextoSecundario, fontWeight = FontWeight.SemiBold)
                }
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
                BotonVolver(onClick = onCerrar)
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
