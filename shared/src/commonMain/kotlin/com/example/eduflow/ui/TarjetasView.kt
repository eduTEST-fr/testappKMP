package com.example.eduflow.ui

import androidx.compose.animation.AnimatedVisibility
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
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Tarjeta(val id: Int, val pregunta: String, val respuesta: String)

// DTOs que reflejan exactamente lo que devuelve el backend (TarjetaDto y
// GenerarTarjetasResponse). Antes esto se leia con regex y se rompia si el
// JSON traia saltos de linea o comillas internas; ahora se parsea de verdad.
@Serializable
private data class TarjetaApiDto(val id: Int, val pregunta: String, val respuesta: String)
@Serializable
private data class GenerarTarjetasApiResponse(val tarjetas: List<TarjetaApiDto>, val total: Int)

private val jsonParser = Json { ignoreUnknownKeys = true }

@Composable
fun TarjetasView(materia: MateriaUI?, token: String) {
    val scope   = rememberCoroutineScope()
    val client  = remember { HttpClient() }
    var tarjetas  by remember { mutableStateOf<List<Tarjeta>>(emptyList()) }
    var cargando  by remember { mutableStateOf(false) }
    var generando by remember { mutableStateOf(false) }
    var tema      by remember { mutableStateOf("") }
    var errorMsg  by remember { mutableStateOf("") }

    // Cargar tarjetas existentes al abrir
    LaunchedEffect(materia?.id) {
        if (materia == null) return@LaunchedEffect
        cargando = true
        try {
            val resp = client.get(
                "${ApiConfig.BASE_URL}/tarjetas/${materia.id}"
            ) {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val lista = jsonParser.decodeFromString<List<TarjetaApiDto>>(resp)
            tarjetas = lista.map { Tarjeta(it.id, it.pregunta, it.respuesta) }
        } catch (e: Exception) {}
        cargando = false
    }

    if (materia == null) {
        Text("Selecciona una materia primero",
            fontSize = 14.sp, color = TextoSecundario,
            modifier = Modifier.padding(20.dp))
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (cargando) {
            CircularProgressIndicator(color = VerdePrimario,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
        } else if (tarjetas.isEmpty()) {
            // No hay tarjetas: mostrar formulario de generacion
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Generar tarjetas de estudio",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = TextoPrimario)
                    Text("Describe el tema o pega el contenido de tus apuntes",
                        fontSize = 13.sp, color = TextoSecundario,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                    OutlinedTextField(
                        value = tema, onValueChange = { tema = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Ej: Algebra lineal, matrices, determinantes...",
                            color = Color(0xFFBBBBBB)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdePrimario,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    if (errorMsg.isNotEmpty())
                        Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (tema.isEmpty()) { errorMsg = "Escribe el tema"; return@Button }
                            generando = true; errorMsg = ""
                            scope.launch {
                                try {
                                    val respuesta = client.post(
                                        "${ApiConfig.BASE_URL}/tarjetas/generar"
                                    ) {
                                        header("Authorization", "Bearer $token")
                                        contentType(ContentType.Application.Json)
                                        setBody("""{"materiaId":${materia.id},"examenId":0,""" +
                                            """"materia":"${materia.nombre}","texto":"$tema"}""")
                                    }
                                    val resp = respuesta.bodyAsText()

                                    if (respuesta.status == HttpStatusCode.UnprocessableEntity) {
                                        errorMsg = "La IA no pudo generar tarjetas. Intenta de nuevo."
                                    } else {
                                        val parsed = jsonParser.decodeFromString<GenerarTarjetasApiResponse>(resp)
                                        tarjetas = parsed.tarjetas.map { Tarjeta(it.id, it.pregunta, it.respuesta) }
                                    }
                                } catch (e: Exception) { errorMsg = "Error al generar" }
                                generando = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario),
                        enabled = !generando
                    ) {
                        if (generando)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else
                            Text("Generar tarjetas con IA", color = Color.White,
                                fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            // Mostrar tarjetas
            Text("${tarjetas.size} tarjetas de ${materia.nombre}",
                fontSize = 14.sp, color = TextoSecundario,
                modifier = Modifier.padding(bottom = 12.dp))
            tarjetas.forEach { tarjeta ->
                TarjetaCard(tarjeta)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TarjetaCard(tarjeta: Tarjeta) {
    var mostrarRespuesta by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { mostrarRespuesta = !mostrarRespuesta },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pregunta", fontSize = 11.sp, color = VerdePrimario,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp))
            Text(tarjeta.pregunta, fontSize = 14.sp, color = TextoPrimario)
            AnimatedVisibility(visible = mostrarRespuesta) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                    Text("Respuesta", fontSize = 11.sp, color = Color(0xFF8B6914),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp))
                    Text(tarjeta.respuesta, fontSize = 14.sp, color = TextoPrimario)
                }
            }
            Text(
                if (mostrarRespuesta) "Tocar para ocultar" else "Tocar para ver respuesta",
                fontSize = 11.sp, color = TextoSecundario,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
