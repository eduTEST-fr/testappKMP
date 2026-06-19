package com.example.eduflow.service

class GorqService {

    fun construirPrompt(tema: String): String {
        // Sanitiza el input para evitar caracteres que rompan el JSON
        val temaSeguro = tema.replace("\"", "'").replace("\\", "")
        return "Eres un asistente educativo. " +
                "Dame un consejo de estudio breve y concreto " +
                "(máximo 3 oraciones) para estudiar el tema: $temaSeguro. " +
                "Solo el consejo, sin saludos."
    }
}
