package com.example.eduflow

import androidx.compose.runtime.*
import com.example.eduflow.storage.SesionStorage
import com.example.eduflow.ui.*

enum class Pantalla { LOGIN, REGISTER, DASHBOARD, STUDYCAST, PEERS }

@Composable
fun App() {
    // Si hay JWT guardado, saltar directo al Dashboard
    val inicio = if (SesionStorage.haySesion()) Pantalla.DASHBOARD else Pantalla.LOGIN
    var pantalla by remember { mutableStateOf(inicio) }

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
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST },
            onVerPeers     = { pantalla = Pantalla.PEERS },
            onCerrarSesion = {
                SesionStorage.cerrarSesion()
                pantalla = Pantalla.LOGIN
            }
        )
        Pantalla.STUDYCAST  -> StudyCastView(
            onVolver   = { pantalla = Pantalla.DASHBOARD },
            onVerPeers = { pantalla = Pantalla.PEERS }
        )
        Pantalla.PEERS      -> PeersView(
            onVolver      = { pantalla = Pantalla.DASHBOARD },
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST }
        )
    }
}
