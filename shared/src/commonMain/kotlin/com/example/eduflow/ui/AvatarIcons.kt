package com.example.eduflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Avatares predeterminados de perfil: un icono de persona (silueta simple,
// cabeza + hombros) dibujado a mano sobre un fondo de color. Reemplaza el
// uso de emojis en el perfil sin depender de un icon-pack ni de imágenes
// descargadas -- todo se dibuja con Canvas, igual de liviano que el resto
// de la app.
data class AvatarOpcion(val id: String, val color: Color)

val avataresDisponibles = listOf(
    AvatarOpcion("avatar_1", Color(0xFF2D5A3D)), // verde primario
    AvatarOpcion("avatar_2", Color(0xFF8B6914)), // dorado
    AvatarOpcion("avatar_3", Color(0xFF1565C0)), // azul
    AvatarOpcion("avatar_4", Color(0xFF6A1B9A)), // morado
    AvatarOpcion("avatar_5", Color(0xFFAD1457)), // rosa
    AvatarOpcion("avatar_6", Color(0xFF455A64)), // gris azulado
)

fun colorDeAvatar(avatarId: String): Color =
    avataresDisponibles.find { it.id == avatarId }?.color ?: VerdePrimario

// Dibuja la silueta: un circulo (cabeza) y un arco/elipse recortado (hombros),
// todo en blanco sobre el color de fondo del avatar.
@Composable
fun AvatarIcono(avatarId: String, sizeDp: Int = 64, modifier: Modifier = Modifier) {
    val color = colorDeAvatar(avatarId)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp * 0.62f).dp)) {
            val w = size.width
            val h = size.height
            // Cabeza: circulo en el tercio superior
            drawCircle(
                color = Color.White,
                radius = w * 0.22f,
                center = Offset(w * 0.5f, h * 0.30f)
            )
            // Hombros: medio ovalo en la base, recortado por el propio Box circular
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.08f, h * 0.50f),
                size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.62f)
            )
        }
    }
}

// Selector de avatar: fila de circulos para elegir uno de los predeterminados.
@Composable
fun SelectorAvatar(avatarSeleccionado: String, onSeleccionar: (String) -> Unit) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        avataresDisponibles.forEach { opcion ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .then(
                        if (avatarSeleccionado == opcion.id)
                            Modifier.border(2.5.dp, TextoPrimario, CircleShape)
                        else Modifier
                    )
                    .clickable { onSeleccionar(opcion.id) },
                contentAlignment = Alignment.Center
            ) {
                AvatarIcono(avatarId = opcion.id, sizeDp = 40)
            }
        }
    }
}
