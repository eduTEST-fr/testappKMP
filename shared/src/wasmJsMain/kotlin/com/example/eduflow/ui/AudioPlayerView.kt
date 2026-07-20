package com.example.eduflow.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun AudioPlayerWidget(
    audioUrl: String,
    token: String,
    onCompleted: () -> Unit
) {
    Text("El reproductor completo está disponible en la aplicación Android.")
}
