package com.example.eduflow.service

// Valida las credenciales del usuario.
// Usuario de prueba: alumno@upt.mx / 1234
class AuthService {
    fun validar(email: String, password: String): Boolean {
        return email == "alumno@upt.mx" && password == "1234"
    }
}
