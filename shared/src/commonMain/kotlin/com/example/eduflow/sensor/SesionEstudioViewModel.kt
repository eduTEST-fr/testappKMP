package com.example.eduflow.sensor

import com.example.eduflow.api.GuardarSesionEstudioRequest
import com.example.eduflow.api.MateriaEstudioDto
import com.example.eduflow.api.SesionEstudioDto
import com.example.eduflow.api.SesionesEstudioApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class EstadoSesionEstudio {
    INACTIVA,
    ESPERANDO_TELEFONO,
    ACTIVA,
    PAUSADA,
    FINALIZADA,
    GUARDANDO,
    GUARDADA,
    ERROR
}

data class SesionEstudioUiState(
    val materia: MateriaEstudioDto? = null,
    val estado: EstadoSesionEstudio = EstadoSesionEstudio.INACTIVA,
    val sensorDisponible: Boolean = false,
    val sensorCerca: Boolean = false,
    val distanciaCm: Float? = null,
    val tiempoAcumuladoMillis: Long = 0L,
    val sesionGuardada: SesionEstudioDto? = null,
    val error: String? = null
)

/**
 * Controla inicio, pausa, reanudación, fin y persistencia de la sesión.
 * El tiempo se obtiene de timestamps, por lo que no depende del refresco visual.
 */
class SesionEstudioViewModel(
    private val scope: CoroutineScope,
    private val sensorRepository: SensorRepository = createSensorRepository(),
    private val apiService: SesionesEstudioApi = SesionesEstudioApi(),
    nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val timer = StudyTimer(nowMillis)
    private val filter = SensorFilter(scope)
    private val _uiState = MutableStateFlow(
        SesionEstudioUiState(sensorDisponible = sensorRepository.isAvailable)
    )
    val uiState: StateFlow<SesionEstudioUiState> = _uiState.asStateFlow()

    fun seleccionarMateria(materia: MateriaEstudioDto) {
        if (esSesionEnCurso()) return
        _uiState.update { it.copy(materia = materia, error = null, sesionGuardada = null) }
    }

    fun iniciarSesion(): Boolean {
        if (_uiState.value.materia == null) {
            _uiState.update { it.copy(error = "Selecciona una materia antes de iniciar.") }
            return false
        }
        timer.reset()
        filter.reset()
        _uiState.update {
            it.copy(
                estado = EstadoSesionEstudio.ESPERANDO_TELEFONO,
                sensorCerca = false,
                distanciaCm = null,
                tiempoAcumuladoMillis = 0L,
                sesionGuardada = null,
                error = null
            )
        }
        sensorRepository.startListening { reading ->
            filter.submit(reading, ::procesarLecturaEstable)
        }
        return true
    }

    fun simularSensor(cerca: Boolean) {
        if (!sensorRepository.isAvailable && esSesionEnCurso()) {
            procesarLecturaEstable(ProximityReading(cerca, if (cerca) 0f else 5f))
        }
    }

    private fun procesarLecturaEstable(reading: ProximityReading) {
        if (!esSesionEnCurso()) return
        timer.setActive(reading.isNear)
        _uiState.update {
            it.copy(
                estado = if (reading.isNear) EstadoSesionEstudio.ACTIVA else EstadoSesionEstudio.PAUSADA,
                sensorCerca = reading.isNear,
                distanciaCm = reading.distanceCm,
                tiempoAcumuladoMillis = timer.totalMillis(),
                error = null
            )
        }
    }

    fun tiempoActualMillis(): Long = if (esSesionEnCurso()) {
        timer.totalMillis()
    } else {
        _uiState.value.tiempoAcumuladoMillis
    }

    fun finalizarSesion(): Boolean {
        if (!esSesionEnCurso()) return false
        sensorRepository.stopListening()
        filter.reset()
        val total = timer.finish()
        _uiState.update {
            it.copy(
                estado = EstadoSesionEstudio.FINALIZADA,
                sensorCerca = false,
                tiempoAcumuladoMillis = total,
                error = if (total <= 0L) "Coloca el teléfono boca abajo al menos unos segundos antes de finalizar." else null
            )
        }
        return total > 0L
    }

    suspend fun guardarSesion(metaMinutos: Int): Result<SesionEstudioDto> {
        val materia = _uiState.value.materia
            ?: return Result.failure(IllegalStateException("No hay materia seleccionada"))
        val duracionMillis = _uiState.value.tiempoAcumuladoMillis
        if (duracionMillis <= 0L) {
            return Result.failure(IllegalStateException("La sesión no tiene tiempo registrado"))
        }

        _uiState.update { it.copy(estado = EstadoSesionEstudio.GUARDANDO, error = null) }
        return runCatching {
            val request = GuardarSesionEstudioRequest(
                materiaId = materia.id,
                fecha = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
                duracionSegundos = ((duracionMillis + 999L) / 1000L).toInt().coerceAtLeast(1),
                metaMinutos = metaMinutos
            )
            apiService.guardarSesion(request)
        }.onSuccess { sesion ->
            _uiState.update {
                it.copy(
                    estado = EstadoSesionEstudio.GUARDADA,
                    sesionGuardada = sesion,
                    error = null
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    estado = EstadoSesionEstudio.ERROR,
                    error = error.message ?: "No se pudo guardar la sesión."
                )
            }
        }
    }

    fun prepararNuevaSesion() {
        sensorRepository.stopListening()
        filter.reset()
        timer.reset()
        _uiState.update {
            SesionEstudioUiState(
                materia = it.materia,
                sensorDisponible = sensorRepository.isAvailable
            )
        }
    }

    fun close() {
        sensorRepository.stopListening()
        filter.reset()
        apiService.close()
    }

    private fun esSesionEnCurso(): Boolean = _uiState.value.estado in setOf(
        EstadoSesionEstudio.ESPERANDO_TELEFONO,
        EstadoSesionEstudio.ACTIVA,
        EstadoSesionEstudio.PAUSADA
    )
}
