package com.example.eduflow.sensor

fun diasAnticipacionEstudio(dificultad: Int): Int = when {
    dificultad >= 9 -> 7
    dificultad >= 7 -> 5
    dificultad >= 5 -> 4
    dificultad >= 3 -> 3
    else -> 2
}

/** Meta diaria simple, explicable y compartida por toda la interfaz. */
fun calcularMinutosIdeales(dificultad: Int, diasParaExamen: Long? = null): Int {
    val base = 20 + dificultad.coerceIn(1, 10) * 5
    val urgencia = when {
        diasParaExamen == null -> 0
        diasParaExamen <= 2 -> 15
        diasParaExamen <= 5 -> 10
        else -> 0
    }
    return (base + urgencia).coerceAtMost(90)
}
