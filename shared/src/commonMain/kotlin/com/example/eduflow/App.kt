package com.example.eduflow

import androidx.compose.runtime.*
import com.example.eduflow.storage.SesionStorage
import com.example.eduflow.ui.*

enum class Pantalla { LOGIN, REGISTER, DASHBOARD, STUDYCAST }

@Composable
fun App() {
    // Si hay JWT guardado, saltar directo al Dashboard
    val inicio = if (SesionStorage.haySesion()) Pantalla.DASHBOARD else Pantalla.LOGIN
    var pantalla by remember { mutableStateOf(inicio) }
    var tabStudyCastInicial by remember { mutableStateOf(TabStudyCast.CONSEJOS) }

    when (pantalla) {
        Pantalla.LOGIN      -> LoginView(
            onLoginExitoso = { pantalla = Pantalla.DASHBOARD },
            onIrARegistro  = { pantalla = Pantalla.REGISTER }
        )
        Pantalla.REGISTER   -> RegisterView(
            onRegistroExitoso = { pantalla = Pantalla.DASHBOARD },
            onVolver          = { pantalla = Pantalla.LOGIN }
        )
        Pantalla.DASHBOARD  -> DashboardView(
            onVerStudyCast = { tab ->
                tabStudyCastInicial = tab
                pantalla = Pantalla.STUDYCAST
            },
            onCerrarSesion = {
                SesionStorage.cerrarSesion()
                pantalla = Pantalla.LOGIN
            }
        )
        Pantalla.STUDYCAST  -> StudyCastView(
            tabInicial = tabStudyCastInicial,
            onVolver = { pantalla = Pantalla.DASHBOARD }
        )
    }
}
