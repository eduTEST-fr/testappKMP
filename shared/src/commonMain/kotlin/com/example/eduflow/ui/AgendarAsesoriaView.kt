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

@Serializable
private data class DisponibilidadAgendarDto(
    val id: Int, val diaSemana: Int, val horaInicio: String, val horaFin: String,
    val ocupado: Boolean = false
)

private val jsonAgendar = Json { ignoreUnknownKeys = true }
private val NOMBRES_DIA_CORTO = mapOf(
    1 to "Lun", 2 to "Mar", 3 to "Mié", 4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom"
)

// Flujo del Alumno para agendar una asesoría: elige un día (solo los que el
// asesor tiene disponibilidad), luego un horario libre para ese día y confirma.
@Composable
fun AgendarAsesoriaView(asesorId: Int, onVolver: () -> Unit, onSolicitudEnviada: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""

    var disponibilidadBase by remember { mutableStateOf<List<DisponibilidadAgendarDto>>(emptyList()) }
    var fechaSeleccionada   by remember { mutableStateOf<LocalDate?>(null) }
    var horariosDelDia      by remember { mutableStateOf<List<DisponibilidadAgendarDto>>(emptyList()) }
    var horarioSeleccionado by remember { mutableStateOf<DisponibilidadAgendarDto?>(null) }

    var cargando    by remember { mutableStateOf(true) }
    var cargandoDia by remember { mutableStateOf(false) }
    var enviando    by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    var enviado     by remember { mutableStateOf(false) }

    val hoy = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    LaunchedEffect(asesorId) {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/asesorias/disponibilidad/$asesorId") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            disponibilidadBase = jsonAgendar.decodeFromString(resp)
        } catch (e: Exception) { errorMsg = "No se pudo cargar la disponibilidad del asesor." }
        cargando = false
    }

    val diasDisponibles: List<Int> = disponibilidadBase.map { it.diaSemana }.distinct()
    // Próximos 21 días cuyo día de la semana coincide con alguno con disponibilidad
    val fechasCandidatas: List<LocalDate> = remember(diasDisponibles) {
        (0..20).map { hoy.plus(it, DateTimeUnit.DAY) }
            .filter { it.dayOfWeek.isoDayNumber in diasDisponibles }
            .take(8)
    }

    suspend fun cargarHorariosDeFecha(fecha: LocalDate) {
        cargandoDia = true
        horarioSeleccionado = null
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/asesorias/disponibilidad/$asesorId") {
                header("Authorization", "Bearer $token")
                url { parameters.append("fecha", fecha.toString()) }
            }.bodyAsText()
            val lista = jsonAgendar.decodeFromString<List<DisponibilidadAgendarDto>>(resp)
            horariosDelDia = lista.filter { it.diaSemana == fecha.dayOfWeek.isoDayNumber }
        } catch (e: Exception) { errorMsg = "No se pudieron cargar los horarios de ese día." }
        cargandoDia = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("Agendar Asesoría", fontSize = 16.sp,
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

            if (enviado) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✓", fontSize = 40.sp, color = VerdePrimario)
                        Spacer(Modifier.height(12.dp))
                        Text("Solicitud enviada", fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Spacer(Modifier.height(6.dp))
                        Text("El asesor revisará tu solicitud y te notificaremos cuando responda.",
                            fontSize = 13.sp, color = TextoSecundario,
                            modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onSolicitudEnviada,
                            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario),
                            shape = RoundedCornerShape(12.dp)) {
                            Text("Entendido", color = Color.White)
                        }
                    }
                }
                return@Column
            }

            Column(modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp)) {

                if (errorMsg.isNotEmpty())
                    Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp))

                if (fechasCandidatas.isEmpty()) {
                    Text("Este asesor aún no configura su disponibilidad.",
                        fontSize = 13.sp, color = TextoSecundario)
                    return@Column
                }

                Text("Elige un día", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = TextoPrimario, modifier = Modifier.padding(bottom = 10.dp))

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    fechasCandidatas.forEach { fecha ->
                        val seleccionado = fecha == fechaSeleccionada
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (seleccionado) VerdePrimario else Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                                .clickable {
                                    fechaSeleccionada = fecha
                                    scope.launch { cargarHorariosDeFecha(fecha) }
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(NOMBRES_DIA_CORTO[fecha.dayOfWeek.isoDayNumber] ?: "",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (seleccionado) Color.White else TextoPrimario)
                                Text("${fecha.dayOfMonth}/${fecha.monthNumber}", fontSize = 11.sp,
                                    color = if (seleccionado) Color.White else TextoSecundario)
                            }
                        }
                    }
                }

                if (fechaSeleccionada != null) {
                    Spacer(Modifier.height(22.dp))
                    Text("Elige un horario", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = TextoPrimario, modifier = Modifier.padding(bottom = 10.dp))

                    if (cargandoDia) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VerdePrimario, modifier = Modifier.size(24.dp))
                        }
                    } else if (horariosDelDia.all { it.ocupado }) {
                        Text("No hay horarios libres para este día.",
                            fontSize = 12.sp, color = TextoSecundario)
                    } else {
                        Column {
                            horariosDelDia.forEach { h ->
                                val seleccionado = horarioSeleccionado?.id == h.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        h.ocupado -> Color(0xFFEFEAE0)
                                        seleccionado -> VerdePrimario
                                        else -> Color.White
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        .clickable(enabled = !h.ocupado) { horarioSeleccionado = h }
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${h.horaInicio} – ${h.horaFin}", fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (h.ocupado) Color(0xFFAAAAAA)
                                                     else if (seleccionado) Color.White else TextoPrimario)
                                        Spacer(Modifier.weight(1f))
                                        if (h.ocupado)
                                            Text("Ocupado", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        val fecha = fechaSeleccionada
                        val horario = horarioSeleccionado
                        if (fecha != null && horario != null) {
                            enviando = true; errorMsg = ""
                            scope.launch {
                                try {
                                    val resp = client.post("${ApiConfig.BASE_URL}/asesorias") {
                                        header("Authorization", "Bearer $token")
                                        contentType(ContentType.Application.Json)
                                        setBody(
                                            "{\"asesorId\":$asesorId," +
                                            "\"disponibilidadId\":${horario.id}," +
                                            "\"fecha\":\"$fecha\"}"
                                        )
                                    }
                                    if (resp.status.value in 200..299) enviado = true
                                    else errorMsg = "Ese horario ya no está disponible. Elige otro."
                                } catch (e: Exception) {
                                    errorMsg = "No se pudo enviar la solicitud. Revisa tu conexión."
                                }
                                enviando = false
                            }
                        }
                    },
                    enabled = fechaSeleccionada != null && horarioSeleccionado != null && !enviando,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdePrimario,
                        disabledContainerColor = Color(0xFFBFD0C2)
                    )
                ) {
                    if (enviando) CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Confirmar solicitud", color = Color.White,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
