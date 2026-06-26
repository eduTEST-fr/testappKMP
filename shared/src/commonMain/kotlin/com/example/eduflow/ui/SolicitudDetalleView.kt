package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// DTOs locales para esta pantalla
@Serializable
private data class AutorDetalle(
    val id: Int, val nombre: String, val carrera: String = "",
    val cuatrimestre: Int = 1, val rol: String = "ALUMNO"
)

@Serializable
private data class RespuestaDetalle(
    val id: Int, val contenido: String, val imagenBase64: String = "",
    val createdAt: String = "", val autor: AutorDetalle
)

@Serializable
private data class SolicitudDetalle(
    val id: Int, val titulo: String, val descripcion: String,
    val imagenBase64: String = "", val materia: String = "",
    val estado: String, val createdAt: String = "",
    val autor: AutorDetalle, val respuestas: List<RespuestaDetalle>
)

private val jsonDet = Json { ignoreUnknownKeys = true }

// Pantalla de detalle de una solicitud.
// - ALUMNO: puede responder (si su cuatrimestre >= al del autor) y cerrar si es su solicitud.
// - ASESOR: puede responder siempre y cerrar cualquier solicitud.
// - ADMIN:  puede responder, cerrar y eliminar solicitudes y respuestas individuales.
@Composable
fun SolicitudDetalleView(solicitudId: Int, onVolver: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""
    val rol    = SesionStorage.obtenerRol()

    var detalle   by remember { mutableStateOf<SolicitudDetalle?>(null) }
    var cargando  by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf("") }
    var respuesta by remember { mutableStateOf("") }
    var enviando  by remember { mutableStateOf(false) }
    var confirmEliminarSol  by remember { mutableStateOf(false) }
    var confirmEliminarResp by remember { mutableStateOf<Int?>(null) }

    // Extraemos el userId del perfil para saber si la solicitud es propia
    var miUserId by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            val me = client.get("${ApiConfig.BASE_URL}/auth/me") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            miUserId = Regex(""""id":(\d+)""").find(me)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        } catch (e: Exception) { /* ignorar, userId queda 0 */ }
    }

    suspend fun cargar() {
        cargando = true
        errorMsg = ""
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/peers/solicitudes/$solicitudId") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            detalle = jsonDet.decodeFromString(resp)
        } catch (e: Exception) { errorMsg = "No se pudo cargar la solicitud." }
        cargando = false
    }

    LaunchedEffect(solicitudId) { cargar() }

    // ── Confirmación eliminar solicitud ──
    if (confirmEliminarSol) {
        AlertDialog(
            onDismissRequest = { confirmEliminarSol = false },
            title = { Text("Eliminar solicitud", fontWeight = FontWeight.Bold) },
            text  = { Text("Esta acción es permanente. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            client.delete("${ApiConfig.BASE_URL}/admin/peers/solicitudes/$solicitudId") {
                                header("Authorization", "Bearer $token")
                            }
                            onVolver()
                        } catch (e: Exception) { errorMsg = "Error al eliminar." }
                        confirmEliminarSol = false
                    }
                }) { Text("Eliminar", color = Color(0xFFB00020), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEliminarSol = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Confirmación eliminar respuesta ──
    confirmEliminarResp?.let { respId ->
        AlertDialog(
            onDismissRequest = { confirmEliminarResp = null },
            title = { Text("Eliminar respuesta", fontWeight = FontWeight.Bold) },
            text  = { Text("¿Eliminar esta respuesta de forma permanente?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            client.delete("${ApiConfig.BASE_URL}/admin/peers/respuestas/$respId") {
                                header("Authorization", "Bearer $token")
                            }
                            cargar()
                        } catch (e: Exception) { errorMsg = "Error al eliminar respuesta." }
                        confirmEliminarResp = null
                    }
                }) { Text("Eliminar", color = Color(0xFFB00020), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEliminarResp = null }) { Text("Cancelar") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("Detalle de Solicitud", fontSize = 15.sp,
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

            if (errorMsg.isNotEmpty() && detalle == null) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 14.sp)
                }
                return@Column
            }

            detalle?.let { sol ->
                val esSolicitante = miUserId == sol.autor.id
                val puedeResponder = sol.estado == "ABIERTA" && rol != "ALUMNO" || (
                    rol == "ALUMNO" /* cuatrimestre check se hace en backend */ )
                val puedeCerrar = sol.estado == "ABIERTA" &&
                    (rol == "ADMIN" || rol == "ASESOR" || esSolicitante)
                val puedeEliminar = rol == "ADMIN"

                Column(modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp)) {

                    // Cabecera de la solicitud
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(3.dp)) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sol.titulo, fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold, color = TextoPrimario,
                                        lineHeight = 22.sp)
                                    if (sol.materia.isNotBlank()) {
                                        Text(sol.materia, fontSize = 12.sp, color = VerdePrimario,
                                            modifier = Modifier.padding(top = 3.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = if (sol.estado == "ABIERTA") Color(0xFFDDE8E0)
                                            else Color(0xFFEEEEEE)) {
                                    Text(sol.estado, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = if (sol.estado == "ABIERTA") VerdePrimario
                                                else TextoSecundario,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(sol.descripcion, fontSize = 13.sp, color = TextoSecundario,
                                lineHeight = 19.sp)
                            Spacer(Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarIcono(avatarId = "avatar_1", sizeDp = 28)
                                Spacer(Modifier.width(8.dp))
                                Text(sol.autor.nombre, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                                Text(" · ${sol.autor.cuatrimestre}° cuatrimestre",
                                    fontSize = 11.sp, color = TextoSecundario)
                            }

                            // Botones de gestión (cerrar / eliminar solicitud)
                            if (puedeCerrar || puedeEliminar) {
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (puedeCerrar) {
                                        val urlCerrar = if (rol == "ADMIN")
                                            "${ApiConfig.BASE_URL}/admin/peers/solicitudes/$solicitudId/cerrar"
                                        else
                                            "${ApiConfig.BASE_URL}/peers/solicitudes/$solicitudId/cerrar"
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        client.post(urlCerrar) {
                                                            header("Authorization", "Bearer $token")
                                                        }
                                                        cargar()
                                                    } catch (e: Exception) { errorMsg = "Error al cerrar." }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, VerdePrimario)
                                        ) {
                                            Text("Cerrar solicitud", fontSize = 12.sp,
                                                color = VerdePrimario, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (puedeEliminar) {
                                        OutlinedButton(
                                            onClick = { confirmEliminarSol = true },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFFB00020))
                                        ) {
                                            Text("Eliminar", fontSize = 12.sp,
                                                color = Color(0xFFB00020), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("Respuestas (${sol.respuestas.size})", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = TextoPrimario,
                        modifier = Modifier.padding(bottom = 10.dp))

                    if (sol.respuestas.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center) {
                                Text("Sé el primero en responder.", fontSize = 13.sp,
                                    color = TextoSecundario)
                            }
                        }
                    } else {
                        sol.respuestas.forEach { resp ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarIcono(avatarId = "avatar_1", sizeDp = 28)
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(resp.autor.nombre, fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                                            val rolLabel = when (resp.autor.rol) {
                                                "ASESOR" -> " · Asesor"
                                                "ADMIN"  -> " · Admin"
                                                else     -> " · ${resp.autor.cuatrimestre}° cuatrimestre"
                                            }
                                            Text(rolLabel, fontSize = 11.sp, color = TextoSecundario)
                                        }
                                        // Admin puede eliminar cualquier respuesta
                                        if (puedeEliminar) {
                                            TextButton(
                                                onClick = { confirmEliminarResp = resp.id },
                                                contentPadding = PaddingValues(4.dp)
                                            ) {
                                                Text("✕", fontSize = 14.sp, color = Color(0xFFB00020))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(resp.contenido, fontSize = 13.sp,
                                        color = TextoSecundario, lineHeight = 19.sp)
                                }
                            }
                        }
                    }

                    // Caja para responder — visible si la solicitud está abierta
                    if (sol.estado == "ABIERTA") {
                        Spacer(Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Tu respuesta", fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = TextoPrimario,
                                    modifier = Modifier.padding(bottom = 8.dp))
                                OutlinedTextField(
                                    value = respuesta,
                                    onValueChange = { respuesta = it },
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    placeholder = { Text("Escribe tu respuesta aquí...",
                                        color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VerdePrimario,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )

                                if (errorMsg.isNotEmpty()) {
                                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 6.dp))
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (respuesta.isBlank()) return@Button
                                        enviando = true; errorMsg = ""
                                        scope.launch {
                                            try {
                                                val result = client.post(
                                                    "${ApiConfig.BASE_URL}/peers/solicitudes/$solicitudId/responder"
                                                ) {
                                                    header("Authorization", "Bearer $token")
                                                    contentType(ContentType.Application.Json)
                                                    setBody("{\"contenido\":\"${escaparJson(respuesta)}\"}")
                                                }
                                                if (result.status == HttpStatusCode.Forbidden) {
                                                    errorMsg = "Solo puedes responder solicitudes de tu cuatrimestre o inferior."
                                                } else {
                                                    respuesta = ""
                                                    cargar()
                                                }
                                            } catch (e: Exception) { errorMsg = "Error al enviar. Revisa tu conexión." }
                                            enviando = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !enviando && respuesta.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                                ) {
                                    if (enviando)
                                        CircularProgressIndicator(color = Color.White,
                                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else
                                        Text("Publicar respuesta", color = Color.White,
                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
