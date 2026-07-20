package com.example.eduflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Portada local generada para cada materia.
 *
 * No usa imágenes externas: identifica el área por palabras clave y dibuja
 * una composición estable con Canvas. La variante compacta se utiliza en el
 * Dashboard y la variante completa en la biblioteca de audios y la vista
 * previa al registrar una materia.
 */
private enum class TemaMateria(
    val etiqueta: String,
    val fondo: Color,
    val secundario: Color
) {
    MATEMATICAS("Matemáticas", Color(0xFF315F8C), Color(0xFF78A7D2)),
    TECNOLOGIA("Tecnología", Color(0xFF24543A), Color(0xFF6FA47F)),
    FISICA("Ciencias", Color(0xFF654B84), Color(0xFFA58BC2)),
    QUIMICA("Química", Color(0xFF8A3F5A), Color(0xFFC8879D)),
    IDIOMAS("Idiomas", Color(0xFF786225), Color(0xFFBDA75B)),
    NEGOCIOS("Negocios", Color(0xFF455E68), Color(0xFF89A6AE)),
    COMUNICACION("Comunicación", Color(0xFF8A5834), Color(0xFFC99369)),
    CREATIVIDAD("Creatividad", Color(0xFF8A4936), Color(0xFFC98067)),
    GENERAL("Materia", Color(0xFF365B49), Color(0xFF80A591))
}

private fun detectarTema(nombre: String): TemaMateria {
    val n = nombre.lowercase()
    return when {
        listOf("mat", "cálc", "calc", "álgebra", "algebra", "geometr", "estadíst", "estadist", "trigon").any { n.contains(it) } -> TemaMateria.MATEMATICAS
        listOf("prog", "cód", "cod", "software", "dato", "algoritmo", "web", "app", "sistema", "redes", "iot", "embeb", "inteligencia artificial", "machine", "base de datos").any { n.contains(it) } -> TemaMateria.TECNOLOGIA
        listOf("fís", "fis", "mecán", "mecan", "eléct", "elect", "óptica", "optica", "ondas").any { n.contains(it) } -> TemaMateria.FISICA
        listOf("quím", "quim", "orgán", "organ", "molecul", "bioqu").any { n.contains(it) } -> TemaMateria.QUIMICA
        listOf("inglés", "ingles", "francés", "frances", "idioma", "lengua").any { n.contains(it) } -> TemaMateria.IDIOMAS
        listOf("admin", "negocio", "contab", "finanz", "econom", "mercado", "emprend", "proyecto").any { n.contains(it) } -> TemaMateria.NEGOCIOS
        listOf("expresión", "expresion", "oral", "comunicación", "comunicacion", "redacción", "redaccion", "liderazgo").any { n.contains(it) } -> TemaMateria.COMUNICACION
        listOf("arte", "diseñ", "disen", "música", "musica", "dibujo", "creativ").any { n.contains(it) } -> TemaMateria.CREATIVIDAD
        else -> TemaMateria.GENERAL
    }
}

@Composable
fun MateriaIlustracion(
    nombre: String,
    modifier: Modifier = Modifier,
    compacta: Boolean = false
) {
    val tema = remember(nombre) { detectarTema(nombre) }
    val titulo = nombre.trim().ifEmpty { "Nueva materia" }
    val radio = if (compacta) 13.dp else 18.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radio))
            .background(tema.fondo)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val min = size.minDimension

            // Formas suaves de fondo para que cada portada tenga profundidad,
            // sin saturar ni parecer una imagen externa pegada.
            drawCircle(
                color = tema.secundario.copy(alpha = 0.34f),
                radius = min * 0.56f,
                center = Offset(size.width * 0.93f, size.height * 0.04f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = min * 0.38f,
                center = Offset(size.width * 0.10f, size.height * 1.02f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.11f),
                start = Offset(size.width * 0.03f, size.height * 0.18f),
                end = Offset(size.width * 0.68f, size.height * 0.86f),
                strokeWidth = (min * 0.035f).coerceAtLeast(2f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(size.width * 0.18f, size.height * 0.04f),
                end = Offset(size.width * 0.80f, size.height * 0.66f),
                strokeWidth = (min * 0.018f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        }

        if (compacta) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(9.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                TemaIconoCanvas(tema, Modifier.fillMaxSize().padding(6.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.66f)
                    .padding(start = 15.dp, bottom = 13.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = tema.etiqueta.uppercase(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = titulo,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .size(62.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                TemaIconoCanvas(tema, Modifier.fillMaxSize().padding(13.dp))
            }
        }
    }
}

@Composable
private fun TemaIconoCanvas(tema: TemaMateria, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val color = Color.White
        val linea = (size.minDimension * 0.085f).coerceAtLeast(2f)
        val stroke = Stroke(width = linea, cap = StrokeCap.Round)
        val centro = Offset(w / 2f, h / 2f)

        when (tema) {
            TemaMateria.MATEMATICAS -> {
                drawLine(color, Offset(w * 0.25f, h * 0.18f), Offset(w * 0.72f, h * 0.18f), linea, StrokeCap.Round)
                drawLine(color, Offset(w * 0.25f, h * 0.18f), Offset(w * 0.55f, h * 0.50f), linea, StrokeCap.Round)
                drawLine(color, Offset(w * 0.55f, h * 0.50f), Offset(w * 0.25f, h * 0.82f), linea, StrokeCap.Round)
                drawLine(color, Offset(w * 0.25f, h * 0.82f), Offset(w * 0.72f, h * 0.82f), linea, StrokeCap.Round)
            }
            TemaMateria.TECNOLOGIA -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.10f, h * 0.16f),
                    size = Size(w * 0.80f, h * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.39f, h * 0.35f), Offset(w * 0.27f, h * 0.50f), linea * 0.75f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.27f, h * 0.50f), Offset(w * 0.39f, h * 0.65f), linea * 0.75f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.61f, h * 0.35f), Offset(w * 0.73f, h * 0.50f), linea * 0.75f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.73f, h * 0.50f), Offset(w * 0.61f, h * 0.65f), linea * 0.75f, StrokeCap.Round)
            }
            TemaMateria.FISICA -> {
                drawCircle(color, radius = w * 0.075f, center = centro)
                drawOval(color, Offset(w * 0.08f, h * 0.40f), Size(w * 0.84f, h * 0.20f), style = stroke)
                rotate(60f, centro) { drawOval(color, Offset(w * 0.08f, h * 0.40f), Size(w * 0.84f, h * 0.20f), style = stroke) }
                rotate(-60f, centro) { drawOval(color, Offset(w * 0.08f, h * 0.40f), Size(w * 0.84f, h * 0.20f), style = stroke) }
            }
            TemaMateria.QUIMICA -> {
                drawLine(color, Offset(w * 0.43f, h * 0.12f), Offset(w * 0.43f, h * 0.42f), linea, StrokeCap.Round)
                drawLine(color, Offset(w * 0.57f, h * 0.12f), Offset(w * 0.57f, h * 0.42f), linea, StrokeCap.Round)
                val matraz = Path().apply {
                    moveTo(w * 0.43f, h * 0.40f)
                    lineTo(w * 0.18f, h * 0.83f)
                    lineTo(w * 0.82f, h * 0.83f)
                    lineTo(w * 0.57f, h * 0.40f)
                }
                drawPath(matraz, color, style = stroke)
                drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.39f, h * 0.68f))
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.60f, h * 0.72f))
            }
            TemaMateria.IDIOMAS -> {
                drawCircle(color, radius = w * 0.38f, center = centro, style = stroke)
                drawOval(color, Offset(w * 0.32f, h * 0.12f), Size(w * 0.36f, h * 0.76f), style = stroke)
                drawLine(color, Offset(w * 0.13f, h * 0.50f), Offset(w * 0.87f, h * 0.50f), linea * 0.78f, StrokeCap.Round)
            }
            TemaMateria.NEGOCIOS -> {
                drawRoundRect(color, Offset(w * 0.12f, h * 0.55f), Size(w * 0.16f, h * 0.30f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
                drawRoundRect(color, Offset(w * 0.40f, h * 0.35f), Size(w * 0.16f, h * 0.50f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
                drawRoundRect(color, Offset(w * 0.68f, h * 0.16f), Size(w * 0.16f, h * 0.69f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
            }
            TemaMateria.COMUNICACION -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.10f, h * 0.15f),
                    size = Size(w * 0.80f, h * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.38f, h * 0.73f), Offset(w * 0.29f, h * 0.88f), linea, StrokeCap.Round)
                drawLine(color, Offset(w * 0.28f, h * 0.38f), Offset(w * 0.72f, h * 0.38f), linea * 0.72f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.28f, h * 0.55f), Offset(w * 0.58f, h * 0.55f), linea * 0.72f, StrokeCap.Round)
            }
            TemaMateria.CREATIVIDAD -> {
                drawOval(color, Offset(w * 0.16f, h * 0.20f), Size(w * 0.68f, h * 0.60f), style = stroke)
                drawCircle(color, radius = w * 0.055f, center = Offset(w * 0.35f, h * 0.44f))
                drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.53f, h * 0.35f))
                drawCircle(color, radius = w * 0.043f, center = Offset(w * 0.66f, h * 0.51f))
            }
            TemaMateria.GENERAL -> {
                val libro = Path().apply {
                    moveTo(w * 0.50f, h * 0.19f)
                    lineTo(w * 0.16f, h * 0.29f)
                    lineTo(w * 0.16f, h * 0.72f)
                    lineTo(w * 0.50f, h * 0.82f)
                    lineTo(w * 0.84f, h * 0.72f)
                    lineTo(w * 0.84f, h * 0.29f)
                    close()
                }
                drawPath(libro, color, style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.20f), Offset(w * 0.50f, h * 0.81f), linea * 0.78f, StrokeCap.Round)
            }
        }
    }
}
