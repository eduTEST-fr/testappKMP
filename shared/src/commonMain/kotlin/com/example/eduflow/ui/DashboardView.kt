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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.eduflow.config.ApiConfig
import com.example.eduflow.storage.PerfilStorage
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Modelo local con ID, según EP6 (reemplaza Pair<String,Int> en memoria)
data class MateriaUI(val id: Int, val nombre: String, val dificultad: Int)

// Refleja exactamente el MateriaDto del backend.
@Serializable
private data class MateriaApiDto(val id: Int, val nombre: String, val dificultad: Int)

@Serializable
private data class NotificacionResumenDto(val id: Int, val leida: Boolean)

@Serializable
private data class PerfilDashboardDto(
    val avatarId: String = "student_buho",
    val rol: String = "ALUMNO",
    val grado: String = ""
)

enum class BloqueoMateriaUI { NINGUNO, SIN_EXAMEN_MENSUAL, EXAMEN_HOY }

private val jsonParserDashboard = Json { ignoreUnknownKeys = true }

@Composable
fun DashboardView(
    onVerStudyCast: () -> Unit,
    onVerAudios: () -> Unit,
    onVerPeers: () -> Unit,
    onVerPerfil: () -> Unit,
    onVerNotificaciones: () -> Unit,
    onVerMisAsesorias: () -> Unit,
    onIniciarSesionEstudio: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var materias by remember { mutableStateOf<List<MateriaUI>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var notificacionesNoLeidas by remember { mutableStateOf(0) }
    var avatarId by remember { mutableStateOf(PerfilStorage.obtenerAvatar()) }
    var materiaExamenPendiente by remember { mutableStateOf<MateriaUI?>(null) }
    var mensajeBloqueo by remember { mutableStateOf<String?>(null) }
    var errorCrearMateria by remember { mutableStateOf("") }

    val notifClient = remember { HttpClient() }
    LaunchedEffect(Unit) {
        try {
            val token = SesionStorage.obtenerToken() ?: ""
            val resp = notifClient.get("${ApiConfig.BASE_URL}/notificaciones") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val lista = jsonParserDashboard.decodeFromString<List<NotificacionResumenDto>>(resp)
            notificacionesNoLeidas = lista.count { !it.leida }
        } catch (e: Exception) { /* sin notificaciones por ahora */ }
        try {
            val token = SesionStorage.obtenerToken() ?: ""
            val perfil = notifClient.get("${ApiConfig.BASE_URL}/peers/perfil") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val dto = jsonParserDashboard.decodeFromString<PerfilDashboardDto>(perfil)
            avatarId = if (dto.rol == "ASESOR") avatarAsesorPorGrado(dto.grado) else dto.avatarId
            PerfilStorage.guardarAvatar(avatarId)
        } catch (_: Exception) { /* usa el avatar local */ }
    }
    var mostrarProximamente by remember { mutableStateOf(false) }
    var materiaDetalle by remember { mutableStateOf<MateriaUI?>(null) }
    // fechas de examen por materiaId, usado para el bloqueo "EXAMEN HOY"
    var fechasPorMateria by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    // Evita confundir una falla de red con la ausencia real de un examen mensual.
    var examenesCargados by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // true si TODAS las subcarpetas de tarjetas de esa materia ya se estudiaron
    var completoPorMateria by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    suspend fun cargarMaterias() {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/materias") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val listaDto = jsonParserDashboard.decodeFromString<List<MateriaApiDto>>(resp)
            val lista = listaDto.map { MateriaUI(it.id, it.nombre, it.dificultad) }
            materias = lista

            // Carga las fechas de examen de cada materia para el bloqueo "EXAMEN HOY"
            val mapa = mutableMapOf<Int, List<String>>()
            val cargados = mutableSetOf<Int>()
            val progreso = mutableMapOf<Int, Boolean>()
            lista.forEach { m ->
                try {
                    val respEx = client.get("${ApiConfig.BASE_URL}/materias/${m.id}/examenes") {
                        header("Authorization", "Bearer $token")
                    }.bodyAsText()
                    val rFecha = Regex(""""fecha":"([^"]+)"""")
                    mapa[m.id] = rFecha.findAll(respEx).map { it.groupValues[1] }.toList()
                    cargados += m.id
                } catch (e: Exception) {
                    // No se marca como vacío: todavía no fue posible validar el calendario.
                }
                try {
                    val tarjetas = cargarTarjetasMateria(client, token, m.id)
                    val subcarpetas = agruparPorTema(tarjetas)
                    progreso[m.id] = subcarpetas.isNotEmpty() && subcarpetas.all { it.completado }
                } catch (e: Exception) {
                    progreso[m.id] = false
                }
            }
            fechasPorMateria = mapa
            examenesCargados = cargados
            completoPorMateria = progreso
            materiaExamenPendiente = lista.firstOrNull {
                it.id in cargados && !tieneExamenMesActual(mapa[it.id] ?: emptyList())
            }
        } catch (e: Exception) { /* mostrar mensaje de error */ }
        cargando = false
    }

    LaunchedEffect(Unit) { cargarMaterias() }

    if (mostrarFormulario) {
        AgregarMateriaDialog(
            errorServidor = errorCrearMateria,
            onGuardar = { nombre, dificultad, examenNombre, examenFecha ->
                scope.launch {
                    try {
                        val respuesta = client.post("${ApiConfig.BASE_URL}/materias") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                "{\"nombre\":\"${escaparJson(nombre.trim())}\"," +
                                    "\"dificultad\":$dificultad," +
                                    "\"examenNombre\":\"${escaparJson(examenNombre)}\"," +
                                    "\"examenFecha\":\"$examenFecha\"}"
                            )
                        }
                        if (respuesta.status.isSuccess()) {
                            errorCrearMateria = ""
                            cargarMaterias()
                            mostrarFormulario = false
                        } else {
                            errorCrearMateria = "No se pudo crear la materia. Revisa la evaluación seleccionada."
                        }
                    } catch (_: Exception) {
                        errorCrearMateria = "No se pudo conectar con el servidor."
                    }
                }
            },
            onCerrar = { mostrarFormulario = false }
        )
        return
    }

    if (materiaDetalle != null) {
        DetalleMateriaDialog(
            materia = materiaDetalle!!,
            token = token,
            onCerrar = {
                materiaDetalle = null
                scope.launch { cargarMaterias() }
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 130.dp)  // espacio para nav + FAB
        ) {
            // Top bar estilo mockup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger — abre el menú lateral
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { mostrarMenu = true }
                ) {
                    repeat(3) {
                        Box(
                            Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .background(VerdePrimario, RoundedCornerShape(1.dp))
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("EduFlow", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                // Notificaciones — muestra cuántas no se han leído
                Box(
                    modifier = Modifier.size(36.dp).clickable { onVerNotificaciones() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.width(15.dp).height(12.dp)
                                .background(
                                    VerdePrimario,
                                    RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                                )
                        )
                        Spacer(Modifier.height(1.dp))
                        Box(
                            modifier = Modifier.width(5.dp).height(3.dp)
                                .background(VerdePrimario, RoundedCornerShape(2.dp))
                        )
                    }
                    if (notificacionesNoLeidas > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFB00020)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (notificacionesNoLeidas > 9) "9+" else "$notificacionesNoLeidas",
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(19.dp))
                        .clickable { mostrarMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    AvatarIcono(avatarId = avatarId, sizeDp = 38)
                }
            }

            // Saludo
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    "Hola de nuevo, ${SesionStorage.obtenerNombre()}!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrimario
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (materias.isEmpty())
                        "Agrega tu primera materia para comenzar."
                    else
                        "Tienes ${materias.size} sesión(es) de estudio para hoy.",
                    fontSize = 13.sp,
                    color = TextoSecundario,
                    lineHeight = 19.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Mini-card de perfil (carrera/cuatrimestre locales del alumno)
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clickable { onVerPerfil() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarIcono(avatarId = avatarId, sizeDp = 46)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(SesionStorage.obtenerNombre(), fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                        Text(PerfilStorage.obtenerCarrera(), fontSize = 11.sp,
                            color = TextoSecundario, maxLines = 1)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFDDE8E0)) {
                        Text("Gestionar perfil", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = VerdePrimario,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Encabezado sección materias
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sesiones de hoy",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrimario
                )
                Spacer(Modifier.weight(1f))
                if (materias.isNotEmpty()) {
                    val pendientes = materias.count { !(completoPorMateria[it.id] ?: false) }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDDE8E0)
                    ) {
                        Text(
                            "$pendientes Pendiente${if (pendientes != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = VerdePrimario,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (cargando) {
                Box(Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdePrimario)
                }
            } else if (materias.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EduFlowMark(modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sin materias aún",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrimario
                        )
                        Text(
                            "Toca el botón + para agregar tu primera materia.",
                            fontSize = 13.sp,
                            color = TextoSecundario,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                materias.forEach { materia ->
                    val fechas = fechasPorMateria[materia.id] ?: emptyList()
                    val bloqueo = if (materia.id !in examenesCargados) {
                        BloqueoMateriaUI.NINGUNO
                    } else when {
                        tieneExamenHoy(fechas) -> BloqueoMateriaUI.EXAMEN_HOY
                        !tieneExamenMesActual(fechas) -> BloqueoMateriaUI.SIN_EXAMEN_MENSUAL
                        else -> BloqueoMateriaUI.NINGUNO
                    }
                    val completa = completoPorMateria[materia.id] ?: false
                    MateriaCard(
                        nombre = materia.nombre,
                        dificultad = materia.dificultad,
                        bloqueo = bloqueo,
                        completa = completa,
                        onClick = {
                            when (bloqueo) {
                                BloqueoMateriaUI.NINGUNO -> materiaDetalle = materia
                                BloqueoMateriaUI.SIN_EXAMEN_MENSUAL -> materiaExamenPendiente = materia
                                BloqueoMateriaUI.EXAMEN_HOY -> mensajeBloqueo =
                                    "Hoy es el examen de ${materia.nombre}. Las tarjetas y los audios se mantienen bloqueados durante este día."
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Card StudyCast — estilo mockup verde oscuro
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onVerStudyCast() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VerdePrimario),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            "PROXIMO STUDYCAST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Consejos, tarjetas y podcasts\ncon IA",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 26.sp
                    )
                    Text(
                        "Selecciona una materia y\nestudia con ayuda de IA.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 6.dp),
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▶", fontSize = 12.sp, color = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("StudyCast", fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f))
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Text(
                                "Abrir ahora",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(
                                    horizontal = 14.dp, vertical = 8.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Bottom navigation bar estilo mockup
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
                BottomNavItem("Inicio", true, NavIcono.INICIO)
                BottomNavItem("Tarjetas", false, NavIcono.TARJETAS, onVerStudyCast)
                BottomNavItem("Audios", false, NavIcono.AUDIOS, onVerAudios)
                BottomNavItem("Comunidad", false, NavIcono.COMUNIDAD, onVerPeers)
            }
        }

        // Accesos rápidos: sesión de estudio y agregar materia.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onIniciarSesionEstudio,
                modifier = Modifier.size(52.dp),
                containerColor = VerdePrimario,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                HourglassIcon(Modifier.size(25.dp), Color.White)
            }

            FloatingActionButton(
                onClick = {
                    errorCrearMateria = ""
                    mostrarFormulario = true
                },
                modifier = Modifier.size(52.dp),
                containerColor = Color(0xFF8B6914),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 26.sp, fontWeight = FontWeight.Light)
            }
        }
    }

    if (mostrarProximamente) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { mostrarProximamente = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EduFlowMark(modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Próximamente", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = TextoPrimario)
                    Text("Esta sección estará disponible\nen una próxima versión.",
                        fontSize = 13.sp, color = TextoSecundario,
                        modifier = Modifier.padding(top = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 19.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { mostrarProximamente = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                    ) {
                        Text("Entendido", color = Color.White,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Menú lateral
    if (mostrarMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { mostrarMenu = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .align(Alignment.CenterStart)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(24.dp)
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    // Logo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EduFlowBrandIcon(modifier = Modifier.size(40.dp), padding = 2)
                        Spacer(Modifier.width(10.dp))
                        Text("EduFlow", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = VerdePrimario)
                    }

                    Spacer(Modifier.height(32.dp))

                    // Cuenta activa
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDDE8E0)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarIcono(avatarId = avatarId, sizeDp = 36)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Cuenta activa", fontSize = 11.sp,
                                    color = TextoSecundario)
                                Text(SesionStorage.obtenerNombre(), fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Mi perfil (carrera, cuatrimestre, materias destacadas)
                    OutlinedButton(
                        onClick = {
                            mostrarMenu = false
                            onVerPerfil()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdePrimario),
                        border = BorderStroke(1.dp, VerdePrimario)
                    ) {
                        Text("Mi perfil", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Mis Asesorías (con asesores agendadas)
                    OutlinedButton(
                        onClick = {
                            mostrarMenu = false
                            onVerMisAsesorias()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdePrimario),
                        border = BorderStroke(1.dp, VerdePrimario)
                    ) {
                        Text("Mis Asesorías", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Cerrar sesión
                    OutlinedButton(
                        onClick = {
                            mostrarMenu = false
                            onCerrarSesion()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdePrimario),
                        border = BorderStroke(1.dp, VerdePrimario)
                    ) {
                        Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    materiaExamenPendiente?.let { materia ->
        ExamenMensualObligatorioDialog(
            materia = materia,
            token = token,
            onGuardado = {
                materiaExamenPendiente = null
                scope.launch { cargarMaterias() }
            }
        )
    }
    mensajeBloqueo?.let { mensaje ->
        AvisoBloqueoDialog(mensaje = mensaje, onCerrar = { mensajeBloqueo = null })
    }

}

// Funcion auxiliar: compara la fecha actual del dispositivo contra las fechas de examen
fun tieneExamenHoy(fechas: List<String>): Boolean {
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
    return fechas.any { it == hoy }
}

@Composable
fun DetalleMateriaDialog(materia: MateriaUI, token: String, onCerrar: () -> Unit) {
    val scope = rememberCoroutineScope()
    val client = remember { HttpClient() }
    var examenes by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var tipoExamen by remember { mutableStateOf(tiposExamen.first()) }
    var fechaExamen by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    suspend fun cargarExamenes() {
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/materias/${materia.id}/examenes") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val rNom = Regex(""""nombre":"([^"]+)"""")
            val rFecha = Regex(""""fecha":"([^"]+)"""")
            val noms = rNom.findAll(resp).map { it.groupValues[1] }.toList()
            val fechas = rFecha.findAll(resp).map { it.groupValues[1] }.toList()
            examenes = noms.zip(fechas)
        } catch (_: Exception) { errorMsg = "No se pudieron consultar los exámenes." }
    }

    LaunchedEffect(materia.id) { cargarExamenes() }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MateriaIlustracion(materia.nombre, compacta = true, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(materia.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Dificultad ${materia.dificultad}/10", fontSize = 12.sp, color = TextoSecundario)
                    }
                    TextButton(onClick = onCerrar) { Text("Cerrar", color = VerdePrimario) }
                }

                Spacer(Modifier.height(18.dp))
                Text("Evaluaciones registradas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                Spacer(Modifier.height(8.dp))
                if (examenes.isEmpty()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF3E0)) {
                        Text(
                            "Aún no hay evaluaciones. Debes registrar al menos una por mes para usar tarjetas y audios.",
                            fontSize = 12.sp, color = Color(0xFF73520D), lineHeight = 18.sp,
                            modifier = Modifier.padding(13.dp)
                        )
                    }
                } else {
                    examenes.forEach { (nombre, fecha) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = Color(0xFFF4F6F1),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(VerdePrimario))
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(nombre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                                    Text(etiquetaFecha(fecha), fontSize = 11.sp, color = TextoSecundario)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("Agregar otra evaluación", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                Text(
                    "Selecciona opciones controladas para mantener fechas y recordatorios consistentes.",
                    fontSize = 11.sp, color = TextoSecundario, modifier = Modifier.padding(top = 3.dp, bottom = 13.dp)
                )
                SelectorTipoExamen(tipoExamen) { tipoExamen = it; errorMsg = "" }
                Spacer(Modifier.height(12.dp))
                SelectorFechaProxima(fechaExamen, soloMesActual = false, maxDias = 120) {
                    fechaExamen = it; errorMsg = ""
                }

                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (fechaExamen.isBlank()) {
                            errorMsg = "Selecciona la fecha del examen."
                        } else {
                            guardando = true
                            scope.launch {
                                try {
                                    val respuesta = client.post("${ApiConfig.BASE_URL}/materias/${materia.id}/examenes") {
                                        header("Authorization", "Bearer $token")
                                        contentType(ContentType.Application.Json)
                                        setBody("{\"nombre\":\"${escaparJson(tipoExamen)}\",\"fecha\":\"$fechaExamen\"}")
                                    }
                                    if (respuesta.status.isSuccess()) {
                                        fechaExamen = ""
                                        errorMsg = ""
                                        cargarExamenes()
                                    } else errorMsg = "No se pudo guardar la evaluación."
                                } catch (_: Exception) { errorMsg = "No se pudo conectar con el servidor." }
                                guardando = false
                            }
                        }
                    },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Guardar evaluación", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ExamenMensualObligatorioDialog(
    materia: MateriaUI,
    token: String,
    onGuardado: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val client = remember { HttpClient() }
    var tipo by remember(materia.id) { mutableStateOf(tiposExamen.first()) }
    var fecha by remember(materia.id) { mutableStateOf("") }
    var error by remember(materia.id) { mutableStateOf("") }
    var guardando by remember(materia.id) { mutableStateOf(false) }

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EduFlowMark(Modifier.size(52.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Fecha mensual requerida", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text(materia.nombre, fontSize = 12.sp, color = VerdePrimario, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    "EduFlow necesita una evaluación del mes para calcular cuándo enviarte recordatorios. Hasta registrarla, las tarjetas y los audios de esta materia permanecerán bloqueados.",
                    fontSize = 12.sp, color = TextoSecundario, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 18.dp)
                )
                SelectorTipoExamen(tipo) { tipo = it; error = "" }
                Spacer(Modifier.height(12.dp))
                SelectorFechaProxima(fecha, soloMesActual = true, maxDias = 31) { fecha = it; error = "" }
                if (error.isNotBlank()) Text(error, color = Color(0xFFB00020), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (fecha.isBlank()) error = "Selecciona una fecha dentro del mes actual."
                        else {
                            guardando = true
                            scope.launch {
                                try {
                                    val respuesta = client.post("${ApiConfig.BASE_URL}/materias/${materia.id}/examenes") {
                                        header("Authorization", "Bearer $token")
                                        contentType(ContentType.Application.Json)
                                        setBody("{\"nombre\":\"${escaparJson(tipo)}\",\"fecha\":\"$fecha\"}")
                                    }
                                    if (respuesta.status.isSuccess()) onGuardado()
                                    else error = "No se pudo guardar. Revisa la fecha seleccionada."
                                } catch (_: Exception) { error = "No se pudo conectar con Railway." }
                                guardando = false
                            }
                        }
                    },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Registrar fecha del mes", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AvisoBloqueoDialog(mensaje: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Materiales no disponibles", fontWeight = FontWeight.Bold) },
        text = { Text(mensaje, lineHeight = 19.sp) },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido", color = VerdePrimario) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun MateriaCard(
    nombre: String,
    dificultad: Int,
    bloqueo: BloqueoMateriaUI = BloqueoMateriaUI.NINGUNO,
    completa: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bloqueada = bloqueo != BloqueoMateriaUI.NINGUNO
    val colorBarra = when {
        dificultad >= 8 -> VerdePrimario
        dificultad >= 5 -> Color(0xFF8B6914)
        else            -> Color(0xFF4A9060)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bloqueada) Color(0xFFF0F0F0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MateriaIlustracion(
                    nombre = nombre,
                    compacta = true,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when (bloqueo) {
                            BloqueoMateriaUI.EXAMEN_HOY -> "Materiales bloqueados por examen"
                            BloqueoMateriaUI.SIN_EXAMEN_MENSUAL -> "Falta registrar el examen del mes"
                            BloqueoMateriaUI.NINGUNO -> if (completa) "Tarjetas estudiadas" else "Sesión de estudio pendiente"
                        },
                        fontSize = 11.sp,
                        color = if (completa && bloqueo == BloqueoMateriaUI.NINGUNO) VerdePrimario else TextoSecundario
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dificultad", fontSize = 11.sp, color = TextoSecundario)
                        Spacer(Modifier.weight(1f))
                        Text("$dificultad/10", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = colorBarra)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { dificultad / 10f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colorBarra,
                        trackColor = Color(0xFFEEEEEE)
                    )
                }
            }
            if (bloqueada) {
                Row(modifier = Modifier.padding(start = 14.dp, bottom = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (bloqueo == BloqueoMateriaUI.EXAMEN_HOY) Color(0xFF8B6914) else VerdePrimario
                    ) {
                        Text(
                            if (bloqueo == BloqueoMateriaUI.EXAMEN_HOY) "EXAMEN HOY" else "REGISTRA EL EXAMEN DEL MES",
                            fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgregarMateriaDialog(
    errorServidor: String = "",
    onGuardar: (String, Int, String, String) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var dificultad by remember { mutableIntStateOf(5) }
    var tipoExamen by remember { mutableStateOf(tiposExamen.first()) }
    var fechaExamen by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(22.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EduFlowMark(Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Nueva materia", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Organiza desde ahora su preparación mensual", fontSize = 12.sp, color = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Nombre de la materia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(100); errorMsg = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    placeholder = { Text("Ej. Arquitectura de software", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdePrimario,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    )
                )

                Spacer(Modifier.height(12.dp))
                MateriaIlustracion(
                    nombre = nombre,
                    modifier = Modifier.fillMaxWidth().height(96.dp)
                )

                Spacer(Modifier.height(16.dp))
                Text("Dificultad de la materia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                Text("Selecciona un nivel; se utilizará para programar los recordatorios.", fontSize = 11.sp, color = TextoSecundario, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                SelectorDificultad(dificultad) { dificultad = it; errorMsg = "" }

                HorizontalDivider(Modifier.padding(vertical = 17.dp))
                Text("Primera evaluación del mes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                Text("Es obligatoria para habilitar tarjetas y audios.", fontSize = 11.sp, color = TextoSecundario, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                SelectorTipoExamen(tipoExamen) { tipoExamen = it; errorMsg = "" }
                Spacer(Modifier.height(12.dp))
                SelectorFechaProxima(fechaExamen, soloMesActual = true, maxDias = 31) {
                    fechaExamen = it; errorMsg = ""
                }

                val mensajeError = errorMsg.ifBlank { errorServidor }
                if (mensajeError.isNotBlank()) Text(mensajeError, color = Color(0xFFB00020), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCerrar,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, VerdePrimario)
                    ) { Text("Cancelar", color = VerdePrimario, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = {
                            when {
                                nombre.isBlank() -> errorMsg = "Escribe el nombre de la materia."
                                fechaExamen.isBlank() -> errorMsg = "Selecciona la fecha de examen del mes."
                                else -> onGuardar(nombre.trim(), dificultad, tipoExamen, fechaExamen)
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                    ) { Text("Crear materia", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
