package com.eduflow.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

object GroqService {
    private val apiKey = System.getenv("GROQ_API_KEY") ?: ""
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    // --- TARJETAS DESDE IMAGEN (Llama 4 Scout - vision) ---
    suspend fun generarTarjetasDesdeImagen(
        imagenBase64: String, contexto: String, materia: String
    ): String {
        val prompt = "Analiza la imagen de apuntes de $materia. " +
            "Genera exactamente 8 tarjetas de estudio en JSON. " +
            "Responde SOLO con este formato sin texto adicional, sin backticks, " +
            "todo en una sola linea: " +
            "[{\"pregunta\":\"...\",\"respuesta\":\"...\"}]"

        // El body se construye con JsonObject para evitar errores de
        // escapado al insertar texto del usuario dentro del JSON.
        val bodyJson = buildJsonObject {
            put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", "data:image/jpeg;base64,$imagenBase64")
                            }
                        }
                    }
                }
            }
            put("max_tokens", 1500)
        }
        return llamarGroqChat(bodyJson.toString())
    }

    // --- TARJETAS DESDE TEXTO (Llama 4 Scout - solo texto) ---
    suspend fun generarTarjetasDesdeTexto(texto: String, materia: String): String {
        val prompt = "Eres un asistente educativo para $materia. " +
            "Con base en este contenido: $texto. " +
            "Genera exactamente 8 tarjetas de estudio. " +
            "Responde SOLO con JSON, sin backticks, todo en una sola linea: " +
            "[{\"pregunta\":\"...\",\"respuesta\":\"...\"}]"

        val bodyJson = buildJsonObject {
            put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            put("max_tokens", 1500)
        }
        return llamarGroqChat(bodyJson.toString())
    }

    // --- CONSEJO DE ESTUDIO (llama-3.1-8b-instant) ---
    suspend fun generarConsejo(materia: String): String {
        val prompt = "Eres un asistente educativo. Dame un consejo de estudio " +
            "breve y concreto (maximo 3 oraciones) para estudiar la materia: $materia. " +
            "Solo el consejo, sin saludos."

        val bodyJson = buildJsonObject {
            put("model", "llama-3.1-8b-instant")
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            put("max_tokens", 200)
        }
        return llamarGroqChat(bodyJson.toString())
    }

    // --- GUION DEL PODCAST (llama-3.1-8b-instant - rapido) ---
    suspend fun generarGuionPodcast(materia: String, tema: String): String {
        val prompt = "Eres un host de podcast educativo divertido y entusiasta. " +
            "Crea un episodio corto (maximo 150 palabras) sobre $tema de $materia. " +
            "Incluye 2-3 datos curiosos sorprendentes. Usa un tono casual y emocionante. " +
            "Empieza con [cheerful] y usa [curious] antes de los datos curiosos. " +
            "NO incluyas titulos ni marcadores, solo el guion directo."

        val bodyJson = buildJsonObject {
            put("model", "llama-3.1-8b-instant")
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            put("max_tokens", 400)
        }
        return llamarGroqChat(bodyJson.toString())
    }

    // --- AUDIO CON ORPHEUS TTS ---
    suspend fun generarAudio(texto: String): ByteArray {
        // Orpheus tiene limite de 200 chars. Si el guion es mas largo,
        // se toma solo los primeros 190 chars para no exceder el limite.
        val textoLimitado = if (texto.length > 190) texto.take(190) else texto

        val bodyJson = buildJsonObject {
            put("model", "canopylabs/orpheus-v1-english")
            put("input", textoLimitado)
            put("voice", "aria")
            put("response_format", "wav")
        }

        val response = client.post("https://api.groq.com/openai/v1/audio/speech") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(bodyJson.toString())
        }
        return response.readBytes()
    }

    // --- FUNCION AUXILIAR: llamada al chat de Groq ---
    // Usa kotlinx.serialization para leer el campo "content" en vez de un
    // regex fragil. Esto soporta saltos de linea, comillas internas y
    // cualquier formato de espaciado que devuelva el modelo.
    private suspend fun llamarGroqChat(body: String): String {
        val response = client.post(
            "https://api.groq.com/openai/v1/chat/completions"
        ) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()

        return try {
            val root = json.parseToJsonElement(response).jsonObject
            val choices = root["choices"]?.jsonArray
            val content = choices?.firstOrNull()
                ?.jsonObject?.get("message")
                ?.jsonObject?.get("content")
                ?.jsonPrimitive?.content
            content?.trim() ?: "Sin respuesta"
        } catch (e: Exception) {
            "Sin respuesta"
        }
    }

    // --- EXTRAE EL JSON DE TARJETAS DE LA RESPUESTA DEL MODELO ---
    // Los modelos a veces envuelven el JSON en texto adicional pese a
    // pedirselo limpio. Esta funcion busca el primer '[' y el ultimo ']'
    // para aislar el array antes de parsearlo, y devuelve una lista
    // de pares (pregunta, respuesta) ya parseados de forma robusta.
    fun extraerTarjetas(textoModelo: String): List<Pair<String, String>> {
        return try {
            val inicio = textoModelo.indexOf('[')
            val fin = textoModelo.lastIndexOf(']')
            if (inicio == -1 || fin == -1 || fin < inicio) return emptyList()

            val jsonLimpio = textoModelo.substring(inicio, fin + 1)
            val arr = json.parseToJsonElement(jsonLimpio).jsonArray
            arr.mapNotNull { elemento ->
                val obj = elemento.jsonObject
                val pregunta = obj["pregunta"]?.jsonPrimitive?.content
                val respuesta = obj["respuesta"]?.jsonPrimitive?.content
                if (pregunta != null && respuesta != null) pregunta to respuesta else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
