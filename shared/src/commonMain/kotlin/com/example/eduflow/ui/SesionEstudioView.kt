package com.example.eduflow.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.api.ExamenEstudioDto
import com.example.eduflow.api.HistorialSesionesDto
import com.example.eduflow.api.MateriaEstudioDto
import com.example.eduflow.api.SesionesEstudioApi
import com.example.eduflow.notifications.NotificationScheduler
import com.example.eduflow.sensor.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock

private enum class EtapaEstudio { SELECCION, PREPARACION, INSTRUCCION, ACTIVA, RESUMEN }

@Composable
fun SesionEstudioView(onVolver: () -> Unit) {
    val scope = rememberCoroutineScope()
    val api = remember { SesionesEstudioApi() }
    val viewModel = remember { SesionEstudioViewModel(scope = scope) }
    val estado by viewModel.uiState.collectAsState()

    var etapa by remember { mutableStateOf(EtapaEstudio.SELECCION) }
    var materias by remember { mutableStateOf<List<MateriaEstudioDto>>(emptyList()) }
    var examenes by remember { mutableStateOf<List<ExamenEstudioDto>>(emptyList()) }
    var historial by remember { mutableStateOf(HistorialSesionesDto(emptyList(), 0L)) }
    var cargando by remember { mutableStateOf(true) }
    var cargandoDetalle by remember { mutableStateOf(false) }
    var errorCarga by remember { mutableStateOf("") }
    var mostrarConsentimiento by remember { mutableStateOf(false) }
    var tiempoVisible by remember { mutableLongStateOf(0L) }

    val materia = estado.materia
    val diasExamen = examenes.mapNotNull { diasHasta(it.fecha) }
        .filter { it >= 0L }
        .minOrNull()
    val proximoExamen = examenes
        .mapNotNull { examen -> diasHasta(examen.fecha)?.takeIf { it >= 0 }?.let { examen to it } }
        .minByOrNull { it.second }
    val metaMinutos = calcularMinutosIdeales(materia?.dificultad ?: 1, diasExamen)

    suspend fun cargarDetalle(materiaId: Int) {
        cargandoDetalle = true
        errorCarga = ""
        examenes = emptyList()
        historial = HistorialSesionesDto(emptyList(), 0L)
        try {
            examenes = api.cargarExamenes(materiaId)
            historial = api.cargarHistorial(materiaId)
        } catch (_: Exception) {
            examenes = emptyList()
            historial = HistorialSesionesDto(emptyList(), 0L)
            errorCarga = "No se pudo cargar todo el historial. Puedes continuar con la sesión."
        }
        cargandoDetalle = false
    }

    LaunchedEffect(Unit) {
        try {
            materias = api.cargarMaterias()
        } catch (_: Exception) {
            errorCarga = "No se pudieron cargar tus materias. Revisa tu conexión."
        }
        cargando = false
    }

    LaunchedEffect(estado.estado) {
        if (estado.estado in setOf(
                EstadoSesionEstudio.ESPERANDO_TELEFONO,
                EstadoSesionEstudio.ACTIVA,
                EstadoSesionEstudio.PAUSADA
            )) {
            while (true) {
                tiempoVisible = viewModel.tiempoActualMillis()
                delay(250)
            }
        } else {
            tiempoVisible = estado.tiempoAcumuladoMillis
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.close()
            api.close()
        }
    }

    fun volverSegunEtapa() {
        when (etapa) {
            EtapaEstudio.SELECCION -> onVolver()
            EtapaEstudio.PREPARACION -> etapa = EtapaEstudio.SELECCION
            EtapaEstudio.INSTRUCCION -> etapa = EtapaEstudio.PREPARACION
            EtapaEstudio.ACTIVA -> Unit
            EtapaEstudio.RESUMEN -> onVolver()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Beige)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(Modifier.fillMaxSize()) {
            StudyTopBar(
                title = when (etapa) {
                    EtapaEstudio.SELECCION -> "Sesión de estudio"
                    EtapaEstudio.PREPARACION -> "Preparación"
                    EtapaEstudio.INSTRUCCION -> "Cómo colocar el teléfono"
                    EtapaEstudio.ACTIVA -> "Estudio en curso"
                    EtapaEstudio.RESUMEN -> "Sesión finalizada"
                },
                backEnabled = etapa != EtapaEstudio.ACTIVA,
                onBack = ::volverSegunEtapa
            )

            when (etapa) {
                EtapaEstudio.SELECCION -> SeleccionMateriaStep(
                    materias = materias,
                    seleccionada = materia,
                    historial = historial,
                    cargando = cargando,
                    cargandoDetalle = cargandoDetalle,
                    error = errorCarga,
                    onSelect = { seleccionada ->
                        viewModel.seleccionarMateria(seleccionada)
                        scope.launch { cargarDetalle(seleccionada.id) }
                    },
                    onContinue = { etapa = EtapaEstudio.PREPARACION }
                )

                EtapaEstudio.PREPARACION -> PreparacionStep(
                    materia = materia!!,
                    metaMinutos = metaMinutos,
                    proximoExamen = proximoExamen,
                    totalEstudiadoSegundos = historial.totalSegundos,
                    onContinue = { etapa = EtapaEstudio.INSTRUCCION }
                )

                EtapaEstudio.INSTRUCCION -> InstruccionStep(
                    sensorDisponible = estado.sensorDisponible,
                    onStart = {
                        if (SensorConsentStore.isAccepted()) {
                            if (viewModel.iniciarSesion()) etapa = EtapaEstudio.ACTIVA
                        } else {
                            mostrarConsentimiento = true
                        }
                    }
                )

                EtapaEstudio.ACTIVA -> SesionActivaStep(
                    materia = materia!!,
                    estado = estado,
                    tiempoMillis = tiempoVisible,
                    metaMinutos = metaMinutos,
                    onSimulate = viewModel::simularSensor,
                    onFinish = {
                        if (viewModel.finalizarSesion()) {
                            etapa = EtapaEstudio.RESUMEN
                            scope.launch {
                                val resultado = viewModel.guardarSesion(metaMinutos)
                                if (resultado.isSuccess) {
                                    NotificationScheduler.sincronizarAhora()
                                    cargarDetalle(materia!!.id)
                                }
                            }
                        }
                    }
                )

                EtapaEstudio.RESUMEN -> ResumenSesionStep(
                    materia = materia!!,
                    estado = estado,
                    tiempoMillis = tiempoVisible,
                    metaMinutos = metaMinutos,
                    onRetry = {
                        scope.launch {
                            val resultado = viewModel.guardarSesion(metaMinutos)
                            if (resultado.isSuccess) {
                                NotificationScheduler.sincronizarAhora()
                                cargarDetalle(materia!!.id)
                            }
                        }
                    },
                    onNewSession = {
                        viewModel.prepararNuevaSesion()
                        etapa = EtapaEstudio.SELECCION
                    },
                    onDashboard = onVolver
                )
            }
        }
    }

    if (mostrarConsentimiento) {
        AlertDialog(
            onDismissRequest = { mostrarConsentimiento = false },
            title = { Text("Autorizar uso del sensor", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "EduFlow utilizará el sensor de proximidad únicamente para detectar si el teléfono está boca abajo y medir el tiempo de estudio."
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDDE8E0)) {
                        Text(
                            "TYPE_PROXIMITY no requiere un permiso oficial de Android. No se capturan imágenes, audio, ubicación ni información personal.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = VerdePrimario,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SensorConsentStore.accept()
                        mostrarConsentimiento = false
                        if (viewModel.iniciarSesion()) etapa = EtapaEstudio.ACTIVA
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                ) { Text("Autorizar e iniciar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConsentimiento = false }) {
                    Text("Ahora no", color = TextoSecundario)
                }
            }
        )
    }
}

@Composable
private fun StudyTopBar(title: String, backEnabled: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            if (backEnabled) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 22.sp, color = VerdePrimario)
                }
            }
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VerdePrimario
        )
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            HourglassIcon(Modifier.size(22.dp), VerdePrimario)
        }
    }
}

@Composable
private fun SeleccionMateriaStep(
    materias: List<MateriaEstudioDto>,
    seleccionada: MateriaEstudioDto?,
    historial: HistorialSesionesDto,
    cargando: Boolean,
    cargandoDetalle: Boolean,
    error: String,
    onSelect: (MateriaEstudioDto) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp)
    ) {
        Text("Selecciona una materia", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
        Text(
            "El tiempo registrado quedará asociado a la materia que elijas.",
            fontSize = 13.sp, color = TextoSecundario, lineHeight = 19.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 18.dp)
        )

        if (cargando) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdePrimario)
            }
        } else if (materias.isEmpty()) {
            EmptyStudyCard("Primero agrega una materia desde el Dashboard.")
        } else {
            materias.forEach { item ->
                val selected = seleccionada?.id == item.id
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        .clickable { onSelect(item) },
                    shape = RoundedCornerShape(18.dp),
                    border = if (selected) BorderStroke(2.dp, VerdePrimario) else null,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(if (selected) 4.dp else 1.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).background(
                                if (selected) VerdePrimario else Color(0xFFEDE5D8),
                                RoundedCornerShape(14.dp)
                            ), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                item.nombre.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else VerdePrimario
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                            Text("Dificultad ${item.dificultad}/10", fontSize = 12.sp, color = TextoSecundario)
                        }
                        Text(if (selected) "✓" else "›", fontSize = 22.sp, color = VerdePrimario)
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(error, fontSize = 12.sp, color = Color(0xFF8B6914), modifier = Modifier.padding(vertical = 8.dp))
        }

        if (seleccionada != null) {
            Spacer(Modifier.height(12.dp))
            Text("Historial reciente", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
            if (cargandoDetalle) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = VerdePrimario,
                    trackColor = Color(0xFFDDE8E0)
                )
            } else if (historial.sesiones.isEmpty()) {
                Text("Aún no hay sesiones registradas para esta materia.", fontSize = 12.sp,
                    color = TextoSecundario, modifier = Modifier.padding(top = 8.dp))
            } else {
                historial.sesiones.take(3).forEach { sesion ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 9.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sesion.fecha, fontSize = 12.sp, color = TextoSecundario, modifier = Modifier.weight(1f))
                        Text(formatearSegundos(sesion.duracionSegundos.toLong()), fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, color = VerdePrimario)
                    }
                }
                Text(
                    "Total: ${formatearSegundos(historial.totalSegundos)}",
                    fontSize = 12.sp, color = TextoSecundario,
                    modifier = Modifier.padding(top = 9.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onContinue,
                enabled = !cargandoDetalle,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
            ) {
                Text("Continuar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PreparacionStep(
    materia: MateriaEstudioDto,
    metaMinutos: Int,
    proximoExamen: Pair<ExamenEstudioDto, Long>?,
    totalEstudiadoSegundos: Long,
    onContinue: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(materia.nombre, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextoPrimario,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        Text("Dificultad ${materia.dificultad}/10", fontSize = 13.sp, color = TextoSecundario)

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = VerdePrimario)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                HourglassIcon(Modifier.size(42.dp), Color.White)
                Text("Tiempo ideal para hoy", fontSize = 13.sp, color = Color.White.copy(alpha = .78f),
                    modifier = Modifier.padding(top = 12.dp))
                Text("$metaMinutos minutos", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "La meta se calcula con la dificultad${if (proximoExamen != null) " y la cercanía del examen" else ""}.",
                    fontSize = 12.sp, color = Color.White.copy(alpha = .75f), textAlign = TextAlign.Center
                )
            }
        }

        if (proximoExamen != null) {
            InfoStudyCard(
                title = if (proximoExamen.second == 0L) "Examen hoy" else "Próximo examen",
                body = "${proximoExamen.first.nombre}: ${textoDias(proximoExamen.second)}"
            )
        }
        InfoStudyCard(
            title = "Tiempo acumulado",
            body = if (totalEstudiadoSegundos == 0L) "Esta será tu primera sesión registrada."
            else "Ya estudiaste ${formatearSegundos(totalEstudiadoSegundos)} en esta materia."
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
        ) { Text("Ver instrucciones", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun InstruccionStep(sensorDisponible: Boolean, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhonePlacementAnimation(Modifier.fillMaxWidth().height(270.dp))
        Text("Coloca el teléfono boca abajo", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = TextoPrimario, textAlign = TextAlign.Center)
        Text(
            "Déjalo sobre una superficie plana con la pantalla orientada hacia la mesa. El cronómetro se iniciará cuando el sensor quede cubierto.",
            fontSize = 13.sp, lineHeight = 19.sp, color = TextoSecundario,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 9.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            shape = RoundedCornerShape(14.dp),
            color = if (sensorDisponible) Color(0xFFDDE8E0) else Color(0xFFFFF2D5)
        ) {
            Text(
                if (sensorDisponible) "Sensor de proximidad detectado."
                else "Emulador o dispositivo sin sensor: se habilitarán controles de demostración.",
                fontSize = 12.sp,
                color = if (sensorDisponible) VerdePrimario else Color(0xFF76520B),
                modifier = Modifier.padding(13.dp), textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
        ) { Text("Iniciar sesión", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SesionActivaStep(
    materia: MateriaEstudioDto,
    estado: SesionEstudioUiState,
    tiempoMillis: Long,
    metaMinutos: Int,
    onSimulate: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val activo = estado.estado == EstadoSesionEstudio.ACTIVA
    val progreso = (tiempoMillis / (metaMinutos * 60_000f)).coerceIn(0f, 1f)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            when (estado.estado) {
                EstadoSesionEstudio.ACTIVA -> "ESTUDIO ACTIVO"
                EstadoSesionEstudio.PAUSADA -> "CRONÓMETRO PAUSADO"
                else -> "ESPERANDO EL SENSOR"
            },
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = if (activo) VerdePrimario else Color(0xFF8B6914),
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(materia.nombre, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextoPrimario,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (activo) VerdePrimario else Color.White
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(78.dp).background(
                        if (activo) Color.White.copy(alpha = .16f) else Color(0xFFDDE8E0), CircleShape
                    ), contentAlignment = Alignment.Center
                ) {
                    HourglassIcon(Modifier.size(34.dp), if (activo) Color.White else VerdePrimario)
                }
                Text(formatearMillis(tiempoMillis), fontSize = 42.sp, fontWeight = FontWeight.Bold,
                    color = if (activo) Color.White else VerdePrimario,
                    modifier = Modifier.padding(top = 18.dp))
                Text(
                    when (estado.estado) {
                        EstadoSesionEstudio.ACTIVA -> "El teléfono está boca abajo."
                        EstadoSesionEstudio.PAUSADA -> "Levantaste el teléfono; el tiempo se conserva."
                        else -> "Pon el teléfono boca abajo para comenzar."
                    },
                    fontSize = 12.sp, textAlign = TextAlign.Center,
                    color = if (activo) Color.White.copy(alpha = .78f) else TextoSecundario
                )
                if (estado.distanciaCm != null && estado.sensorDisponible) {
                    Text("Lectura: ${estado.distanciaCm} cm", fontSize = 10.sp,
                        color = if (activo) Color.White.copy(alpha = .65f) else TextoSecundario,
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Meta de hoy", fontSize = 12.sp, color = TextoSecundario)
                Spacer(Modifier.weight(1f))
                Text("${(progreso * 100).roundToInt()}% de $metaMinutos min", fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
            }
            LinearProgressIndicator(
                progress = { progreso }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(8.dp),
                color = VerdePrimario, trackColor = Color(0xFFDDE8E0)
            )
        }

        if (!estado.sensorDisponible) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2D5))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Modo de demostración", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = Color(0xFF76520B))
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSimulate(false) }, modifier = Modifier.weight(1f)) {
                            Text("Boca arriba", fontSize = 11.sp)
                        }
                        Button(onClick = { onSimulate(true) }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)) {
                            Text("Boca abajo", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        if (!estado.error.isNullOrBlank()) {
            Text(estado.error.orEmpty(), color = Color(0xFFB00020), fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onFinish,
            enabled = tiempoMillis > 0L,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, Color(0xFF9B3A32))
        ) { Text("Finalizar sesión", color = Color(0xFF9B3A32), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ResumenSesionStep(
    materia: MateriaEstudioDto,
    estado: SesionEstudioUiState,
    tiempoMillis: Long,
    metaMinutos: Int,
    onRetry: () -> Unit,
    onNewSession: () -> Unit,
    onDashboard: () -> Unit
) {
    val progreso = (tiempoMillis / (metaMinutos * 60_000f)).coerceIn(0f, 1f)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(86.dp).background(Color(0xFFDDE8E0), CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
        }
        Text("¡Buen trabajo!", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = TextoPrimario,
            modifier = Modifier.padding(top = 14.dp))
        Text(materia.nombre, fontSize = 13.sp, color = TextoSecundario)

        SummaryMetric("Tiempo registrado", formatearMillis(tiempoMillis))
        SummaryMetric("Meta alcanzada", "${(progreso * 100).roundToInt()}% de $metaMinutos minutos")

        when (estado.estado) {
            EstadoSesionEstudio.GUARDANDO -> Row(
                Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), color = VerdePrimario, strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text("Guardando en Railway...", fontSize = 12.sp, color = TextoSecundario)
            }
            EstadoSesionEstudio.GUARDADA -> Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp), color = Color(0xFFDDE8E0)
            ) {
                Text("Sesión guardada correctamente en MySQL.", fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = VerdePrimario,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(13.dp))
            }
            EstadoSesionEstudio.ERROR -> {
                Text(estado.error ?: "No se pudo guardar la sesión.", color = Color(0xFFB00020),
                    fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)) {
                    Text("Reintentar guardado", color = Color.White)
                }
            }
            else -> Unit
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNewSession,
            enabled = estado.estado != EstadoSesionEstudio.GUARDANDO,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
        ) { Text("Nueva sesión", color = Color.White, fontWeight = FontWeight.Bold) }
        OutlinedButton(
            onClick = onDashboard,
            enabled = estado.estado != EstadoSesionEstudio.GUARDANDO,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, VerdePrimario)
        ) { Text("Volver al Dashboard", color = VerdePrimario) }
    }
}

@Composable
private fun PhonePlacementAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "phone-placement")
    val raw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "placement-progress"
    )
    Canvas(modifier) {
        val motion = when {
            raw < .12f -> 0f
            raw < .78f -> (raw - .12f) / .66f
            else -> 1f
        }
        val tableY = size.height * .78f
        drawLine(Color(0xFFCFC5B6), Offset(size.width * .12f, tableY), Offset(size.width * .88f, tableY),
            strokeWidth = 5f, cap = StrokeCap.Round)

        val phoneW = mix(size.width * .22f, size.width * .43f, motion)
        val phoneH = mix(size.height * .50f, size.height * .10f, motion)
        val left = (size.width - phoneW) / 2f
        val top = mix(size.height * .10f, tableY - phoneH - 4f, motion)
        val phoneColor = if (motion < .68f) VerdePrimario else Color(0xFF244733)
        drawRoundRect(phoneColor, Offset(left, top), Size(phoneW, phoneH), CornerRadius(22f, 22f))

        if (motion < .65f) {
            drawRoundRect(
                Color(0xFFF8F4ED),
                Offset(left + phoneW * .09f, top + phoneH * .07f),
                Size(phoneW * .82f, phoneH * .80f),
                CornerRadius(15f, 15f)
            )
            drawCircle(Color(0xFF222222), phoneW * .025f, Offset(size.width / 2f, top + phoneH * .04f))
        } else {
            drawCircle(Color(0xFF162D20), phoneH * .12f, Offset(left + phoneW * .16f, top + phoneH * .5f))
        }

        if (motion < .72f) {
            drawArc(
                color = Color(0xFF8B6914),
                startAngle = 210f,
                sweepAngle = 230f,
                useCenter = false,
                topLeft = Offset(size.width * .27f, size.height * .03f),
                size = Size(size.width * .46f, size.height * .42f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            val arrow = Path().apply {
                moveTo(size.width * .70f, size.height * .18f)
                lineTo(size.width * .64f, size.height * .14f)
                lineTo(size.width * .65f, size.height * .22f)
                close()
            }
            drawPath(arrow, Color(0xFF8B6914))
        }

        if (motion > .88f) {
            drawCircle(Color(0xFF7BAA8A), 26f, Offset(size.width * .78f, tableY - 48f))
            drawLine(Color.White, Offset(size.width * .75f, tableY - 48f),
                Offset(size.width * .77f, tableY - 30f), strokeWidth = 5f, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(size.width * .77f, tableY - 30f),
                Offset(size.width * .82f, tableY - 64f), strokeWidth = 5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun InfoStudyCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
            Text(body, fontSize = 12.sp, color = TextoSecundario, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = TextoSecundario)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VerdePrimario,
                modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun EmptyStudyCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center,
            fontSize = 13.sp, color = TextoSecundario)
    }
}

private fun diasHasta(fecha: String): Long? = runCatching {
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    (LocalDate.parse(fecha).toEpochDays() - hoy.toEpochDays()).toLong()
}.getOrNull()

private fun textoDias(dias: Long): String = when (dias) {
    0L -> "es hoy"
    1L -> "es mañana"
    else -> "faltan $dias días"
}

private fun formatearMillis(millis: Long): String = formatearSegundos(millis / 1000L)

private fun formatearSegundos(total: Long): String {
    val horas = total / 3600
    val minutos = (total % 3600) / 60
    val segundos = total % 60
    return "${horas.pad2()}:${minutos.pad2()}:${segundos.pad2()}"
}

private fun Long.pad2(): String = toString().padStart(2, '0')
private fun mix(start: Float, end: Float, amount: Float): Float = start + (end - start) * amount.coerceIn(0f, 1f)
