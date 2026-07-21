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
import com.example.eduflow.notifications.NotificationScheduler
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AutorAsesoriaDto(val id: Int, val nombre: String, val carrera: String = "", val cuatrimestre: Int = 1, val rol: String = "")

@Serializable
private data class AsesoriaApiDto(
    val id: Int,
    val asesor: AutorAsesoriaDto,
    val alumno: AutorAsesoriaDto,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: String,
    val mensajeAsesor: String = "",
    val enlace: String = "",
    val ubicacion: String = "",
    val calificacion: Int? = null,
    val createdAt: String = ""
)

private val jsonMisAsesorias = Json { ignoreUnknownKeys = true }

private fun colorEstado(estado: String): Color = when (estado) {
    "ACEPTADA" -> Color(0xFF2D5A3D)
    "CANCELADA" -> Color(0xFFB00020)
    else -> Color(0xFF8B6914) // PENDIENTE
}

private fun etiquetaEstado(estado: String): String = when (estado) {
    "ACEPTADA" -> "Aceptada"
    "CANCELADA" -> "Cancelada"
    else -> "Pendiente"
}

// Lista de asesorías del usuario logueado. Si es Alumno ve a quién le pidió
// asesoría y el estado; si es Asesor ve las solicitudes y puede aceptar o
// cancelar las que están pendientes.
@Composable
fun MisAsesoriasView(onVolver: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""
    val rol    = SesionStorage.obtenerRol()

    var asesorias by remember { mutableStateOf<List<AsesoriaApiDto>>(emptyList()) }
    var cargando  by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf("") }
    var procesandoId by remember { mutableStateOf<Int?>(null) }
    var asesoriaParaAceptar by remember { mutableStateOf<AsesoriaApiDto?>(null) }
    var asesoriaParaCancelar by remember { mutableStateOf<AsesoriaApiDto?>(null) }
    var asesoriaParaCalificar by remember { mutableStateOf<AsesoriaApiDto?>(null) }

    suspend fun cargar() {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/asesorias/mis-asesorias") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            asesorias = jsonMisAsesorias.decodeFromString(resp)
        } catch (e: Exception) { errorMsg = "No se pudieron cargar tus asesorías." }
        cargando = false
    }

    LaunchedEffect(Unit) { cargar() }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                BotonVolver(onClick = onVolver)
                Spacer(Modifier.weight(1f))
                Text("Mis Asesorías", fontSize = 16.sp,
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

                if (asesorias.isEmpty()) {
                    Text(
                        if (rol == "ASESOR") "Aún no tienes solicitudes de asesoría."
                        else "Aún no has agendado ninguna asesoría.",
                        fontSize = 13.sp, color = TextoSecundario
                    )
                } else {
                    asesorias.forEach { a ->
                        val otraPersona = if (rol == "ASESOR") a.alumno else a.asesor
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(otraPersona.nombre, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold, color = TextoPrimario,
                                        modifier = Modifier.weight(1f))
                                    Surface(shape = RoundedCornerShape(8.dp),
                                        color = colorEstado(a.estado).copy(alpha = 0.12f)) {
                                        Text(etiquetaEstado(a.estado), fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold, color = colorEstado(a.estado),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                Text("${a.fecha} · ${a.horaInicio} – ${a.horaFin}",
                                    fontSize = 12.sp, color = TextoSecundario,
                                    modifier = Modifier.padding(top = 6.dp))

                                if (a.estado == "ACEPTADA") {
                                    if (a.ubicacion.isNotBlank())
                                        Text("Ubicación: ${a.ubicacion}", fontSize = 12.sp,
                                            color = TextoSecundario, modifier = Modifier.padding(top = 4.dp))
                                    if (a.enlace.isNotBlank())
                                        Text("Enlace: ${a.enlace}", fontSize = 12.sp,
                                            color = VerdePrimario, modifier = Modifier.padding(top = 4.dp))
                                    if (a.mensajeAsesor.isNotBlank())
                                        Text(a.mensajeAsesor, fontSize = 12.sp,
                                            color = TextoSecundario, modifier = Modifier.padding(top = 4.dp))
                                }

                                if (rol != "ASESOR" && a.estado == "ACEPTADA") {
                                    Spacer(Modifier.height(10.dp))
                                    if (a.calificacion != null) {
                                        Text(
                                            "Tu calificación: ${"★".repeat(a.calificacion)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8B6914)
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { asesoriaParaCalificar = a },
                                            modifier = Modifier.fillMaxWidth().height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF8B6914))
                                        ) {
                                            Text("Calificar asesoría", color = Color(0xFF8B6914), fontSize = 12.sp)
                                        }
                                    }
                                }

                                if (rol == "ASESOR" && a.estado == "PENDIENTE") {
                                    Spacer(Modifier.height(10.dp))
                                    Row {
                                        Button(
                                            onClick = { asesoriaParaAceptar = a },
                                            enabled = procesandoId == null,
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                                        ) { Text("Aceptar", color = Color.White, fontSize = 12.sp) }
                                        Spacer(Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { asesoriaParaCancelar = a },
                                            enabled = procesandoId == null,
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) { Text("Cancelar", color = Color(0xFFB00020), fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo: aceptar con mensaje/enlace/ubicación opcional
    val asesoriaAceptar = asesoriaParaAceptar
    if (asesoriaAceptar != null) {
        var mensaje    by remember(asesoriaAceptar.id) { mutableStateOf("") }
        var enlace     by remember(asesoriaAceptar.id) { mutableStateOf("") }
        var ubicacion  by remember(asesoriaAceptar.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { asesoriaParaAceptar = null },
            title = { Text("Aceptar asesoría", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Indícale a ${asesoriaAceptar.alumno.nombre} dónde o cómo será la asesoría.",
                        fontSize = 12.sp, color = TextoSecundario, modifier = Modifier.padding(bottom = 10.dp))
                    OutlinedTextField(value = ubicacion, onValueChange = { ubicacion = it },
                        label = { Text("Ubicación (ej: Salón B-201)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true)
                    OutlinedTextField(value = enlace, onValueChange = { enlace = it },
                        label = { Text("Enlace (ej: Google Meet)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true)
                    OutlinedTextField(value = mensaje, onValueChange = { mensaje = it },
                        label = { Text("Mensaje adicional", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = asesoriaAceptar.id
                    procesandoId = id
                    asesoriaParaAceptar = null
                    scope.launch {
                        try {
                            client.put("${ApiConfig.BASE_URL}/asesorias/$id/aceptar") {
                                header("Authorization", "Bearer $token")
                                contentType(ContentType.Application.Json)
                                setBody(
                                    "{\"mensajeAsesor\":\"${escaparJson(mensaje)}\"," +
                                    "\"enlace\":\"${escaparJson(enlace)}\"," +
                                    "\"ubicacion\":\"${escaparJson(ubicacion)}\"}"
                                )
                            }
                            cargar()
                        } catch (e: Exception) { errorMsg = "No se pudo aceptar la asesoría." }
                        procesandoId = null
                    }
                }) { Text("Confirmar", color = VerdePrimario, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { asesoriaParaAceptar = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo: calificar asesoría finalizada/aceptada.
    val asesoriaCalificar = asesoriaParaCalificar
    if (asesoriaCalificar != null) {
        var estrellas by remember(asesoriaCalificar.id) { mutableStateOf(0) }
        var comentario by remember(asesoriaCalificar.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { asesoriaParaCalificar = null },
            title = { Text("Calificar asesoría", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¿Cómo fue tu asesoría con ${asesoriaCalificar.asesor.nombre}?",
                        fontSize = 13.sp, color = TextoSecundario)
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { n ->
                            Text(
                                if (n <= estrellas) "★" else "☆",
                                fontSize = 32.sp,
                                color = if (n <= estrellas) Color(0xFF8B6914) else Color(0xFFCCCCCC),
                                modifier = Modifier.clickable { estrellas = n }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = comentario,
                        onValueChange = { comentario = it.take(300) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Comentario opcional", fontSize = 11.sp) },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = estrellas > 0 && procesandoId == null,
                    onClick = {
                        val id = asesoriaCalificar.id
                        val estrellasEnviar = estrellas
                        val comentarioEnviar = comentario
                        procesandoId = id
                        asesoriaParaCalificar = null
                        scope.launch {
                            try {
                                client.post("${ApiConfig.BASE_URL}/asesorias/$id/calificar") {
                                    header("Authorization", "Bearer $token")
                                    contentType(ContentType.Application.Json)
                                    setBody(
                                        """{"estrellas":$estrellasEnviar,"comentario":"${escaparJson(comentarioEnviar)}"}"""
                                    )
                                }
                                NotificationScheduler.sincronizarAhora()
                                cargar()
                            } catch (e: Exception) {
                                errorMsg = "No se pudo registrar la calificación."
                            }
                            procesandoId = null
                        }
                    }
                ) { Text("Enviar", color = VerdePrimario, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { asesoriaParaCalificar = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo: confirmar cancelación
    val asesoriaCancelar = asesoriaParaCancelar
    if (asesoriaCancelar != null) {
        AlertDialog(
            onDismissRequest = { asesoriaParaCancelar = null },
            title = { Text("Cancelar asesoría", fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que quieres cancelar la asesoría con ${asesoriaCancelar.alumno.nombre}?",
                fontSize = 13.sp, color = TextoSecundario) },
            confirmButton = {
                TextButton(onClick = {
                    val id = asesoriaCancelar.id
                    procesandoId = id
                    asesoriaParaCancelar = null
                    scope.launch {
                        try {
                            client.put("${ApiConfig.BASE_URL}/asesorias/$id/cancelar") {
                                header("Authorization", "Bearer $token")
                            }
                            cargar()
                        } catch (e: Exception) { errorMsg = "No se pudo cancelar la asesoría." }
                        procesandoId = null
                    }
                }) { Text("Sí, cancelar", color = Color(0xFFB00020), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { asesoriaParaCancelar = null }) { Text("Volver") }
            }
        )
    }
}
