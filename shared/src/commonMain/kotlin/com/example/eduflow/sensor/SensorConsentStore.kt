package com.example.eduflow.sensor

import com.russhwolf.settings.Settings

object SensorConsentStore {
    private const val KEY = "proximity_sensor_consent"
    private val settings = Settings()

    fun isAccepted(): Boolean = settings.getBoolean(KEY, false)
    fun accept() = settings.putBoolean(KEY, true)
}
