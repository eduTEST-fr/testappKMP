package com.example.eduflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

// Categorías temáticas para ilustrar la portada de cada materia. Se elige
// una segun palabras clave del nombre -- sin llamar a ningun servicio
// externo, todo se dibuja localmente con Canvas.
private enum class TemaMateria(val fondo: Color, val acento: Color) {
    MATEMATICAS(Color(0xFF1565C0), Color.White),
    PROGRAMACION(Color(0xFF2D5A3D), Color.White),
    FISICA(Color(0xFF6A1B9A), Color.White),
    QUIMICA(Color(0xFFAD1457), Color.White),
    IDIOMAS(Color(0xFF8B6914), Color.White),
    NEGOCIOS(Color(0xFF455A64), Color.White),
    ARTE(Color(0xFFD84315), Color.White),
    GENERAL(Color(0xFF2D5A3D), Color.White),
}

private fun detectarTema(nombre: String): TemaMateria {
    val n = nombre.lowercase()
    return when {
        listOf("mat", "calc", "alg", "geometr", "estadist", "trigon").any { n.contains(it) } -> TemaMateria.MATEMATICAS
        listOf("prog", "cod", "soft", "dato", "algoritmo", "web", "app", "sistema").any { n.contains(it) } -> TemaMateria.PROGRAMACION
        listOf("fis", "mecan", "electric", "ondas").any { n.contains(it) } -> TemaMateria.FISICA
        listOf("quim", "organic", "molecul").any { n.contains(it) } -> TemaMateria.QUIMICA
        listOf("ingles", "frances", "idioma", "lengua").any { n.contains(it) } -> TemaMateria.IDIOMAS
        listOf("admin", "negocio", "contab", "finanz", "economi", "mercado").any { n.contains(it) } -> TemaMateria.NEGOCIOS
        listOf("arte", "diseñ", "music", "dibujo").any { n.contains(it) } -> TemaMateria.ARTE
        else -> TemaMateria.GENERAL
    }
}

// Dibuja un icono simple representativo del tema (sigma, código, atomo,
// matraz, globo, grafica de barras, paleta) sobre un fondo de color.
@Composable
fun MateriaIlustracion(nombre: String, modifier: Modifier = Modifier) {
    val tema = detectarTema(nombre)
    androidx.compose.foundation.layout.Box(
        modifier = modifier.background(tema.fondo)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = w * 0.025f)
            when (tema) {
                TemaMateria.MATEMATICAS -> {
                    // Simbolo de sumatoria estilizado: dos lineas en angulo
                    val p = Offset(w * 0.32f, h * 0.30f)
                    drawLine(tema.acento, p, Offset(w * 0.62f, h * 0.30f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.32f, h * 0.30f), Offset(w * 0.50f, h * 0.50f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.32f, h * 0.70f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.32f, h * 0.70f), Offset(w * 0.62f, h * 0.70f), strokeWidth = stroke.width)
                }
                TemaMateria.PROGRAMACION -> {
                    // Simbolo < >
                    drawLine(tema.acento, Offset(w * 0.42f, h * 0.32f), Offset(w * 0.30f, h * 0.50f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.42f, h * 0.68f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.58f, h * 0.32f), Offset(w * 0.70f, h * 0.50f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.70f, h * 0.50f), Offset(w * 0.58f, h * 0.68f), strokeWidth = stroke.width)
                }
                TemaMateria.FISICA -> {
                    // Atomo: nucleo + dos orbitas elipticas
                    drawCircle(tema.acento, radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.5f))
                    drawOval(tema.acento, topLeft = Offset(w * 0.20f, h * 0.42f), size = Size(w * 0.60f, h * 0.16f), style = stroke)
                    rotate(degrees = 60f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                        drawOval(tema.acento, topLeft = Offset(w * 0.20f, h * 0.42f), size = Size(w * 0.60f, h * 0.16f), style = stroke)
                    }
                    rotate(degrees = -60f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                        drawOval(tema.acento, topLeft = Offset(w * 0.20f, h * 0.42f), size = Size(w * 0.60f, h * 0.16f), style = stroke)
                    }
                }
                TemaMateria.QUIMICA -> {
                    // Matraz: triangulo + cuello
                    drawLine(tema.acento, Offset(w * 0.46f, h * 0.28f), Offset(w * 0.46f, h * 0.45f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.54f, h * 0.28f), Offset(w * 0.54f, h * 0.45f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.46f, h * 0.45f), Offset(w * 0.30f, h * 0.70f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.54f, h * 0.45f), Offset(w * 0.70f, h * 0.70f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.30f, h * 0.70f), Offset(w * 0.70f, h * 0.70f), strokeWidth = stroke.width)
                    drawCircle(tema.acento, radius = w * 0.025f, center = Offset(w * 0.43f, h * 0.62f))
                    drawCircle(tema.acento, radius = w * 0.02f, center = Offset(w * 0.55f, h * 0.65f))
                }
                TemaMateria.IDIOMAS -> {
                    // Globo terraqueo: circulo + meridianos
                    drawCircle(tema.acento, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                    drawOval(tema.acento, topLeft = Offset(w * 0.30f, h * 0.30f), size = Size(w * 0.40f, h * 0.40f), style = stroke)
                    drawLine(tema.acento, Offset(w * 0.30f, h * 0.5f), Offset(w * 0.70f, h * 0.5f), strokeWidth = stroke.width)
                }
                TemaMateria.NEGOCIOS -> {
                    // Grafica de barras ascendente
                    drawRect(tema.acento, topLeft = Offset(w * 0.30f, h * 0.58f), size = Size(w * 0.10f, h * 0.16f))
                    drawRect(tema.acento, topLeft = Offset(w * 0.44f, h * 0.46f), size = Size(w * 0.10f, h * 0.28f))
                    drawRect(tema.acento, topLeft = Offset(w * 0.58f, h * 0.34f), size = Size(w * 0.10f, h * 0.40f))
                }
                TemaMateria.ARTE -> {
                    // Paleta: ovalo con un circulo hueco (pulgar)
                    drawOval(tema.acento, topLeft = Offset(w * 0.28f, h * 0.36f), size = Size(w * 0.44f, h * 0.34f), style = stroke)
                    drawCircle(tema.acento, radius = w * 0.05f, center = Offset(w * 0.40f, h * 0.55f))
                }
                TemaMateria.GENERAL -> {
                    // Libro abierto simple
                    drawLine(tema.acento, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.68f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.28f, h * 0.38f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.28f, h * 0.38f), Offset(w * 0.28f, h * 0.62f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.28f, h * 0.62f), Offset(w * 0.5f, h * 0.68f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.72f, h * 0.38f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.72f, h * 0.38f), Offset(w * 0.72f, h * 0.62f), strokeWidth = stroke.width)
                    drawLine(tema.acento, Offset(w * 0.72f, h * 0.62f), Offset(w * 0.5f, h * 0.68f), strokeWidth = stroke.width)
                }
            }
        }
    }
}
