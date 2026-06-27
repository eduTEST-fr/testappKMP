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
    var pantalla          by remember { mutableStateOf(Pantalla.SPLASH) }
    var origenPerfil      by remember { mutableStateOf(Pantalla.DASHBOARD) }
    var solicitudIdActual by remember { mutableStateOf(0) }

    // Tras login, Asesor y Admin van directo a Peers; Alumno va a Dashboard
    fun pantallaInicial(): Pantalla {
        if (!SesionStorage.haySesion()) return Pantalla.LOGIN
        return when (SesionStorage.obtenerRol()) {
            "ASESOR", "ADMIN" -> Pantalla.PEERS
            else -> Pantalla.DASHBOARD
        }
    }

    when (pantalla) {
        Pantalla.SPLASH -> SplashScreenView(
            onFinish = { pantalla = pantallaInicial() }
        )
        Pantalla.LOGIN -> LoginView(
            onLoginExitoso = { pantalla = pantallaInicial() },
            onIrARegistro  = { pantalla = Pantalla.REGISTER }
        )
        Pantalla.REGISTER -> RegisterView(
            onRegistroExitoso = { pantalla = pantallaInicial() },
            onVolver          = { pantalla = Pantalla.LOGIN }
        )

        // ── Solo ALUMNO llega aquí ──
        Pantalla.DASHBOARD -> DashboardView(
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST },
            onVerAudios    = { pantalla = Pantalla.AUDIOS },
            onVerPeers     = { pantalla = Pantalla.PEERS },
            onVerPerfil    = { origenPerfil = Pantalla.DASHBOARD; pantalla = Pantalla.PERFIL },
            onCerrarSesion = { SesionStorage.cerrarSesion(); pantalla = Pantalla.LOGIN }
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
        Pantalla.PERFIL -> PerfilView(
            onVolver = { pantalla = origenPerfil }
        )

        // ── Todos los roles llegan aquí ──
        Pantalla.PEERS -> PeersView(
            onVolver       = { pantalla = Pantalla.DASHBOARD },
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST },
            onVerAudios    = { pantalla = Pantalla.AUDIOS },
            onVerPerfil    = { origenPerfil = Pantalla.PEERS; pantalla = Pantalla.PERFIL },
            onCerrarSesion = { SesionStorage.cerrarSesion(); pantalla = Pantalla.LOGIN },
            onVerDetalle   = { id -> solicitudIdActual = id; pantalla = Pantalla.SOLICITUD_DETALLE }
        )
        Pantalla.SOLICITUD_DETALLE -> SolicitudDetalleView(
            solicitudId = solicitudIdActual,
            onVolver    = { pantalla = Pantalla.PEERS }
        )
    }
}
