package com.eduflow.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URLEncoder

// Genera audio en ESPAÑOL usando el endpoint público de Google Translate
// (translate.google.com/translate_tts). No requiere cuenta ni API key.
// Groq/Orpheus solo soporta voz en ingles y arabe, por eso esta parte se
// resuelve con un proveedor distinto -- el resto de la app (login, materias,
// tarjetas, guion del podcast) sigue usando Groq sin cambios.
object TtsService {
    private val client = HttpClient()

    // El endpoint acepta como mucho ~200 caracteres por peticion. Para un
    // guion mas largo, se divide en varios trozos (respetando palabras
    // completas) y se pide el audio de cada uno por separado.
    private const val MAX_CHARS_POR_PETICION = 200

    suspend fun generarAudioEnEspanol(texto: String): ByteArray? {
        // Las etiquetas como [cheerful] son una convencion de Orpheus, pero
        // este motor no las entiende -- se leerian en voz alta tal cual.
        // Se quitan antes de generar el audio (el guion en pantalla las
        // sigue mostrando igual, esto solo afecta lo que se escucha).
        val textoLimpio = texto
            .replace(Regex("\\[[a-zA-Z]+\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (textoLimpio.isBlank()) {
            println("TtsService.generarAudioEnEspanol - texto vacío, se descarta")
            return null
        }

        val trozos = dividirEnTrozos(textoLimpio, MAX_CHARS_POR_PETICION)
        val partes = mutableListOf<ByteArray>()

        for (trozo in trozos) {
            val audioParte = pedirAudioDeUnTrozo(trozo)
            if (audioParte == null) {
                println("TtsService.generarAudioEnEspanol - fallo un trozo, se descarta el podcast completo")
                return null
            }
            partes.add(audioParte)
        }

        if (partes.isEmpty()) return null

        // Los MP3 que devuelve este endpoint se pueden concatenar
        // directamente como bytes: cada parte es un stream MP3 valido por
        // si mismo y la mayoria de reproductores (incluido MediaPlayer)
        // los reproduce de corrido sin necesidad de remezclar nada.
        val total = partes.sumOf { it.size }
        val resultado = ByteArray(total)
        var offset = 0
        for (parte in partes) {
            parte.copyInto(resultado, offset)
            offset += parte.size
        }
        return resultado
    }

    private suspend fun pedirAudioDeUnTrozo(trozo: String): ByteArray? {
        val textoCodificado = URLEncoder.encode(trozo, "UTF-8")
        val url = "https://translate.google.com/translate_tts" +
            "?ie=UTF-8&q=$textoCodificado&tl=es&client=tw-ob"

        return try {
            val response = client.get(url) {
                // Este endpoint exige un User-Agent de navegador real;
                // sin esto, Google responde 403.
                header("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            if (response.status != HttpStatusCode.OK) {
                println("TtsService - error HTTP ${response.status} para trozo: $trozo")
                return null
            }
            response.readBytes()
        } catch (e: Exception) {
            println("TtsService - excepción al pedir audio: ${e.message}")
            null
        }
    }

    // Corta el texto en trozos que no exceden el limite de caracteres,
    // cortando siempre en un espacio para no partir una palabra a la mitad.
    private fun dividirEnTrozos(texto: String, maxChars: Int): List<String> {
        if (texto.length <= maxChars) return listOf(texto)

        val trozos = mutableListOf<String>()
        var restante = texto
        while (restante.length > maxChars) {
            val corte = restante.take(maxChars)
            val ultimoEspacio = corte.lastIndexOf(' ')
            val puntoDeCorte = if (ultimoEspacio > 0) ultimoEspacio else maxChars
            trozos.add(restante.take(puntoDeCorte).trim())
            restante = restante.substring(puntoDeCorte).trim()
        }
        if (restante.isNotEmpty()) trozos.add(restante)
        return trozos
    }
}
