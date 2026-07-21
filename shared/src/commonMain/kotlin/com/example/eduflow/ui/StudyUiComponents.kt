package com.example.eduflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Marca vectorial propia de EduFlow: libro abierto + hoja de progreso. */
@Composable
fun EduFlowMark(
    modifier: Modifier = Modifier,
    color: Color = VerdePrimario,
    background: Color = Color(0xFFDDE8E0)
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(16.dp)) {
            val w = size.width
            val h = size.height
            val stroke = (w * 0.065f).coerceAtLeast(2f)

            // Libro abierto.
            drawLine(color, Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.78f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.50f, h * 0.38f), Offset(w * 0.18f, h * 0.25f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.18f, h * 0.25f), Offset(w * 0.18f, h * 0.68f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.18f, h * 0.68f), Offset(w * 0.50f, h * 0.78f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.50f, h * 0.38f), Offset(w * 0.82f, h * 0.25f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.82f, h * 0.25f), Offset(w * 0.82f, h * 0.68f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.82f, h * 0.68f), Offset(w * 0.50f, h * 0.78f), stroke, StrokeCap.Round)

            // Hoja / avance que nace del centro del libro.
            drawArc(
                color = color,
                startAngle = 205f,
                sweepAngle = 125f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.03f),
                size = Size(w * 0.34f, h * 0.34f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(color, Offset(w * 0.53f, h * 0.30f), Offset(w * 0.67f, h * 0.12f), stroke * 0.75f, StrokeCap.Round)
        }
    }
}

@Composable
fun BotonVolver(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(width = 44.dp, height = 40.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFFDDE8E0)
    ) {
        Canvas(Modifier.fillMaxSize().padding(11.dp)) {
            val grosor = (size.width * .11f).coerceAtLeast(2f)
            drawLine(
                color = VerdePrimario,
                start = Offset(size.width * .62f, size.height * .18f),
                end = Offset(size.width * .30f, size.height * .50f),
                strokeWidth = grosor,
                cap = StrokeCap.Round
            )
            drawLine(
                color = VerdePrimario,
                start = Offset(size.width * .30f, size.height * .50f),
                end = Offset(size.width * .62f, size.height * .82f),
                strokeWidth = grosor,
                cap = StrokeCap.Round
            )
        }
    }
}

enum class NavIcono { INICIO, TARJETAS, AUDIOS, COMUNIDAD }

@Composable
private fun NavIcon(icono: NavIcono, selected: Boolean, modifier: Modifier = Modifier) {
    val color = if (selected) Color.White else TextoSecundario
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = (w * 0.09f).coerceAtLeast(1.8f)
        when (icono) {
            NavIcono.INICIO -> {
                drawLine(color, Offset(w * .18f, h * .48f), Offset(w * .50f, h * .20f), s, StrokeCap.Round)
                drawLine(color, Offset(w * .50f, h * .20f), Offset(w * .82f, h * .48f), s, StrokeCap.Round)
                drawRoundRect(color, Offset(w * .27f, h * .44f), Size(w * .46f, h * .40f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .07f), style = Stroke(s))
            }
            NavIcono.TARJETAS -> {
                drawRoundRect(color, Offset(w * .20f, h * .22f), Size(w * .56f, h * .58f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .07f), style = Stroke(s))
                drawLine(color, Offset(w * .34f, h * .40f), Offset(w * .64f, h * .40f), s * .7f, StrokeCap.Round)
                drawLine(color, Offset(w * .34f, h * .56f), Offset(w * .59f, h * .56f), s * .7f, StrokeCap.Round)
            }
            NavIcono.AUDIOS -> {
                drawArc(color, 180f, 180f, false, Offset(w * .18f, h * .18f), Size(w * .64f, h * .62f), Stroke(s, cap = StrokeCap.Round))
                drawRoundRect(color, Offset(w * .12f, h * .48f), Size(w * .18f, h * .30f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .06f))
                drawRoundRect(color, Offset(w * .70f, h * .48f), Size(w * .18f, h * .30f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .06f))
            }
            NavIcono.COMUNIDAD -> {
                drawCircle(color, w * .13f, Offset(w * .38f, h * .36f))
                drawCircle(color, w * .11f, Offset(w * .68f, h * .40f))
                drawArc(color, 180f, 180f, true, Offset(w * .18f, h * .50f), Size(w * .42f, h * .34f))
                drawArc(color, 180f, 180f, true, Offset(w * .51f, h * .55f), Size(w * .34f, h * .28f))
            }
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    selected: Boolean,
    icono: NavIcono,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = if (selected) VerdePrimario else Color.Transparent,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                NavIcon(icono, selected, Modifier.size(23.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = if (selected) VerdePrimario else TextoSecundario,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun SelectorDificultad(nivel: Int, onSeleccionar: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1..5, 6..10).forEach { rango ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rango.forEach { n ->
                    val seleccionado = n == nivel
                    Surface(
                        modifier = Modifier.weight(1f).height(40.dp).clickable { onSeleccionar(n) },
                        shape = RoundedCornerShape(11.dp),
                        color = if (seleccionado) VerdePrimario else Color(0xFFF4F2EC),
                        border = if (seleccionado) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E0D7))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                n.toString(),
                                fontSize = 13.sp,
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                color = if (seleccionado) Color.White else TextoPrimario
                            )
                        }
                    }
                }
            }
        }
        Text(
            when (nivel) {
                in 1..3 -> "Carga ligera"
                in 4..6 -> "Carga media"
                in 7..8 -> "Carga alta"
                else -> "Requiere preparación constante"
            },
            fontSize = 11.sp,
            color = TextoSecundario
        )
    }
}

private val gradosAcademicos = listOf("LICENCIATURA", "MAESTRIA", "DOCTORADO")

fun etiquetaGrado(grado: String): String = when (grado.uppercase()) {
    "MAESTRIA" -> "Maestría"
    "DOCTORADO" -> "Doctorado"
    else -> "Licenciatura"
}

@Composable
fun SelectorGradoAcademico(grado: String, onSeleccionar: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Text("Grado académico", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
    Box {
        OutlinedTextField(
            value = etiquetaGrado(grado), onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            trailingIcon = { Text("⌄", color = VerdePrimario, fontSize = 16.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VerdePrimario,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        Box(Modifier.matchParentSize().clickable { expandido = true })
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            gradosAcademicos.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(etiquetaGrado(opcion), fontSize = 13.sp) },
                    onClick = { onSeleccionar(opcion); expandido = false }
                )
            }
        }
    }
}

val tiposExamen = listOf(
    "Examen mensual", "Primer parcial", "Segundo parcial", "Evaluación práctica", "Examen final"
)

@Composable
fun SelectorTipoExamen(valor: String, onSeleccionar: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Text("Tipo de evaluación", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
    Box {
        OutlinedTextField(
            value = valor, onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            trailingIcon = { Text("⌄", color = VerdePrimario, fontSize = 16.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VerdePrimario,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        Box(Modifier.matchParentSize().clickable { expandido = true })
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            tiposExamen.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, fontSize = 13.sp) },
                    onClick = { onSeleccionar(opcion); expandido = false }
                )
            }
        }
    }
}

private val meses = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

fun etiquetaFecha(fechaIso: String): String {
    return try {
        val f = LocalDate.parse(fechaIso)
        "${f.dayOfMonth} de ${meses[f.monthNumber - 1]} de ${f.year}"
    } catch (_: Exception) { fechaIso }
}

fun fechasDisponibles(soloMesActual: Boolean, maxDias: Int = 90): List<LocalDate> {
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return (0..maxDias).map { hoy.plus(it, DateTimeUnit.DAY) }
        .filter { !soloMesActual || (it.year == hoy.year && it.monthNumber == hoy.monthNumber) }
}

@Composable
fun SelectorFechaProxima(
    fechaSeleccionada: String,
    soloMesActual: Boolean = false,
    maxDias: Int = 90,
    onSeleccionar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val opciones = remember(soloMesActual, maxDias) { fechasDisponibles(soloMesActual, maxDias) }
    Text("Fecha del examen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
    Box {
        OutlinedTextField(
            value = if (fechaSeleccionada.isBlank()) "Selecciona una fecha" else etiquetaFecha(fechaSeleccionada),
            onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            trailingIcon = { Text("▣", color = VerdePrimario, fontSize = 15.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VerdePrimario,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        Box(Modifier.matchParentSize().clickable { expandido = true })
        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
            modifier = Modifier.heightIn(max = 340.dp)
        ) {
            opciones.forEach { fecha ->
                DropdownMenuItem(
                    text = { Text(etiquetaFecha(fecha.toString()), fontSize = 13.sp) },
                    onClick = { onSeleccionar(fecha.toString()); expandido = false }
                )
            }
        }
    }
}

fun perteneceAlMesActual(fechaIso: String): Boolean {
    return try {
        val fecha = LocalDate.parse(fechaIso)
        val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
        fecha.year == hoy.year && fecha.monthNumber == hoy.monthNumber
    } catch (_: Exception) { false }
}

fun tieneExamenMesActual(fechas: List<String>): Boolean = fechas.any(::perteneceAlMesActual)
