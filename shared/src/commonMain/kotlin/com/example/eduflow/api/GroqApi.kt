package com.example.eduflow.api

import com.example.eduflow.service.GorqService
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

// Solo agregamos la variable al constructor para recibirla desde afuera
class GeminiApi(private val apiKey: String) {

    private val service = GorqService()
    private val client  = HttpClient()

    suspend fun obtenerConsejo(tema: String): String {
        return try {
            val prompt = service.construirPrompt(tema)

            val respuesta = client.post(
                "https://api.groq.com/openai/v1/chat/completions"
            ) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody("""
                    {
                      "model": "llama-3.1-8b-instant",
                      "messages": [{"role": "user",
                                    "content": "${prompt.replace("\"", "'")}"
                                   }],
                      "max_tokens": 200
                    }
                """.trimIndent())
            }.bodyAsText()

            // Extrae el campo "content" con expresión regular
            val regex = Regex(""""content"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            val match = regex.find(respuesta)
                ?: return "Sin respuesta. Cruda: ${respuesta.take(200)}"

            match.groupValues[1]
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .trim()

        } catch (e: Exception) {
            "Error: ${e::class.simpleName} | ${e.message}"
        }
    }
}

