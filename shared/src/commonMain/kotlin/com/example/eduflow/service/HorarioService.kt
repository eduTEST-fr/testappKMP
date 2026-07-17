// service/HorarioService.kt
package com.example.eduflow.service

class HorarioService {

    fun calcularDiasEstudio(dificultad: Int): Int {
        return when {
            dificultad >= 9 -> 10
            dificultad >= 7 -> 7
            dificultad >= 5 -> 4
            dificultad >= 3 -> 2
            else            -> 1
        }
    }

    fun generarRecomendacion(nombreMateria: String, dificultad: Int): String {
        val dias = calcularDiasEstudio(dificultad)
        return "Para $nombreMateria debes empezar a estudiar $dias días antes del examen."
    }
}