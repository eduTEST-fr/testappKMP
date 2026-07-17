package com.example.eduflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun HourglassIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = (w * 0.10f).coerceAtLeast(2f)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(w * .18f, h * .10f),
            end = androidx.compose.ui.geometry.Offset(w * .82f, h * .10f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(w * .18f, h * .90f),
            end = androidx.compose.ui.geometry.Offset(w * .82f, h * .90f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        val outline = Path().apply {
            moveTo(w * .25f, h * .16f)
            cubicTo(w * .28f, h * .38f, w * .44f, h * .42f, w * .50f, h * .50f)
            cubicTo(w * .56f, h * .58f, w * .72f, h * .62f, w * .75f, h * .84f)
            moveTo(w * .75f, h * .16f)
            cubicTo(w * .72f, h * .38f, w * .56f, h * .42f, w * .50f, h * .50f)
            cubicTo(w * .44f, h * .58f, w * .28f, h * .62f, w * .25f, h * .84f)
        }
        drawPath(outline, color, style = Stroke(width = stroke * .7f, cap = StrokeCap.Round))
        val sand = Path().apply {
            moveTo(w * .34f, h * .24f)
            lineTo(w * .66f, h * .24f)
            lineTo(w * .50f, h * .44f)
            close()
            moveTo(w * .50f, h * .58f)
            lineTo(w * .34f, h * .78f)
            lineTo(w * .66f, h * .78f)
            close()
        }
        drawPath(sand, color)
    }
}
