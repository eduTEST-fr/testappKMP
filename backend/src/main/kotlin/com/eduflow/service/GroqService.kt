package com.eduflow.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

object GroqService {
    private val apiKey = System.getenv("GROQ_API_KEY") ?: ""
    private val client = HttpClient()

    // Convierte un prompt multilinea (con indentacion de Kotlin) en una sola
    // linea limpia, lista para insertarse como valor de texto en JSON.
    private fun aUnaLinea(s: String): String =
        s.lines().joinToString(" ") { it.trim() }.trim()

    // Escapa un texto para que pueda insertarse de forma segura como valor
    // dentro de un literal JSON (comillas, backslashes, saltos de linea, etc).
    // Sin esto, cualquier prompt con saltos de linea o comillas generaba JSON
    // invalido y Groq respondia con error -> por eso salia "Sin respuesta".
    private fun jsonEscape(s: String): String = buildString {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '"'  -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }

    // --- TARJETAS DESDE IMAGEN (Llama 4 Scout - vision) ---
    suspend fun generarTarjetasDesdeImagen(
        imagenBase64: String, contexto: String, materia: String
    ): String {
        val prompt = jsonEscape(aUnaLinea("""Analiza la imagen de apuntes de $materia.
            Genera exactamente 8 tarjetas de estudio en JSON.
            Responde SOLO con este formato sin texto adicional:
            [{"pregunta":"...","respuesta":"..."},...]"""))
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
        val prompt = jsonEscape(aUnaLinea("""Eres un asistente educativo para $materia.
            Con base en este contenido: $texto
            Genera 8 tarjetas de estudio. Responde SOLO con JSON:
            [{"pregunta":"...","respuesta":"..."},...]"""))
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
        val prompt = jsonEscape(aUnaLinea("""Eres un asistente educativo. Dame un consejo de estudio
            breve y concreto (maximo 3 oraciones) para estudiar la materia: $materia.
            Solo el consejo, sin saludos."""))
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
        val prompt = jsonEscape(aUnaLinea("""Eres un host de podcast educativo divertido y entusiasta.
            Crea un episodio corto (maximo 150 palabras) sobre $tema de $materia.
            Incluye 2-3 datos curiosos sorprendentes. Usa un tono casual y emocionante.
            Empieza con [cheerful] y usa [curious] antes de los datos curiosos.
            NO incluyas titulos ni marcadores, solo el guion directo."""))
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
        val textoLimitado = jsonEscape(
            if (texto.length > 190) texto.take(190) else texto
        )
        // Voces validas de canopylabs/orpheus-v1-english: autumn, diana,
        // hannah, austin, daniel, troy. "aria" no existe -> Groq devolvia
        // error y ese error se guardaba como si fuera el audio.
        val body = """
            {
              "model": "canopylabs/orpheus-v1-english",
              "input": "$textoLimitado",
              "voice": "autumn",
              "response_format": "wav"
            }
        """.trimIndent()
        val response = client.post("https://api.groq.com/openai/v1/audio/speech") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw IllegalStateException("Groq TTS error (${response.status.value}): $errorBody")
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
        }
        val responseText = response.bodyAsText()

        if (!response.status.isSuccess()) {
            // Mostramos el motivo real en vez de un generico "Sin respuesta",
            // para poder diagnosticar rapido si algo vuelve a fallar.
            val rErrMsg = Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            val msg = rErrMsg.find(responseText)?.groupValues?.get(1) ?: responseText.take(200)
            return "Error de IA: $msg"
        }

        // Extrae el campo "content" de la respuesta
        val regex = Regex(""""content"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return regex.find(responseText)?.groupValues?.get(1)
            ?.replace("\\n", " ")?.replace("\\\"", "\"")?.trim()
            ?: "Sin respuesta"
    }
}
