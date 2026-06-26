package com.example.eduflow

import androidx.compose.runtime.*
import com.example.eduflow.storage.SesionStorage
import com.example.eduflow.ui.*

enum class Pantalla {
    SPLASH, LOGIN, REGISTER,
    DASHBOARD, STUDYCAST, AUDIOS,
    PEERS, SOLICITUD_DETALLE, PERFIL
}

@Composable
fun App() {
    var pantalla        by remember { mutableStateOf(Pantalla.SPLASH) }
    var origenPerfil    by remember { mutableStateOf(Pantalla.PEERS) }
    var solicitudIdActual by remember { mutableStateOf(0) }

    when (pantalla) {
        Pantalla.SPLASH -> SplashScreenView(
            onFinish = {
                pantalla = if (SesionStorage.haySesion()) Pantalla.DASHBOARD else Pantalla.LOGIN
            }
        )
        Pantalla.LOGIN -> LoginView(
            onLoginExitoso = { pantalla = Pantalla.DASHBOARD },
            onIrARegistro  = { pantalla = Pantalla.REGISTER }
        )
        Pantalla.REGISTER -> RegisterView(
            onRegistroExitoso = { pantalla = Pantalla.DASHBOARD },
            onVolver          = { pantalla = Pantalla.LOGIN }
        )
        Pantalla.DASHBOARD -> DashboardView(
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST },
            onVerAudios    = { pantalla = Pantalla.AUDIOS },
            onVerPeers     = { pantalla = Pantalla.PEERS },
            onVerPerfil    = { origenPerfil = Pantalla.DASHBOARD; pantalla = Pantalla.PERFIL },
            onCerrarSesion = {
                SesionStorage.cerrarSesion()
                pantalla = Pantalla.LOGIN
            }
        )
        Pantalla.STUDYCAST -> StudyCastView(
            onVolver    = { pantalla = Pantalla.DASHBOARD },
            onVerAudios = { pantalla = Pantalla.AUDIOS },
            onVerPeers  = { pantalla = Pantalla.PEERS }
        )
        Pantalla.AUDIOS -> AudiosView(
            onVolver       = { pantalla = Pantalla.DASHBOARD },
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST },
            onVerPeers     = { pantalla = Pantalla.PEERS }
        )
        Pantalla.PEERS -> PeersView(
            onVolver       = { pantalla = Pantalla.DASHBOARD },
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST },
            onVerAudios    = { pantalla = Pantalla.AUDIOS },
            onVerPerfil    = { origenPerfil = Pantalla.PEERS; pantalla = Pantalla.PERFIL },
            onVerDetalle   = { id -> solicitudIdActual = id; pantalla = Pantalla.SOLICITUD_DETALLE }
        )
        Pantalla.SOLICITUD_DETALLE -> SolicitudDetalleView(
            solicitudId    = solicitudIdActual,
            onVolver       = { pantalla = Pantalla.PEERS }
        )
        Pantalla.PERFIL -> PerfilView(
            onVolver = { pantalla = origenPerfil }
        )
    }
}
