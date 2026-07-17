package com.example.eduflow.ui

import androidx.compose.runtime.Composable

// Widget de reproduccion para UN episodio puntual (recibe la ruta relativa
// que devuelve el backend, ej: /podcasts/audio/7). La logica de biblioteca
// (materias > temas > episodios) vive en AudiosView.kt, que es comun a
// todas las plataformas; solo la reproduccion en si es expect/actual.
@Composable
expect fun AudioPlayerWidget(audioUrl: String, token: String)
