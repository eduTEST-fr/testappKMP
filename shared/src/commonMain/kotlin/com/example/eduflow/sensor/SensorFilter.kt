package com.example.eduflow.sensor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Propaga un cambio únicamente cuando permanece estable durante 300 ms.
 * El acceso al hardware queda limpio y el filtrado sigue siendo multiplataforma.
 */
class SensorFilter(
    private val scope: CoroutineScope,
    private val stabilityMillis: Long = 300L
) {
    private var pendingJob: Job? = null
    private var pendingState: Boolean? = null
    private var lastStableState: Boolean? = null

    fun submit(reading: ProximityReading, onStable: (ProximityReading) -> Unit) {
        if (reading.isNear == lastStableState || reading.isNear == pendingState) return
        pendingJob?.cancel()
        pendingState = reading.isNear
        pendingJob = scope.launch {
            delay(stabilityMillis)
            lastStableState = reading.isNear
            pendingState = null
            onStable(reading)
        }
    }

    fun reset() {
        pendingJob?.cancel()
        pendingJob = null
        pendingState = null
        lastStableState = null
    }
}
