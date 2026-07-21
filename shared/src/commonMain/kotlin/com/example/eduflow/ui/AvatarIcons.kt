package com.example.eduflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Avatares académicos propios de EduFlow, dibujados completamente con Canvas. */
data class AvatarOpcion(
    val id: String,
    val nombre: String,
    val fondo: Color,
    val especie: EspecieAvatar
)

enum class EspecieAvatar {
    BUHO,
    ZORRO,
    GATO,
    CONEJO,
    PANDA,
    MAPACHE,
    ASESOR_LIC,
    ASESOR_MAE,
    ASESOR_DOC
}

/**
 * Los ids anteriores se conservan para no romper perfiles ya almacenados
 * en MySQL o en el almacenamiento local de la aplicación.
 */
val avataresDisponibles = listOf(
    AvatarOpcion("student_buho", "Búho lector", Color(0xFF315C46), EspecieAvatar.BUHO),
    AvatarOpcion("student_zorro", "Zorro matemático", Color(0xFFB7653B), EspecieAvatar.ZORRO),
    AvatarOpcion("student_gato", "Gato investigador", Color(0xFF4E6887), EspecieAvatar.GATO),
    AvatarOpcion("student_conejo", "Conejo de apuntes", Color(0xFF80648A), EspecieAvatar.CONEJO),
    AvatarOpcion("student_oso", "Panda científico", Color(0xFF4F6D68), EspecieAvatar.PANDA),
    AvatarOpcion("student_tortuga", "Mapache graduado", Color(0xFF665D73), EspecieAvatar.MAPACHE)
)

private val avataresAsesor = listOf(
    AvatarOpcion("advisor_lic", "Asesor de licenciatura", Color(0xFF3B7352), EspecieAvatar.ASESOR_LIC),
    AvatarOpcion("advisor_mae", "Asesor de maestría", Color(0xFF415F80), EspecieAvatar.ASESOR_MAE),
    AvatarOpcion("advisor_doc", "Asesor de doctorado", Color(0xFF5C496F), EspecieAvatar.ASESOR_DOC)
)

fun avatarAsesorPorGrado(grado: String): String = when (grado.uppercase()) {
    "DOCTORADO" -> "advisor_doc"
    "MAESTRIA", "MAESTRÍA" -> "advisor_mae"
    else -> "advisor_lic"
}

private fun normalizarAvatarId(id: String): String = when (id) {
    "avatar_1" -> "student_buho"
    "avatar_2" -> "student_zorro"
    "avatar_3" -> "student_gato"
    "avatar_4" -> "student_conejo"
    "avatar_5" -> "student_oso"
    "avatar_6" -> "student_tortuga"
    else -> id
}

private fun opcionAvatar(id: String): AvatarOpcion {
    val normalizado = normalizarAvatarId(id)
    return (avataresDisponibles + avataresAsesor).find { it.id == normalizado }
        ?: avataresDisponibles.first()
}

private fun DrawScope.triangulo(a: Offset, b: Offset, c: Offset, color: Color) {
    drawPath(
        path = Path().apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            close()
        },
        color = color
    )
}

private fun DrawScope.libroAbierto(
    baseY: Float,
    color: Color = Color(0xFFFFE9A8),
    tinta: Color = Color(0xFF31413A)
) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = color,
        topLeft = Offset(w * .18f, baseY),
        size = Size(w * .30f, h * .17f),
        cornerRadius = CornerRadius(w * .035f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * .52f, baseY),
        size = Size(w * .30f, h * .17f),
        cornerRadius = CornerRadius(w * .035f)
    )
    drawLine(
        color = tinta.copy(alpha = .42f),
        start = Offset(w * .50f, baseY + h * .015f),
        end = Offset(w * .50f, baseY + h * .16f),
        strokeWidth = w * .015f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = tinta.copy(alpha = .28f),
        start = Offset(w * .24f, baseY + h * .07f),
        end = Offset(w * .42f, baseY + h * .07f),
        strokeWidth = w * .010f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = tinta.copy(alpha = .28f),
        start = Offset(w * .58f, baseY + h * .07f),
        end = Offset(w * .76f, baseY + h * .07f),
        strokeWidth = w * .010f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.lentes(
    y: Float,
    color: Color = Color(0xFF26362F),
    escala: Float = 1f
) {
    val w = size.width
    val r = w * .10f * escala
    val stroke = w * .025f * escala
    drawCircle(
        color = color,
        radius = r,
        center = Offset(w * .38f, y),
        style = Stroke(stroke)
    )
    drawCircle(
        color = color,
        radius = r,
        center = Offset(w * .62f, y),
        style = Stroke(stroke)
    )
    drawLine(
        color = color,
        start = Offset(w * .48f, y),
        end = Offset(w * .52f, y),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.birrete(y: Float, color: Color = Color(0xFF1F3028)) {
    val w = size.width
    val h = size.height
    val board = Path().apply {
        moveTo(w * .18f, y)
        lineTo(w * .50f, y - h * .12f)
        lineTo(w * .82f, y)
        lineTo(w * .50f, y + h * .12f)
        close()
    }
    drawPath(board, color)
    drawRoundRect(
        color = color,
        topLeft = Offset(w * .35f, y + h * .06f),
        size = Size(w * .30f, h * .08f),
        cornerRadius = CornerRadius(w * .025f)
    )
    drawLine(
        color = color,
        start = Offset(w * .72f, y),
        end = Offset(w * .79f, y + h * .20f),
        strokeWidth = w * .023f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color(0xFFE3BB4E),
        radius = w * .027f,
        center = Offset(w * .79f, y + h * .21f)
    )
}

private fun DrawScope.lapiz(
    start: Offset,
    end: Offset,
    color: Color = Color(0xFFF1C84B)
) {
    val w = size.width
    drawLine(color, start, end, w * .055f, StrokeCap.Round)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val ux = dx / length
    val uy = dy / length
    val px = -uy
    val py = ux
    val tipBase = Offset(end.x - ux * w * .09f, end.y - uy * w * .09f)
    triangulo(
        Offset(tipBase.x + px * w * .045f, tipBase.y + py * w * .045f),
        end,
        Offset(tipBase.x - px * w * .045f, tipBase.y - py * w * .045f),
        Color(0xFFF4D5B3)
    )
    drawCircle(Color(0xFF2B3933), w * .012f, end)
}

private fun DrawScope.cuaderno(baseY: Float, color: Color = Color(0xFFF6F0DF)) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = color,
        topLeft = Offset(w * .29f, baseY),
        size = Size(w * .42f, h * .19f),
        cornerRadius = CornerRadius(w * .035f)
    )
    drawRect(
        color = Color(0xFFDBA743),
        topLeft = Offset(w * .29f, baseY),
        size = Size(w * .06f, h * .19f)
    )
    repeat(2) { index ->
        val y = baseY + h * (.065f + index * .055f)
        drawLine(
            color = Color(0xFF5A6B63).copy(alpha = .35f),
            start = Offset(w * .40f, y),
            end = Offset(w * .64f, y),
            strokeWidth = w * .010f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.matraz(baseY: Float) {
    val w = size.width
    val h = size.height
    drawLine(
        color = Color(0xFFF8FBF8),
        start = Offset(w * .45f, baseY),
        end = Offset(w * .45f, baseY + h * .07f),
        strokeWidth = w * .028f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFF8FBF8),
        start = Offset(w * .55f, baseY),
        end = Offset(w * .55f, baseY + h * .07f),
        strokeWidth = w * .028f,
        cap = StrokeCap.Round
    )
    val body = Path().apply {
        moveTo(w * .45f, baseY + h * .06f)
        lineTo(w * .34f, baseY + h * .20f)
        quadraticBezierTo(w * .31f, baseY + h * .27f, w * .40f, baseY + h * .29f)
        lineTo(w * .60f, baseY + h * .29f)
        quadraticBezierTo(w * .69f, baseY + h * .27f, w * .66f, baseY + h * .20f)
        lineTo(w * .55f, baseY + h * .06f)
        close()
    }
    drawPath(body, Color(0xFFF8FBF8))
    val liquid = Path().apply {
        moveTo(w * .36f, baseY + h * .22f)
        lineTo(w * .64f, baseY + h * .22f)
        quadraticBezierTo(w * .67f, baseY + h * .27f, w * .59f, baseY + h * .275f)
        lineTo(w * .41f, baseY + h * .275f)
        quadraticBezierTo(w * .33f, baseY + h * .27f, w * .36f, baseY + h * .22f)
        close()
    }
    drawPath(liquid, Color(0xFF78C7B0))
}

private fun DrawScope.diploma(baseY: Float) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = Color(0xFFF7E7B9),
        topLeft = Offset(w * .28f, baseY),
        size = Size(w * .44f, h * .14f),
        cornerRadius = CornerRadius(w * .07f)
    )
    drawLine(
        color = Color(0xFFC34E43),
        start = Offset(w * .48f, baseY + h * .02f),
        end = Offset(w * .48f, baseY + h * .12f),
        strokeWidth = w * .035f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFC34E43),
        start = Offset(w * .52f, baseY + h * .02f),
        end = Offset(w * .52f, baseY + h * .12f),
        strokeWidth = w * .035f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.dibujarBuho(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val crema = Color(0xFFFFF6E6)
    val tinta = Color(0xFF26362F)
    val ala = Color(0xFFC9A66A)

    triangulo(Offset(w * .20f, h * .31f), Offset(w * .31f, h * .08f), Offset(w * .43f, h * .31f), crema)
    triangulo(Offset(w * .57f, h * .31f), Offset(w * .69f, h * .08f), Offset(w * .80f, h * .31f), crema)
    drawOval(crema, Offset(w * .17f, h * .20f), Size(w * .66f, h * .58f))
    drawOval(ala, Offset(w * .15f, h * .43f), Size(w * .19f, h * .27f))
    drawOval(ala, Offset(w * .66f, h * .43f), Size(w * .19f, h * .27f))
    drawCircle(Color.White, w * .15f, Offset(w * .37f, h * .42f))
    drawCircle(Color.White, w * .15f, Offset(w * .63f, h * .42f))
    drawCircle(tinta, w * .045f, Offset(w * .38f, h * .42f))
    drawCircle(tinta, w * .045f, Offset(w * .62f, h * .42f))
    lentes(h * .42f, tinta, .93f)
    triangulo(Offset(w * .45f, h * .54f), Offset(w * .55f, h * .54f), Offset(w * .50f, h * .63f), Color(0xFFE2A343))
    libroAbierto(h * .70f)
}

private fun DrawScope.dibujarZorro(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val crema = Color(0xFFFFF5E9)
    val tinta = Color(0xFF28352F)
    val naranja = Color(0xFFEE9B5B)

    triangulo(Offset(w * .14f, h * .34f), Offset(w * .24f, h * .07f), Offset(w * .43f, h * .30f), naranja)
    triangulo(Offset(w * .57f, h * .30f), Offset(w * .76f, h * .07f), Offset(w * .86f, h * .34f), naranja)
    triangulo(Offset(w * .20f, h * .30f), Offset(w * .25f, h * .13f), Offset(w * .37f, h * .29f), crema)
    triangulo(Offset(w * .63f, h * .29f), Offset(w * .75f, h * .13f), Offset(w * .80f, h * .30f), crema)
    drawOval(naranja, Offset(w * .17f, h * .19f), Size(w * .66f, h * .60f))
    triangulo(Offset(w * .19f, h * .31f), Offset(w * .50f, h * .72f), Offset(w * .81f, h * .31f), crema)
    drawCircle(tinta, w * .038f, Offset(w * .38f, h * .43f))
    drawCircle(tinta, w * .038f, Offset(w * .62f, h * .43f))
    drawCircle(tinta, w * .052f, Offset(w * .50f, h * .60f))
    drawRoundRect(
        color = Color(0xFFECF0E8),
        topLeft = Offset(w * .27f, h * .70f),
        size = Size(w * .46f, h * .15f),
        cornerRadius = CornerRadius(w * .035f)
    )
    drawLine(Color(0xFF59675F).copy(.35f), Offset(w * .36f, h * .75f), Offset(w * .64f, h * .75f), w * .010f, StrokeCap.Round)
    drawLine(Color(0xFF59675F).copy(.35f), Offset(w * .36f, h * .80f), Offset(w * .58f, h * .80f), w * .010f, StrokeCap.Round)
    lapiz(Offset(w * .28f, h * .84f), Offset(w * .72f, h * .70f))
}

private fun DrawScope.dibujarGato(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val gris = Color(0xFFE9EDF1)
    val tinta = Color(0xFF26343C)
    val interior = Color(0xFFE7B6B7)

    triangulo(Offset(w * .16f, h * .34f), Offset(w * .25f, h * .06f), Offset(w * .43f, h * .29f), gris)
    triangulo(Offset(w * .57f, h * .29f), Offset(w * .75f, h * .06f), Offset(w * .84f, h * .34f), gris)
    triangulo(Offset(w * .21f, h * .29f), Offset(w * .25f, h * .13f), Offset(w * .36f, h * .29f), interior)
    triangulo(Offset(w * .64f, h * .29f), Offset(w * .75f, h * .13f), Offset(w * .79f, h * .29f), interior)
    drawRoundRect(
        color = gris,
        topLeft = Offset(w * .17f, h * .20f),
        size = Size(w * .66f, h * .59f),
        cornerRadius = CornerRadius(w * .25f)
    )
    drawCircle(tinta, w * .035f, Offset(w * .38f, h * .43f))
    drawCircle(tinta, w * .035f, Offset(w * .62f, h * .43f))
    lentes(h * .43f, Color(0xFF314B63), .95f)
    triangulo(Offset(w * .45f, h * .55f), Offset(w * .55f, h * .55f), Offset(w * .50f, h * .62f), Color(0xFFD2888C))
    drawLine(tinta.copy(.45f), Offset(w * .20f, h * .57f), Offset(w * .41f, h * .59f), w * .010f)
    drawLine(tinta.copy(.45f), Offset(w * .59f, h * .59f), Offset(w * .80f, h * .57f), w * .010f)
    cuaderno(h * .70f)
}

private fun DrawScope.dibujarConejo(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val blanco = Color(0xFFFFF8F2)
    val rosa = Color(0xFFE8B3BE)
    val tinta = Color(0xFF304039)

    drawOval(blanco, Offset(w * .21f, h * .01f), Size(w * .20f, h * .42f))
    drawOval(blanco, Offset(w * .59f, h * .01f), Size(w * .20f, h * .42f))
    drawOval(rosa, Offset(w * .275f, h * .07f), Size(w * .07f, h * .27f))
    drawOval(rosa, Offset(w * .655f, h * .07f), Size(w * .07f, h * .27f))
    drawOval(blanco, Offset(w * .16f, h * .22f), Size(w * .68f, h * .58f))
    drawCircle(tinta, w * .037f, Offset(w * .38f, h * .44f))
    drawCircle(tinta, w * .037f, Offset(w * .62f, h * .44f))
    drawCircle(rosa, w * .043f, Offset(w * .50f, h * .56f))
    drawLine(tinta, Offset(w * .50f, h * .61f), Offset(w * .44f, h * .66f), w * .014f, StrokeCap.Round)
    drawLine(tinta, Offset(w * .50f, h * .61f), Offset(w * .56f, h * .66f), w * .014f, StrokeCap.Round)
    drawRoundRect(
        color = Color(0xFFF4E7B5),
        topLeft = Offset(w * .26f, h * .70f),
        size = Size(w * .48f, h * .16f),
        cornerRadius = CornerRadius(w * .035f)
    )
    drawLine(Color(0xFF6D6A5C).copy(.38f), Offset(w * .35f, h * .76f), Offset(w * .64f, h * .76f), w * .010f, StrokeCap.Round)
    lapiz(Offset(w * .31f, h * .85f), Offset(w * .70f, h * .70f), Color(0xFF70A4C6))
}

private fun DrawScope.dibujarPanda(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val blanco = Color(0xFFFFFBF4)
    val negro = Color(0xFF26312D)

    drawCircle(negro, w * .14f, Offset(w * .27f, h * .25f))
    drawCircle(negro, w * .14f, Offset(w * .73f, h * .25f))
    drawCircle(blanco, w * .32f, Offset(w * .50f, h * .47f))
    drawOval(negro, Offset(w * .27f, h * .32f), Size(w * .20f, h * .22f))
    drawOval(negro, Offset(w * .53f, h * .32f), Size(w * .20f, h * .22f))
    drawCircle(Color.White, w * .035f, Offset(w * .38f, h * .43f))
    drawCircle(Color.White, w * .035f, Offset(w * .62f, h * .43f))
    drawOval(Color(0xFFE9D7C8), Offset(w * .38f, h * .49f), Size(w * .24f, h * .17f))
    drawCircle(negro, w * .042f, Offset(w * .50f, h * .55f))
    drawRoundRect(
        color = Color.White.copy(alpha = .94f),
        topLeft = Offset(w * .24f, h * .66f),
        size = Size(w * .52f, h * .20f),
        cornerRadius = CornerRadius(w * .08f)
    )
    drawLine(Color(0xFF6E9C8E), Offset(w * .50f, h * .67f), Offset(w * .50f, h * .84f), w * .015f)
    matraz(h * .65f)
}

private fun DrawScope.dibujarMapache(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val gris = Color(0xFFE3E4E5)
    val oscuro = Color(0xFF38423F)
    val blanco = Color(0xFFFFFBF4)

    triangulo(Offset(w * .17f, h * .34f), Offset(w * .25f, h * .09f), Offset(w * .43f, h * .30f), gris)
    triangulo(Offset(w * .57f, h * .30f), Offset(w * .75f, h * .09f), Offset(w * .83f, h * .34f), gris)
    drawOval(gris, Offset(w * .16f, h * .20f), Size(w * .68f, h * .59f))
    val mascara = Path().apply {
        moveTo(w * .22f, h * .37f)
        quadraticBezierTo(w * .36f, h * .26f, w * .50f, h * .37f)
        quadraticBezierTo(w * .64f, h * .26f, w * .78f, h * .37f)
        lineTo(w * .69f, h * .54f)
        quadraticBezierTo(w * .60f, h * .60f, w * .50f, h * .52f)
        quadraticBezierTo(w * .40f, h * .60f, w * .31f, h * .54f)
        close()
    }
    drawPath(mascara, oscuro)
    drawCircle(blanco, w * .045f, Offset(w * .38f, h * .43f))
    drawCircle(blanco, w * .045f, Offset(w * .62f, h * .43f))
    drawCircle(oscuro, w * .047f, Offset(w * .50f, h * .59f))
    birrete(h * .20f, Color(0xFF20352C))
    diploma(h * .70f)
}

/** Mantiene el diseño de los avatares de asesor que ya había sido aprobado. */
private fun DrawScope.dibujarAsesor(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val blanco = Color(0xFFFFFBF4)
    val tinta = Color(0xFF25342D)

    when (opcion.especie) {
        EspecieAvatar.ASESOR_MAE -> {
            triangulo(Offset(w * .20f, h * .32f), Offset(w * .33f, h * .08f), Offset(w * .44f, h * .34f), blanco)
            triangulo(Offset(w * .56f, h * .34f), Offset(w * .67f, h * .08f), Offset(w * .80f, h * .32f), blanco)
            drawOval(blanco, Offset(w * .18f, h * .20f), Size(w * .64f, h * .64f))
            drawCircle(opcion.fondo.copy(alpha = .35f), w * .17f, Offset(w * .37f, h * .43f))
            drawCircle(opcion.fondo.copy(alpha = .35f), w * .17f, Offset(w * .63f, h * .43f))
            drawCircle(tinta, w * .045f, Offset(w * .38f, h * .43f))
            drawCircle(tinta, w * .045f, Offset(w * .62f, h * .43f))
            triangulo(Offset(w * .45f, h * .53f), Offset(w * .55f, h * .53f), Offset(w * .50f, h * .63f), Color(0xFFE0A64B))
            lentes(h * .43f, Color.White)
            libroAbierto(h * .68f)
        }
        EspecieAvatar.ASESOR_LIC -> {
            triangulo(Offset(w * .16f, h * .34f), Offset(w * .25f, h * .06f), Offset(w * .43f, h * .30f), blanco)
            triangulo(Offset(w * .57f, h * .30f), Offset(w * .75f, h * .06f), Offset(w * .84f, h * .34f), blanco)
            drawOval(blanco, Offset(w * .18f, h * .20f), Size(w * .64f, h * .60f))
            triangulo(Offset(w * .20f, h * .25f), Offset(w * .50f, h * .72f), Offset(w * .80f, h * .25f), Color(0xFFFFE6D6))
            drawCircle(tinta, w * .04f, Offset(w * .38f, h * .42f))
            drawCircle(tinta, w * .04f, Offset(w * .62f, h * .42f))
            drawCircle(tinta, w * .055f, Offset(w * .50f, h * .61f))
            libroAbierto(h * .70f)
        }
        EspecieAvatar.ASESOR_DOC -> {
            drawCircle(blanco, w * .14f, Offset(w * .27f, h * .26f))
            drawCircle(blanco, w * .14f, Offset(w * .73f, h * .26f))
            drawCircle(blanco, w * .32f, Offset(w * .50f, h * .48f))
            drawOval(Color(0xFFF0D7C0), Offset(w * .34f, h * .48f), Size(w * .32f, h * .22f))
            drawCircle(tinta, w * .04f, Offset(w * .39f, h * .42f))
            drawCircle(tinta, w * .04f, Offset(w * .61f, h * .42f))
            drawCircle(tinta, w * .05f, Offset(w * .50f, h * .57f))
            birrete(h * .20f)
            libroAbierto(h * .70f)
        }
        else -> Unit
    }
}

private fun DrawScope.dibujarAvatar(opcion: AvatarOpcion) {
    when (opcion.especie) {
        EspecieAvatar.BUHO -> dibujarBuho(opcion)
        EspecieAvatar.ZORRO -> dibujarZorro(opcion)
        EspecieAvatar.GATO -> dibujarGato(opcion)
        EspecieAvatar.CONEJO -> dibujarConejo(opcion)
        EspecieAvatar.PANDA -> dibujarPanda(opcion)
        EspecieAvatar.MAPACHE -> dibujarMapache(opcion)
        EspecieAvatar.ASESOR_LIC,
        EspecieAvatar.ASESOR_MAE,
        EspecieAvatar.ASESOR_DOC -> dibujarAsesor(opcion)
    }
}

@Composable
fun AvatarIcono(avatarId: String, sizeDp: Int = 64, modifier: Modifier = Modifier) {
    val opcion = opcionAvatar(avatarId)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(opcion.fondo),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding((sizeDp * .075f).dp)) {
            dibujarAvatar(opcion)
        }
    }
}

@Composable
fun SelectorAvatar(avatarSeleccionado: String, onSeleccionar: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        avataresDisponibles.chunked(3).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fila.forEach { opcion ->
                    val seleccionado = normalizarAvatarId(avatarSeleccionado) == opcion.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSeleccionar(opcion.id) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .then(
                                    if (seleccionado) {
                                        Modifier.border(3.dp, VerdePrimario, CircleShape)
                                    } else {
                                        Modifier.border(1.dp, Color(0xFFE0DDD5), CircleShape)
                                    }
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AvatarIcono(opcion.id, 58)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = opcion.nombre,
                            fontSize = 10.sp,
                            color = if (seleccionado) VerdePrimario else TextoSecundario,
                            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectorAvatarDialog(
    avatarSeleccionado: String,
    onSeleccionar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    "Elige tu avatar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrimario
                )
                Text(
                    "Se mostrará en tu perfil, el menú y la comunidad.",
                    fontSize = 12.sp,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )
                SelectorAvatar(avatarSeleccionado) {
                    onSeleccionar(it)
                    onCerrar()
                }
            }
        }
    }
}
