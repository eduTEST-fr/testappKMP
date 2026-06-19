// api/ConsejosApi.kt
package com.example.eduflow.api

class ConsejosApi {
    fun obtenerConsejo(): String {
        return try {
            val url = java.net.URL("https://zenquotes.io/api/random")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "No se pudo obtener un consejo."
        }
    }
}
