package com.example.eduflow.util

import kotlin.math.abs
import kotlin.math.round

/**
 * Da formato a un Double con un solo decimal (ej. 4.8), de forma segura
 * en cualquier plataforma (Android, Web/Wasm, JS). Reemplaza el uso de
 * "%.1f".format(x), que depende de String.format y solo existe en la JVM.
 */
fun formatUnDecimal(valor: Double): String {
    val escalado = round(valor * 10).toLong()
    val parteEntera = escalado / 10
    val parteDecimal = abs(escalado % 10)
    return "$parteEntera.$parteDecimal"
}
