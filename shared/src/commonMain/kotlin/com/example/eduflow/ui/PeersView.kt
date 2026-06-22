package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Serializable
private data class AutorApiDto(
    val id: Int, val nombre: String, val carrera: String = "",
    val cuatrimestre: Int = 1, val rol: String = "ALUMNO"
)

@Serializable
private data class SolicitudApiDto(
    val id: Int, val titulo: String, val descripcion: String, val materia: String,
    val estado: String, val tieneImagen: Boolean, val createdAt: String, val autor: AutorApiDto
)

@Serializable
private data class MentorApiDto(
    val id: Int, val nombre: String, val carrera: String, val cuatrimestre: Int,
    val materiasDestaca: String, val rol: String, val avatarId: String,
    val promedio: Double, val totalCalif: Int, val permiteAsesoria: Boolean
)

private val jsonParser = Json { ignoreUnknownKeys = true }

// Vista de Red de Apoyo, ahora conectada al backend real (EP8): mentores
// destacados, solicitudes recientes y crear/responder, en vez del mockup
// con datos quemados de antes.
@Composable
fun PeersView(
    onVolver: () -> Unit,
    onVerStudyCast: () -> Unit,
    onVerAudios: () -> Unit,
    onVerPerfil: () -> Unit
) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var filtroActivo by remember { mutableStateOf("Todas") }
    val filtros = listOf("Todas", "Cálculo", "Programación", "Física")

    var mentores    by remember { mutableStateOf<List<MentorApiDto>>(emptyList()) }
    var solicitudes by remember { mutableStateOf<List<SolicitudApiDto>>(emptyList()) }
    var cargando    by remember { mutableStateOf(true) }
    var mostrarNueva by remember { mutableStateOf(false) }
    var generando    by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf("") }

    suspend fun cargar() {
        cargando = true
        try {
            val materiaFiltro = if (filtroActivo == "Todas") "" else "?materia=$filtroActivo"
            val respMentores = client.get("${ApiConfig.BASE_URL}/peers/mentores/destacados$materiaFiltro")
                .bodyAsText()
            mentores = jsonParser.decodeFromString(respMentores)

            val respSolicitudes = client.get("${ApiConfig.BASE_URL}/peers/solicitudes$materiaFiltro") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            solicitudes = jsonParser.decodeFromString(respSolicitudes)
        } catch (e: Exception) { errorMsg = "No se pudo cargar la Red de Apoyo." }
        cargando = false
    }

    LaunchedEffect(filtroActivo) { cargar() }

    if (mostrarNueva) {
        NuevaSolicitudDialog(
            generando = generando,
            errorMsg = errorMsg,
            onCerrar = { mostrarNueva = false; errorMsg = "" },
            onGuardar = { titulo, descripcion, materia ->
                generando = true; errorMsg = ""
                scope.launch {
                    try {
                        client.post("${ApiConfig.BASE_URL}/peers/solicitudes") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                "{\"titulo\":\"${escaparJson(titulo)}\"," +
                                "\"descripcion\":\"${escaparJson(descripcion)}\"," +
                                "\"materia\":\"${escaparJson(materia)}\"}"
                            )
                        }
                        mostrarNueva = false
                        cargar()
                    } catch (e: Exception) { errorMsg = "Error al publicar. Revisa tu conexión." }
                    generando = false
                }
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
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("EduFlow", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onVerPerfil() },
                    contentAlignment = Alignment.Center
                ) {
                    AvatarIcono(avatarId = "avatar_1", sizeDp = 36)
                }
            }

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdePrimario)
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 90.dp)
            ) {

                // Card verde de encabezado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VerdePrimario)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tu Red de Apoyo Académico", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White,
                            lineHeight = 26.sp)
                        Text(
                            "Conecta con mentores expertos en áreas técnicas y resuelve " +
                                "tus dudas en un entorno colaborativo y humano.",
                            fontSize = 13.sp, color = Color.White.copy(0.85f),
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { mostrarNueva = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White
                        ) {
                            Text("Publicar Solicitud", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = VerdePrimario,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text("Mentores Destacados", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario,
                    modifier = Modifier.padding(bottom = 12.dp))

                if (mentores.isEmpty()) {
                    Text("Aún no hay mentores calificados en esta materia.",
                        fontSize = 12.sp, color = TextoSecundario,
                        modifier = Modifier.padding(bottom = 8.dp))
                } else {
                    mentores.take(1).forEach { mentor ->
                        MentorDestacadoCard(mentor)
                    }
                    Spacer(Modifier.height(10.dp))
                    mentores.drop(1).take(3).forEach { mentor ->
                        MentorCompactoCard(mentor)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Filtro por materia
                Text("Filtrar por materia", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = TextoSecundario,
                    modifier = Modifier.padding(bottom = 10.dp))

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    filtros.forEach { filtro ->
                        Surface(
                            modifier = Modifier.padding(end = 8.dp)
                                .clickable { filtroActivo = filtro },
                            shape = RoundedCornerShape(20.dp),
                            color = if (filtroActivo == filtro) VerdePrimario
                                    else Color(0xFFEEEEEE)
                        ) {
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

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp))
                }

                if (solicitudes.isEmpty()) {
                    Text("No hay solicitudes abiertas todavía.",
                        fontSize = 12.sp, color = TextoSecundario)
                } else {
                    solicitudes.forEach { sol ->
                        SolicitudCard(sol)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEAE0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("¿Necesitas ayuda tú también?", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Publica tu duda y recibe apoyo de la comunidad.",
                            fontSize = 12.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { mostrarNueva = true },
                            shape = RoundedCornerShape(10.dp),
                            color = VerdePrimario
                        ) {
                            Text("Crear Solicitud", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        }
                    }
                }
            }
        }

        // Bottom nav
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = Beige,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem(label = "Dashboard", selected = false,
                    symbol = "⊞", onClick = onVolver)
                BottomNavItem(label = "StudyCast", selected = false,
                    symbol = "▶", onClick = onVerStudyCast)
                BottomNavItem(label = "Audio", selected = false,
                    symbol = "♪", onClick = onVerAudios)
                BottomNavItem(label = "Peers", selected = true, symbol = "⊙")
            }
        }
    }
}

@Composable
private fun MentorDestacadoCard(mentor: MentorApiDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
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
                    Text(mentor.nombre, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = TextoPrimario,
                        modifier = Modifier.padding(top = 6.dp))
                }
                if (mentor.totalCalif > 0) {
                    Text("★ ${(mentor.promedio * 10).toInt() / 10.0}", fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF8B6914))
                }
            }
            Text(
                "${mentor.carrera} · ${mentor.cuatrimestre}° cuatrimestre",
                fontSize = 12.sp, color = TextoSecundario, lineHeight = 18.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)
            )
            if (mentor.permiteAsesoria) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = VerdePrimario
                ) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarIcono(avatarId = mentor.avatarId, sizeDp = 40)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mentor.materiasDestaca.split(",").firstOrNull()?.trim() ?: "",
                    fontSize = 11.sp, color = VerdePrimario, fontWeight = FontWeight.SemiBold)
                Text(mentor.nombre, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = TextoPrimario)
            }
            if (mentor.totalCalif > 0) {
                Text("★ ${(mentor.promedio * 10).toInt() / 10.0}", fontSize = 11.sp,
                    color = Color(0xFF8B6914), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SolicitudCard(sol: SolicitudApiDto) {
    val icono = when {
        sol.materia.contains("mat", true) || sol.materia.contains("calc", true) -> "Σ"
        sol.materia.contains("prog", true) || sol.materia.contains("cod", true) -> "<>"
        sol.materia.contains("fis", true) -> "λ"
        else -> "◈"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDDE8E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icono, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = VerdePrimario)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(sol.titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = TextoPrimario, lineHeight = 18.sp)
                }
            }
            Text(sol.descripcion, fontSize = 12.sp, color = TextoSecundario,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 42.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sol.autor.nombre, fontSize = 11.sp, color = TextoSecundario,
                    fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(8.dp), color = VerdePrimario) {
                    Text("Ayudar ahora", fontSize = 11.sp, color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun NuevaSolicitudDialog(
    generando: Boolean,
    errorMsg: String,
    onCerrar: () -> Unit,
    onGuardar: (titulo: String, descripcion: String, materia: String) -> Unit
) {
    var titulo      by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var materia      by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(Beige)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCerrar, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("Nueva Solicitud", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
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

                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp))
                        }

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (titulo.isBlank() || descripcion.isBlank()) return@Button
                                onGuardar(titulo.trim(), descripcion.trim(), materia.trim())
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !generando,
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
