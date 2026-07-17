package com.example.eduflow.sensor

/** Lectura cruda proveniente del hardware de proximidad. */
data class ProximityReading(
    val isNear: Boolean,
    val distanceCm: Float
)

/** Contrato común; solo Android accede directamente a android.hardware.Sensor. */
interface SensorRepository {
    val isAvailable: Boolean
    val maximumRangeCm: Float?

    fun startListening(onReading: (ProximityReading) -> Unit)
    fun stopListening()
}

expect fun createSensorRepository(): SensorRepository
