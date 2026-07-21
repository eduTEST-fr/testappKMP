package com.example.eduflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Avatares geométricos propios de EduFlow, dibujados completamente con Canvas. */
data class AvatarOpcion(
    val id: String,
    val nombre: String,
    val fondo: Color,
    val especie: EspecieAvatar
)

enum class EspecieAvatar { BUHO, ZORRO, GATO, CONEJO, OSO, TORTUGA, ASESOR_LIC, ASESOR_MAE, ASESOR_DOC }

val avataresDisponibles = listOf(
    AvatarOpcion("student_buho", "Búho lector", Color(0xFF315C46), EspecieAvatar.BUHO),
    AvatarOpcion("student_zorro", "Zorro aplicado", Color(0xFFB56B3D), EspecieAvatar.ZORRO),
    AvatarOpcion("student_gato", "Gato de biblioteca", Color(0xFF536B87), EspecieAvatar.GATO),
    AvatarOpcion("student_conejo", "Conejo de apuntes", Color(0xFF8B658D), EspecieAvatar.CONEJO),
    AvatarOpcion("student_oso", "Oso investigador", Color(0xFF7A614D), EspecieAvatar.OSO),
    AvatarOpcion("student_tortuga", "Tortuga constante", Color(0xFF3F7B72), EspecieAvatar.TORTUGA)
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
    drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, color)
}

private fun DrawScope.libro(baseY: Float, color: Color = Color(0xFFF7E8B5)) {
    val w = size.width
    val h = size.height
    drawRoundRect(color, Offset(w * .24f, baseY), Size(w * .24f, h * .16f),
        androidx.compose.ui.geometry.CornerRadius(w * .025f))
    drawRoundRect(color, Offset(w * .52f, baseY), Size(w * .24f, h * .16f),
        androidx.compose.ui.geometry.CornerRadius(w * .025f))
    drawLine(Color.White.copy(.7f), Offset(w * .50f, baseY), Offset(w * .50f, baseY + h * .16f), w * .018f)
}

private fun DrawScope.lentes(y: Float, color: Color = Color.White) {
    val w = size.width
    val r = w * .105f
    drawCircle(Color.Transparent, r, Offset(w * .38f, y), style = Stroke(w * .025f))
    drawCircle(Color.Transparent, r, Offset(w * .62f, y), style = Stroke(w * .025f))
    drawCircle(color, r, Offset(w * .38f, y), style = Stroke(w * .025f))
    drawCircle(color, r, Offset(w * .62f, y), style = Stroke(w * .025f))
    drawLine(color, Offset(w * .485f, y), Offset(w * .515f, y), w * .025f, StrokeCap.Round)
}

private fun DrawScope.birrete(y: Float, color: Color = Color(0xFF1E2E27)) {
    val w = size.width
    triangulo(Offset(w * .20f, y), Offset(w * .50f, y - w * .13f), Offset(w * .80f, y), color)
    triangulo(Offset(w * .20f, y), Offset(w * .50f, y + w * .13f), Offset(w * .80f, y), color)
    drawLine(color, Offset(w * .72f, y), Offset(w * .78f, y + w * .19f), w * .025f, StrokeCap.Round)
    drawCircle(Color(0xFFE1B94B), w * .025f, Offset(w * .78f, y + w * .20f))
}

private fun DrawScope.dibujarAvatar(opcion: AvatarOpcion) {
    val w = size.width
    val h = size.height
    val blanco = Color(0xFFFFFBF4)
    val tinta = Color(0xFF25342D)

    when (opcion.especie) {
        EspecieAvatar.BUHO, EspecieAvatar.ASESOR_MAE -> {
            // Silueta redonda y orejas geométricas.
            triangulo(Offset(w * .20f, h * .32f), Offset(w * .33f, h * .08f), Offset(w * .44f, h * .34f), blanco)
            triangulo(Offset(w * .56f, h * .34f), Offset(w * .67f, h * .08f), Offset(w * .80f, h * .32f), blanco)
            drawOval(blanco, Offset(w * .18f, h * .20f), Size(w * .64f, h * .64f))
            drawCircle(opcion.fondo.copy(alpha = .35f), w * .17f, Offset(w * .37f, h * .43f))
            drawCircle(opcion.fondo.copy(alpha = .35f), w * .17f, Offset(w * .63f, h * .43f))
            drawCircle(tinta, w * .045f, Offset(w * .38f, h * .43f))
            drawCircle(tinta, w * .045f, Offset(w * .62f, h * .43f))
            triangulo(Offset(w * .45f, h * .53f), Offset(w * .55f, h * .53f), Offset(w * .50f, h * .63f), Color(0xFFE0A64B))
            if (opcion.especie == EspecieAvatar.ASESOR_MAE) lentes(h * .43f)
            libro(h * .68f)
        }
        EspecieAvatar.ZORRO, EspecieAvatar.ASESOR_LIC -> {
            triangulo(Offset(w * .16f, h * .34f), Offset(w * .25f, h * .06f), Offset(w * .43f, h * .30f), blanco)
            triangulo(Offset(w * .57f, h * .30f), Offset(w * .75f, h * .06f), Offset(w * .84f, h * .34f), blanco)
            drawOval(blanco, Offset(w * .18f, h * .20f), Size(w * .64f, h * .60f))
            triangulo(Offset(w * .20f, h * .25f), Offset(w * .50f, h * .72f), Offset(w * .80f, h * .25f), Color(0xFFFFE6D6))
            drawCircle(tinta, w * .04f, Offset(w * .38f, h * .42f))
            drawCircle(tinta, w * .04f, Offset(w * .62f, h * .42f))
            drawCircle(tinta, w * .055f, Offset(w * .50f, h * .61f))
            libro(h * .70f)
        }
        EspecieAvatar.GATO -> {
            triangulo(Offset(w * .18f, h * .34f), Offset(w * .25f, h * .07f), Offset(w * .43f, h * .28f), blanco)
            triangulo(Offset(w * .57f, h * .28f), Offset(w * .75f, h * .07f), Offset(w * .82f, h * .34f), blanco)
            drawRoundRect(blanco, Offset(w * .18f, h * .20f), Size(w * .64f, h * .60f),
                androidx.compose.ui.geometry.CornerRadius(w * .24f))
            drawCircle(tinta, w * .038f, Offset(w * .38f, h * .43f))
            drawCircle(tinta, w * .038f, Offset(w * .62f, h * .43f))
            triangulo(Offset(w * .45f, h * .54f), Offset(w * .55f, h * .54f), Offset(w * .50f, h * .62f), Color(0xFFD38B89))
            drawLine(tinta.copy(.55f), Offset(w * .22f, h * .56f), Offset(w * .42f, h * .58f), w * .012f)
            drawLine(tinta.copy(.55f), Offset(w * .58f, h * .58f), Offset(w * .78f, h * .56f), w * .012f)
            libro(h * .70f)
        }
        EspecieAvatar.CONEJO -> {
            drawOval(blanco, Offset(w * .25f, h * .02f), Size(w * .18f, h * .40f))
            drawOval(blanco, Offset(w * .57f, h * .02f), Size(w * .18f, h * .40f))
            drawOval(blanco, Offset(w * .18f, h * .22f), Size(w * .64f, h * .58f))
            drawCircle(tinta, w * .04f, Offset(w * .38f, h * .44f))
            drawCircle(tinta, w * .04f, Offset(w * .62f, h * .44f))
            drawCircle(Color(0xFFD98B91), w * .045f, Offset(w * .50f, h * .57f))
            drawLine(tinta, Offset(w * .50f, h * .61f), Offset(w * .43f, h * .66f), w * .015f)
            drawLine(tinta, Offset(w * .50f, h * .61f), Offset(w * .57f, h * .66f), w * .015f)
            libro(h * .70f)
        }
        EspecieAvatar.OSO, EspecieAvatar.ASESOR_DOC -> {
            drawCircle(blanco, w * .14f, Offset(w * .27f, h * .26f))
            drawCircle(blanco, w * .14f, Offset(w * .73f, h * .26f))
            drawCircle(blanco, w * .32f, Offset(w * .50f, h * .48f))
            drawOval(Color(0xFFF0D7C0), Offset(w * .34f, h * .48f), Size(w * .32f, h * .22f))
            drawCircle(tinta, w * .04f, Offset(w * .39f, h * .42f))
            drawCircle(tinta, w * .04f, Offset(w * .61f, h * .42f))
            drawCircle(tinta, w * .05f, Offset(w * .50f, h * .57f))
            if (opcion.especie == EspecieAvatar.ASESOR_DOC) birrete(h * .20f)
            libro(h * .70f)
        }
        EspecieAvatar.TORTUGA -> {
            drawOval(Color(0xFFB7D8C4), Offset(w * .17f, h * .28f), Size(w * .66f, h * .46f))
            drawCircle(blanco, w * .16f, Offset(w * .50f, h * .31f))
            drawCircle(tinta, w * .03f, Offset(w * .44f, h * .29f))
            drawCircle(tinta, w * .03f, Offset(w * .56f, h * .29f))
            drawLine(opcion.fondo.copy(.45f), Offset(w * .50f, h * .46f), Offset(w * .50f, h * .70f), w * .018f)
            drawLine(opcion.fondo.copy(.45f), Offset(w * .28f, h * .52f), Offset(w * .72f, h * .52f), w * .018f)
            libro(h * .70f)
        }
    }
}

@Composable
fun AvatarIcono(avatarId: String, sizeDp: Int = 64, modifier: Modifier = Modifier) {
    val opcion = opcionAvatar(avatarId)
    Box(
        modifier = modifier.size(sizeDp.dp).clip(CircleShape).background(opcion.fondo),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding((sizeDp * .08f).dp)) { dibujarAvatar(opcion) }
    }
}

@Composable
fun SelectorAvatar(avatarSeleccionado: String, onSeleccionar: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        avataresDisponibles.chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { opcion ->
                    val seleccionado = normalizarAvatarId(avatarSeleccionado) == opcion.id
                    Column(
                        modifier = Modifier.weight(1f).clickable { onSeleccionar(opcion.id) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(62.dp).clip(CircleShape).then(
                                if (seleccionado) Modifier.border(3.dp, VerdePrimario, CircleShape)
                                else Modifier.border(1.dp, Color(0xFFE0DDD5), CircleShape)
                            ).padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) { AvatarIcono(opcion.id, 52) }
                        Spacer(Modifier.height(5.dp))
                        Text(opcion.nombre, fontSize = 10.sp, color = TextoSecundario, maxLines = 1)
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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Elige tu compañero de estudio", fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario)
                Text("Todos fueron dibujados para EduFlow y representan una forma distinta de aprender.",
                    fontSize = 12.sp, color = TextoSecundario,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
                SelectorAvatar(avatarSeleccionado) {
                    onSeleccionar(it)
                    onCerrar()
                }
            }
        }
    }
}
