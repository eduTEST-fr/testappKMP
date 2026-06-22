package com.example.eduflow.ui

import androidx.compose.animation.AnimatedVisibility
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
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Tarjeta(
    val id: Int, val pregunta: String, val respuesta: String,
    val tema: String, val completado: Boolean
)

// Una subcarpeta = todas las tarjetas que comparten "tema" dentro de una materia.
data class SubcarpetaTarjetas(val tema: String, val tarjetas: List<Tarjeta>, val completado: Boolean)

@Serializable
private data class TarjetaApiDto(
    val id: Int, val pregunta: String, val respuesta: String,
    val tema: String = "General", val completado: Boolean = false
)

private val jsonParser = Json { ignoreUnknownKeys = true }

suspend fun cargarTarjetasMateria(client: HttpClient, token: String, materiaId: Int): List<Tarjeta> {
    return try {
        val resp = client.get("${ApiConfig.BASE_URL}/tarjetas/$materiaId") {
            header("Authorization", "Bearer $token")
        }.bodyAsText()
        jsonParser.decodeFromString<List<TarjetaApiDto>>(resp)
            .map { Tarjeta(it.id, it.pregunta, it.respuesta, it.tema, it.completado) }
    } catch (e: Exception) { emptyList() }
}

fun agruparPorTema(tarjetas: List<Tarjeta>): List<SubcarpetaTarjetas> =
    tarjetas.groupBy { it.tema }
        .map { (tema, lista) -> SubcarpetaTarjetas(tema, lista, lista.all { it.completado }) }
        .sortedBy { it.tema }

// Biblioteca de tarjetas de UNA materia: lista de subcarpetas (temas) y,
// al entrar a una, las tarjetas de ese tema con su validacion de estudio.
@Composable
fun TarjetasBibliotecaView(materia: MateriaUI, token: String) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }

    var tarjetas    by remember { mutableStateOf<List<Tarjeta>>(emptyList()) }
    var cargando    by remember { mutableStateOf(true) }
    var temaActivo  by remember { mutableStateOf<String?>(null) }
    var mostrarNueva by remember { mutableStateOf(false) }
    var generando   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    suspend fun recargar() {
        cargando = true
        tarjetas = cargarTarjetasMateria(client, token, materia.id)
        cargando = false
    }

    LaunchedEffect(materia.id) { recargar() }

    val subcarpetas = remember(tarjetas) { agruparPorTema(tarjetas) }
    val subcarpetaActiva = subcarpetas.find { it.tema == temaActivo }

    if (mostrarNueva) {
        NuevaSubcarpetaDialog(
            generando = generando,
            errorMsg = errorMsg,
            onCerrar = { mostrarNueva = false; errorMsg = "" },
            onGuardar = { tema, texto ->
                generando = true; errorMsg = ""
                scope.launch {
                    try {
                        val respuesta = client.post("${ApiConfig.BASE_URL}/tarjetas/generar") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                "{\"materiaId\":${materia.id},\"examenId\":0," +
                                "\"materia\":\"${escaparJson(materia.nombre)}\"," +
                                "\"texto\":\"${escaparJson(texto)}\"," +
                                "\"tema\":\"${escaparJson(tema)}\"}"
                            )
                        }
                        if (respuesta.status == HttpStatusCode.UnprocessableEntity) {
                            errorMsg = "La IA no pudo generar tarjetas. Intenta con otro texto."
                        } else {
                            recargar()
                            mostrarNueva = false
                            temaActivo = tema
                        }
                    } catch (e: Exception) { errorMsg = "Error al generar. Revisa tu conexión." }
                    generando = false
                }
            }
        )
    }

    when {
        cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VerdePrimario)
        }
        subcarpetaActiva != null -> SubcarpetaTarjetasDetalle(
            materia = materia,
            subcarpeta = subcarpetaActiva,
            token = token,
            onVolver = { temaActivo = null },
            onCompletado = {
                scope.launch {
                    try {
                        client.put("${ApiConfig.BASE_URL}/tarjetas/completar") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody("{\"materiaId\":${materia.id},\"tema\":\"${escaparJson(subcarpetaActiva.tema)}\"}")
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
                Text("Subcarpetas de tarjetas", fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = TextoPrimario,
                    modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VerdePrimario,
                    modifier = Modifier.clickable { mostrarNueva = true }
                ) {
                    Text("+ Nueva", fontSize = 12.sp, color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }

            if (subcarpetas.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aún no hay tarjetas", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Crea tu primera subcarpeta por tema para empezar.",
                            fontSize = 12.sp, color = TextoSecundario, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                subcarpetas.forEach { sub ->
                    SubcarpetaCard(sub) { temaActivo = sub.tema }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SubcarpetaCard(sub: SubcarpetaTarjetas, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDDE8E0)),
                contentAlignment = Alignment.Center
            ) { Text("◆", fontSize = 18.sp, color = VerdePrimario) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sub.tema, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                Text("${sub.tarjetas.size} tarjeta${if (sub.tarjetas.size != 1) "s" else ""}",
                    fontSize = 12.sp, color = TextoSecundario)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (sub.completado) Color(0xFFDDE8E0) else Color(0xFFFFF3E0)
            ) {
                Text(
                    if (sub.completado) "Estudiado" else "Pendiente",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = if (sub.completado) VerdePrimario else Color(0xFF8B6914),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SubcarpetaTarjetasDetalle(
    materia: MateriaUI,
    subcarpeta: SubcarpetaTarjetas,
    token: String,
    onVolver: () -> Unit,
    onCompletado: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                Text("←  ", fontSize = 15.sp, color = VerdePrimario)
                Text("Subcarpetas", fontSize = 13.sp, color = VerdePrimario, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(subcarpeta.tema, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
        Text("${subcarpeta.tarjetas.size} tarjeta${if (subcarpeta.tarjetas.size != 1) "s" else ""} · ${materia.nombre}",
            fontSize = 12.sp, color = TextoSecundario,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

        subcarpeta.tarjetas.forEach { tarjeta ->
            TarjetaCard(tarjeta)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        if (subcarpeta.completado) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFDDE8E0)
            ) {
                Text("✓ Tema estudiado por completo", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = VerdePrimario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
            }
        } else {
            Button(
                onClick = onCompletado,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
            ) {
                Text("Marcar tema como estudiado", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun NuevaSubcarpetaDialog(
    generando: Boolean,
    errorMsg: String,
    onGuardar: (String, String) -> Unit,
    onCerrar: () -> Unit
) {
    var tema  by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    var validacion by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = !generando) { onCerrar() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp).clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Nueva subcarpeta de tarjetas", fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario)
                Text("La IA genera las tarjetas a partir de tus apuntes",
                    fontSize = 12.sp, color = TextoSecundario,
                    modifier = Modifier.padding(top = 2.dp, bottom = 18.dp))

                CampoTexto("Nombre del tema", tema, "Ej: Matrices") { tema = it; validacion = "" }
                Spacer(Modifier.height(10.dp))
                Text("Apuntes o contenido", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = TextoPrimario,
                    modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = texto, onValueChange = { texto = it; validacion = "" },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Pega aquí tus notas o describe el tema...",
                        color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdePrimario,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                val mensajeError = if (validacion.isNotEmpty()) validacion else errorMsg
                if (mensajeError.isNotEmpty())
                    Text(mensajeError, color = Color(0xFFB00020), fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp))

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCerrar,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !generando,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdePrimario),
                        border = BorderStroke(1.dp, VerdePrimario)
                    ) { Text("Cancelar", fontWeight = FontWeight.SemiBold) }

                    Button(
                        onClick = {
                            when {
                                tema.isBlank()  -> validacion = "Escribe el nombre del tema"
                                texto.isBlank() -> validacion = "Agrega tus apuntes o contenido"
                                else -> onGuardar(tema.trim(), texto.trim())
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !generando,
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                    ) {
                        if (generando)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else
                            Text("Generar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
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

// Escapa comillas y saltos de linea para que el texto libre del usuario no
// rompa el JSON armado a mano (mismo criterio que ya usaba el resto de la app).
fun escaparJson(texto: String): String =
    texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
