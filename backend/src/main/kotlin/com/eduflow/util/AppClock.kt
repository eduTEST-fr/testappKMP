package com.eduflow.util

import java.time.LocalDate
import java.time.ZoneId

/**
 * Fecha académica usada por el backend. Railway suele ejecutar en UTC, por lo que
 * se fija una zona configurable para evitar bloquear materiales varias horas antes
 * o después del día real del examen.
 */
object AppClock {
    private val zona: ZoneId = runCatching {
        ZoneId.of(System.getenv("APP_TIME_ZONE") ?: "America/Mexico_City")
    }.getOrElse { ZoneId.of("America/Mexico_City") }

    fun hoy(): LocalDate = LocalDate.now(zona)
}
