package com.example.eduflow

import androidx.compose.runtime.*
import com.example.eduflow.navigation.PlatformBackHandler
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
    var pantalla by remember { mutableStateOf(Pantalla.SPLASH) }
    val historial = remember { mutableStateListOf<Pantalla>() }
    var solicitudIdActual by remember { mutableStateOf(0) }
    var asesorIdActual by remember { mutableStateOf(0) }

    fun raizSegunRol(): Pantalla = when (SesionStorage.obtenerRol()) {
        "ASESOR", "ADMIN" -> Pantalla.PEERS
        else -> Pantalla.DASHBOARD
    }

    fun navegar(destino: Pantalla, limpiarHistorial: Boolean = false) {
        if (destino == pantalla) return
        if (limpiarHistorial) historial.clear() else historial.add(pantalla)
        pantalla = destino
    }

    fun volver() {
        pantalla = if (historial.isNotEmpty()) historial.removeAt(historial.lastIndex) else raizSegunRol()
    }

    fun navegarPrincipal(destino: Pantalla) {
        if (destino == pantalla) return
        val raiz = raizSegunRol()
        historial.clear()
        if (destino != raiz) historial.add(raiz)
        pantalla = destino
    }


    fun iniciarSesion(): Pantalla {
        if (!SesionStorage.haySesion()) return Pantalla.LOGIN
        NotificationScheduler.iniciar()
        historial.clear()
        return raizSegunRol()
    }

    fun cerrarSesion() {
        NotificationScheduler.detener()
        SesionStorage.cerrarSesion()
        historial.clear()
        pantalla = Pantalla.LOGIN
    }

    // En Android intercepta el gesto/botón Atrás en todas las vistas internas.
    // En la pantalla raíz se conserva el comportamiento normal del sistema.
    PlatformBackHandler(
        enabled = historial.isNotEmpty() && pantalla !in listOf(Pantalla.SPLASH, Pantalla.LOGIN)
    ) { volver() }

    // Mientras la app está abierta, solicita un chequeo ligero cada minuto.
    LaunchedEffect(pantalla, SesionStorage.haySesion()) {
        if (SesionStorage.haySesion() && pantalla !in listOf(Pantalla.SPLASH, Pantalla.LOGIN)) {
            while (true) {
                delay(60_000)
                NotificationScheduler.sincronizarAhora()
            }
        }
    }

    when (pantalla) {
        Pantalla.SPLASH -> SplashScreenView(onFinish = { pantalla = iniciarSesion() })
        Pantalla.LOGIN -> LoginView(
            onLoginExitoso = { pantalla = iniciarSesion() },
            onIrARegistro = { navegar(Pantalla.REGISTER) }
        )
        Pantalla.REGISTER -> RegisterView(
            onRegistroExitoso = { pantalla = iniciarSesion() },
            onVolver = { volver() }
        )

        Pantalla.DASHBOARD -> DashboardView(
            onVerStudyCast = { navegarPrincipal(Pantalla.STUDYCAST) },
            onVerAudios = { navegarPrincipal(Pantalla.AUDIOS) },
            onVerPeers = { navegarPrincipal(Pantalla.PEERS) },
            onVerPerfil = { navegar(Pantalla.PERFIL) },
            onVerNotificaciones = { navegar(Pantalla.NOTIFICACIONES) },
            onVerMisAsesorias = { navegar(Pantalla.MIS_ASESORIAS) },
            onIniciarSesionEstudio = { navegar(Pantalla.SESION_ESTUDIO) },
            onCerrarSesion = ::cerrarSesion
        )
        Pantalla.STUDYCAST -> StudyCastView(
            onVolver = ::volver,
            onVerAudios = { navegarPrincipal(Pantalla.AUDIOS) },
            onVerPeers = { navegarPrincipal(Pantalla.PEERS) }
        )
        Pantalla.AUDIOS -> AudiosView(
            onVolver = ::volver,
            onVerStudyCast = { navegarPrincipal(Pantalla.STUDYCAST) },
            onVerPeers = { navegarPrincipal(Pantalla.PEERS) }
        )
        Pantalla.PERFIL -> PerfilView(onVolver = ::volver)
        Pantalla.NOTIFICACIONES -> NotificacionesView(onVolver = ::volver)
        Pantalla.MIS_ASESORIAS -> MisAsesoriasView(onVolver = ::volver)
        Pantalla.SESION_ESTUDIO -> SesionEstudioView(onVolver = ::volver)

        Pantalla.PEERS -> PeersView(
            onVolver = {
                if (SesionStorage.obtenerRol() == "ALUMNO") volver()
            },
            onVerStudyCast = { if (SesionStorage.obtenerRol() == "ALUMNO") navegarPrincipal(Pantalla.STUDYCAST) },
            onVerAudios = { if (SesionStorage.obtenerRol() == "ALUMNO") navegarPrincipal(Pantalla.AUDIOS) },
            onVerPerfil = { navegar(Pantalla.PERFIL) },
            onCerrarSesion = ::cerrarSesion,
            onVerDetalle = { id -> solicitudIdActual = id; navegar(Pantalla.SOLICITUD_DETALLE) },
            onVerTodosAsesores = { navegar(Pantalla.ASESORES_LISTA) },
            onVerPerfilAsesor = { id -> asesorIdActual = id; navegar(Pantalla.PERFIL_ASESOR) },
            onVerMisAsesorias = { navegar(Pantalla.MIS_ASESORIAS) }
        )
        Pantalla.SOLICITUD_DETALLE -> SolicitudDetalleView(
            solicitudId = solicitudIdActual,
            onVolver = ::volver
        )
        Pantalla.ASESORES_LISTA -> AsesoresListaView(
            onVolver = ::volver,
            onVerPerfilAsesor = { id -> asesorIdActual = id; navegar(Pantalla.PERFIL_ASESOR) }
        )
        Pantalla.PERFIL_ASESOR -> PerfilAsesorView(
            asesorId = asesorIdActual,
            onVolver = ::volver,
            onAgendar = { id -> asesorIdActual = id; navegar(Pantalla.AGENDAR_ASESORIA) }
        )
        Pantalla.AGENDAR_ASESORIA -> AgendarAsesoriaView(
            asesorId = asesorIdActual,
            onVolver = ::volver,
            onSolicitudEnviada = {
                historial.clear()
                pantalla = Pantalla.PEERS
            }
        )
    }
}
