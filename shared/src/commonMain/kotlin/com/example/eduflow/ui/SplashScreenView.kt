// ui/SplashScreenView.kt
package com.example.eduflow.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eduflow.shared.generated.resources.Res
import eduflow.shared.generated.resources.eduflow_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

// Splash inicial, fiel al mockup: fondo beige de marca, logo centrado,
// barra de progreso animada y leyenda corta. Puramente visual: al
// terminar la animacion llama a onFinish() para continuar al flujo real.
@Composable
fun SplashScreenView(onFinish: () -> Unit) {
    val progreso = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progreso.animateTo(1f, animationSpec = tween(1400, easing = LinearOutSlowInEasing))
        delay(200)
        onFinish()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Beige),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.eduflow_logo),
                contentDescription = "EduFlow",
                modifier = Modifier.width(220.dp)
            )
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFDDE8E0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progreso.value)
                        .clip(RoundedCornerShape(3.dp))
                        .background(VerdePrimario)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Estudio impulsado por IA",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextoSecundario
            )
        }
    }
}
