package com.eduflow.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

object GroqService {
    private val apiKey = System.getenv("GROQ_API_KEY") ?: ""
    private val client = HttpClient()

    // --- TARJETAS DESDE IMAGEN (Llama 4 Scout - vision) ---
    suspend fun generarTarjetasDesdeImagen(
        imagenBase64: String, contexto: String, materia: String
    ): String {
        val prompt = """Analiza la imagen de apuntes de $materia.
            Genera exactamente 8 tarjetas de estudio en JSON.
            Responde SOLO con este formato sin texto adicional:
            [{"pregunta":"...","respuesta":"..."},...]"""
        val body = """
            {
              "model": "meta-llama/llama-4-scout-17b-16e-instruct",
              "messages": [{
                "role": "user",
                "content": [
                  {"type": "text", "text": "$prompt"},
                  {"type": "image_url",
                   "image_url": {"url": "data:image/jpeg;base64,$imagenBase64"}}
                ]
              }],
              "max_tokens": 1500
            }
        """.trimIndent()
        return llamarGroqChat(body)
    }

    // --- TARJETAS DESDE TEXTO (Llama 4 Scout - solo texto) ---
    suspend fun generarTarjetasDesdeTexto(texto: String, materia: String): String {
        val prompt = """Eres un asistente educativo para $materia.
            Con base en este contenido: $texto
            Genera 8 tarjetas de estudio. Responde SOLO con JSON:
            [{"pregunta":"...","respuesta":"..."},...]"""
        val body = """
            {
              "model": "meta-llama/llama-4-scout-17b-16e-instruct",
              "messages": [{"role": "user", "content": "$prompt"}],
              "max_tokens": 1500
            }
        """.trimIndent()
        return llamarGroqChat(body)
    }

    // --- CONSEJO DE ESTUDIO (llama-3.1-8b-instant) ---
    suspend fun generarConsejo(materia: String): String {
        val prompt = """Eres un asistente educativo. Dame un consejo de estudio
            breve y concreto (maximo 3 oraciones) para estudiar la materia: $materia.
            Solo el consejo, sin saludos."""
        val body = """
            {
              "model": "llama-3.1-8b-instant",
              "messages": [{"role": "user", "content": "$prompt"}],
              "max_tokens": 200
            }
        """.trimIndent()
        return llamarGroqChat(body)
    }

    // --- GUION DEL PODCAST (llama-3.1-8b-instant - rapido) ---
    suspend fun generarGuionPodcast(materia: String, tema: String): String {
        val prompt = """Eres un host de podcast educativo divertido y entusiasta.
            Crea un episodio corto (maximo 150 palabras) sobre $tema de $materia.
            Incluye 2-3 datos curiosos sorprendentes. Usa un tono casual y emocionante.
            Empieza con [cheerful] y usa [curious] antes de los datos curiosos.
            NO incluyas titulos ni marcadores, solo el guion directo."""
        val body = """
            {
              "model": "llama-3.1-8b-instant",
              "messages": [{"role": "user", "content": "$prompt"}],
              "max_tokens": 400
            }
        """.trimIndent()
        return llamarGroqChat(body)
    }

    // --- AUDIO CON ORPHEUS TTS ---
    suspend fun generarAudio(texto: String): ByteArray {
        // Orpheus tiene limite de 200 chars. Si el guion es mas largo,
        // se toma solo los primeros 190 chars para no exceder el limite.
        val textoLimitado = if (texto.length > 190) texto.take(190) else texto
        val body = """
            {
              "model": "canopylabs/orpheus-v1-english",
              "input": "$textoLimitado",
              "voice": "aria",
              "response_format": "wav"
            }
        """.trimIndent()
        val response = client.post("https://api.groq.com/openai/v1/audio/speech") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.readBytes()
    }

    // --- FUNCION AUXILIAR: llamada al chat de Groq ---
    private suspend fun llamarGroqChat(body: String): String {
        val response = client.post(
            "https://api.groq.com/openai/v1/chat/completions"
        ) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
        // Extrae el campo "content" de la respuesta
        val regex = Regex(""""content"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return regex.find(response)?.groupValues?.get(1)
            ?.replace("\\n", " ")?.replace("\\\"", "\"")?.trim()
            ?: "Sin respuesta"
    }
}
