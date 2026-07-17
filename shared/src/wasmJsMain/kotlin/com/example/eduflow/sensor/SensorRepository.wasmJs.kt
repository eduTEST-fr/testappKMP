package com.example.eduflow.sensor

private class UnavailableSensorRepository : SensorRepository {
    override val isAvailable = false
    override val maximumRangeCm: Float? = null
    override fun startListening(onReading: (ProximityReading) -> Unit) = Unit
    override fun stopListening() = Unit
}

actual fun createSensorRepository(): SensorRepository = UnavailableSensorRepository()
