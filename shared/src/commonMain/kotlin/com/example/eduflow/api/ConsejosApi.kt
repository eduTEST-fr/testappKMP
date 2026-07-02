// api/ConsejosApi.kt
package com.example.eduflow.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

// Antes usaba java.net.URL / HttpURLConnection, que solo existen en la JVM
// (Android) y no compilan para Web/Wasm. Ktor ya es una dependencia del
// proyecto (shared/build.gradle.kts) y funciona igual en Android, JS y
// Wasm, así que el comportamiento real (llamada de red en vivo a la API)
// se mantiene idéntico en todas las plataformas.
class ConsejosApi {
    private val client = HttpClient()

    suspend fun obtenerConsejo(): String {
        return try {
            client.get("https://zenquotes.io/api/random").bodyAsText()
        } catch (e: Exception) {
            "No se pudo obtener un consejo."
        }
    }
}
