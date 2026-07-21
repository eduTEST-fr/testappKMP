package com.eduflow.routes

import com.eduflow.model.*
import com.eduflow.service.NotificacionService
import com.eduflow.util.AppClock
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun diasAnticipacionPorDificultad(dificultad: Int): Int = when {
    dificultad >= 9 -> 7
    dificultad >= 7 -> 5
    dificultad >= 5 -> 4
    dificultad >= 3 -> 3
    else -> 2
}

private fun minutosIdeales(dificultad: Int, diasParaExamen: Long? = null): Int {
    val base = 20 + dificultad.coerceIn(1, 10) * 5
    val bonoUrgencia = when {
        diasParaExamen == null -> 0
        diasParaExamen <= 2 -> 15
        diasParaExamen <= 5 -> 10
        else -> 0
    }
    return (base + bonoUrgencia).coerceAtMost(90)
}

fun Routing.notificacionRoutes() {
    // Genera recordatorios persistentes y deduplicados. Android llama este
    // endpoint antes de consultar la bandeja para convertirlos en avisos del sistema.
    post("/notificaciones/generar-recordatorios") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val hoy = AppClock.hoy()
        val creadas = transaction {
            var total = 0

            Materias.selectAll()
                .where { Materias.usuarioId eq userId }
                .forEach { materia ->
                    val materiaId = materia[Materias.id].value
                    val dificultad = materia[Materias.dificultad]
                    val anticipacion = diasAnticipacionPorDificultad(dificultad)

                    val examenesMateria = Examenes.selectAll()
                        .where { Examenes.materiaId eq materiaId }
                        .toList()

                    val tieneExamenMes = examenesMateria.any { examen ->
                        val fecha = examen[Examenes.fecha]
                        fecha.year == hoy.year && fecha.month == hoy.month
                    }

                    if (!tieneExamenMes) {
                        if (NotificacionService.crear(
                                usuarioId = userId,
                                titulo = "Configura el examen de ${materia[Materias.nombre]}",
                                contenido = "Registra una fecha para este mes. La usaremos para calcular recordatorios y habilitar tus tarjetas y audios.",
                                tipo = "CONFIGURAR_EXAMEN",
                                referenciaId = materiaId,
                                clave = "CONFIG_EXAMEN_${materiaId}_${hoy.year}_${hoy.monthValue}"
                            )) total++
                    }

                    examenesMateria.forEach { examen ->
                        val fecha = examen[Examenes.fecha]
                        val dias = ChronoUnit.DAYS.between(hoy, fecha)
                        if (dias in 0L..anticipacion.toLong()) {
                            val examenId = examen[Examenes.id].value
                            val meta = minutosIdeales(dificultad, dias)
                            val titulo = if (dias == 0L) {
                                "Examen hoy: ${materia[Materias.nombre]}"
                            } else {
                                "Es momento de estudiar ${materia[Materias.nombre]}"
                            }
                            val contenido = if (dias == 0L) {
                                "Tu examen es hoy. Los materiales quedan bloqueados para evitar estudio de último minuto; confía en lo que preparaste y revisa tus indicaciones."
                            } else {
                                val detalleFecha = if (dias == 1L) "Tu examen es mañana." else "Tu examen es en $dias días."
                                val recurso = when {
                                    dificultad >= 8 -> "Haz una sesión y repasa tanto tarjetas como audios."
                                    dificultad >= 5 -> "Repasa tus tarjetas o escucha uno de tus audios."
                                    else -> "Realiza un repaso breve de tus materiales."
                                }
                                "$detalleFecha Por su dificultad $dificultad/10, intenta estudiar $meta minutos hoy. $recurso"
                            }
                            if (NotificacionService.crear(
                                    usuarioId = userId,
                                    titulo = titulo,
                                    contenido = contenido,
                                    tipo = "RECORDATORIO_EXAMEN",
                                    referenciaId = materiaId,
                                    clave = "EXAMEN_${examenId}_${hoy}"
                                )) total++
                        }
                    }
                }

            Asesorias.selectAll()
                .where {
                    ((Asesorias.alumnoId eq userId) or (Asesorias.asesorId eq userId)) and
                        (Asesorias.estado eq "ACEPTADA")
                }
                .forEach { asesoria ->
                    val dias = ChronoUnit.DAYS.between(hoy, asesoria[Asesorias.fecha])
                    if (dias in 0L..1L) {
                        val asesoriaId = asesoria[Asesorias.id].value
                        val esAsesor = asesoria[Asesorias.asesorId] == userId
                        val otraPersonaId = if (esAsesor) asesoria[Asesorias.alumnoId] else asesoria[Asesorias.asesorId]
                        val otraPersona = Usuarios.selectAll().where { Usuarios.id eq otraPersonaId }
                            .firstOrNull()?.get(Usuarios.nombre) ?: "otro usuario"
                        val disponibilidad = AsesorDisponibilidad.selectAll()
                            .where { AsesorDisponibilidad.id eq asesoria[Asesorias.disponibilidadId] }
                            .firstOrNull()
                        val hora = disponibilidad?.get(AsesorDisponibilidad.horaInicio).orEmpty()
                        val cuando = if (dias == 0L) "hoy" else "mañana"
                        if (NotificacionService.crear(
                                usuarioId = userId,
                                titulo = "Recordatorio de asesoría",
                                contenido = "Tienes una asesoría $cuando a las $hora con $otraPersona.",
                                tipo = "RECORDATORIO_ASESORIA",
                                referenciaId = asesoriaId,
                                clave = "ASESORIA_${asesoriaId}_${userId}_${hoy}"
                            )) total++
                    }
                }
            total
        }
        call.respond(mapOf("creadas" to creadas))
    }
}
