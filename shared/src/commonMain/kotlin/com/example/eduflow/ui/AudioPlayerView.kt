package com.example.eduflow.ui

import androidx.compose.runtime.Composable

/**
 * Reproductor de un episodio puntual.
 *
 * Recibe la ruta relativa devuelta por el backend, por ejemplo
 * /podcasts/audio/7. La implementación real vive en androidMain para usar
 * MediaPlayer; las demás plataformas conservan su implementación propia.
 */
@Composable
expect fun AudioPlayerWidget(
    audioUrl: String,
    token: String,
    onCompleted: () -> Unit = {}
)
