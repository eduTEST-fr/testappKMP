// storage/HorarioStorage.kt
package com.example.eduflow.storage

object HorarioStorage {

    private val materias = mutableListOf<Pair<String, Int>>()

    fun guardarMateria(nombre: String, dificultad: Int) {
        materias.add(Pair(nombre, dificultad))
    }

    fun obtenerMaterias(): List<Pair<String, Int>> {
        return materias.toList()
    }

    fun limpiar() {
        materias.clear()
    }
}