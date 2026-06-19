package com.example.eduflow.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun AudioPlayerView(materia: MateriaUI?, token: String) {
    Text("El reproductor de audio no está disponible en Wasm todavía.")
}
