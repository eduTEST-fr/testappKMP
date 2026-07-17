package com.example.eduflow

import androidx.compose.runtime.*
import com.example.eduflow.notifications.NotificationScheduler
import com.example.eduflow.storage.SesionStorage
import com.example.eduflow.ui.*
import kotlinx.coroutines.delay

enum class Pantalla {
    SPLASH, LOGIN, REGISTER,
    DASHBOARD, STUDYCAST, AUDIOS,
    PEERS, SOLICITUD_DETALLE, PERFIL,
    ASESORES_LISTA, PERFIL_ASESOR, AGENDAR_ASESORIA,
    MIS_ASESORIAS, NOTIFICACIONES, SESION_ESTUDIO
}

@Composable
fun App() {
    var pantalla          by remember { mutableStateOf(Pantalla.SPLASH) }
    var origenPerfil      by remember { mutableStateOf(Pantalla.DASHBOARD) }
    var origenAsesores    by remember { mutableStateOf(Pantalla.PEERS) }
    var solicitudIdActual by remember { mutableStateOf(0) }
    var asesorIdActual    by remember { mutableStateOf(0) }

    // Mientras la app está abierta, solicita un chequeo ligero cada minuto.
    // Con la app cerrada, WorkManager conserva la revisión periódica de Android.
    LaunchedEffect(pantalla, SesionStorage.haySesion()) {
        if (SesionStorage.haySesion() && pantalla != Pantalla.SPLASH &&
            pantalla != Pantalla.LOGIN && pantalla != Pantalla.REGISTER) {
            while (true) {
                delay(60_000)
                NotificationScheduler.sincronizarAhora()
            }
        }
    }

    // Tras login, Asesor y Admin van directo a Peers; Alumno va a Dashboard
    fun pantallaInicial(): Pantalla {
        if (!SesionStorage.haySesion()) return Pantalla.LOGIN
        // Hay sesión activa: prendemos la revisión periódica de notificaciones
        // en segundo plano (en Web esto no hace nada).
        NotificationScheduler.iniciar()
        return when (SesionStorage.obtenerRol()) {
            "ASESOR", "ADMIN" -> Pantalla.PEERS
            else -> Pantalla.DASHBOARD
        }
    }

    fun cerrarSesion() {
        NotificationScheduler.detener()
        SesionStorage.cerrarSesion()
        pantalla = Pantalla.LOGIN
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
            onVerNotificaciones = { pantalla = Pantalla.NOTIFICACIONES },
            onVerMisAsesorias   = { pantalla = Pantalla.MIS_ASESORIAS },
            onIniciarSesionEstudio = { pantalla = Pantalla.SESION_ESTUDIO },
            onCerrarSesion = { cerrarSesion() }
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
        Pantalla.NOTIFICACIONES -> NotificacionesView(
            onVolver = { pantalla = Pantalla.DASHBOARD }
        )
        Pantalla.MIS_ASESORIAS -> MisAsesoriasView(
            onVolver = { pantalla = if (SesionStorage.obtenerRol() == "ASESOR") Pantalla.PEERS else Pantalla.DASHBOARD }
        )
        Pantalla.SESION_ESTUDIO -> SesionEstudioView(
            onVolver = { pantalla = Pantalla.DASHBOARD }
        )

        // ── Todos los roles llegan aquí ──
        Pantalla.PEERS -> PeersView(
            onVolver       = { pantalla = Pantalla.DASHBOARD },
            onVerStudyCast = { pantalla = Pantalla.STUDYCAST },
            onVerAudios    = { pantalla = Pantalla.AUDIOS },
            onVerPerfil    = { origenPerfil = Pantalla.PEERS; pantalla = Pantalla.PERFIL },
            onCerrarSesion = { cerrarSesion() },
            onVerDetalle   = { id -> solicitudIdActual = id; pantalla = Pantalla.SOLICITUD_DETALLE },
            onVerTodosAsesores = { origenAsesores = Pantalla.PEERS; pantalla = Pantalla.ASESORES_LISTA },
            onVerPerfilAsesor  = { id -> asesorIdActual = id; origenAsesores = Pantalla.PEERS; pantalla = Pantalla.PERFIL_ASESOR },
            onVerMisAsesorias  = { pantalla = Pantalla.MIS_ASESORIAS }
        )
        Pantalla.SOLICITUD_DETALLE -> SolicitudDetalleView(
            solicitudId = solicitudIdActual,
            onVolver    = { pantalla = Pantalla.PEERS }
        )
        Pantalla.ASESORES_LISTA -> AsesoresListaView(
            onVolver = { pantalla = origenAsesores },
            onVerPerfilAsesor = { id -> asesorIdActual = id; pantalla = Pantalla.PERFIL_ASESOR }
        )
        Pantalla.PERFIL_ASESOR -> PerfilAsesorView(
            asesorId = asesorIdActual,
            onVolver = { pantalla = origenAsesores },
            onAgendar = { id -> asesorIdActual = id; pantalla = Pantalla.AGENDAR_ASESORIA }
        )
        Pantalla.AGENDAR_ASESORIA -> AgendarAsesoriaView(
            asesorId = asesorIdActual,
            onVolver = { pantalla = Pantalla.PERFIL_ASESOR },
            onSolicitudEnviada = { pantalla = origenAsesores }
        )
    }
}
