package com.example.eduflow.sensor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyLogicTest {
    @Test
    fun timerSumaSoloIntervalosActivos() {
        var now = 1_000L
        val timer = StudyTimer { now }

        timer.setActive(true)
        now += 5_000L
        timer.setActive(false)
        now += 2_000L
        timer.setActive(true)
        now += 3_000L

        assertTrue(timer.isActive)
        assertEquals(8_000L, timer.totalMillis())
        assertEquals(8_000L, timer.finish())
        assertFalse(timer.isActive)
    }

    @Test
    fun dificultadAltaConExamenCercanoAumentaLaMeta() {
        assertEquals(65, calcularMinutosIdeales(9, null))
        assertEquals(75, calcularMinutosIdeales(9, 5))
        assertEquals(80, calcularMinutosIdeales(9, 2))
        assertEquals(7, diasAnticipacionEstudio(9))
    }
}
