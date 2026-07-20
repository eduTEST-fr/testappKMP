package com.example.eduflow.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.eduflow.notifications.AppContextHolder

class ProximitySensorManager : SensorRepository {
    private val sensorManager = AppContextHolder.appContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var listener: SensorEventListener? = null

    override val isAvailable: Boolean
        get() = proximitySensor != null

    override val maximumRangeCm: Float?
        get() = proximitySensor?.maximumRange

    override fun startListening(onReading: (ProximityReading) -> Unit) {
        stopListening()
        val sensor = proximitySensor ?: return
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val distance = event.values.firstOrNull() ?: sensor.maximumRange
                // Muchos equipos solo reportan 0 y maximumRange. Comparar contra
                // maximumRange también funciona en sensores con valores intermedios.
                val near = distance < sensor.maximumRange
                onReading(ProximityReading(near, distance))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun stopListening() {
        listener?.let(sensorManager::unregisterListener)
        listener = null
    }
}

actual fun createSensorRepository(): SensorRepository = ProximitySensorManager()
