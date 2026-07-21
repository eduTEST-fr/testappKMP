package com.eduflow.routes

import com.eduflow.model.Examenes
import com.eduflow.model.Materias
import com.eduflow.util.AppClock
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

enum class MotivoBloqueoMaterial {
    NINGUNO,
    MATERIA_NO_DISPONIBLE,
    SIN_EXAMEN_MENSUAL,
    EXAMEN_HOY
}

data class EstadoAccesoMaterial(
    val permitido: Boolean,
    val motivo: MotivoBloqueoMaterial,
    val proximoExamen: LocalDate? = null
)

fun evaluarAccesoMateriales(userId: Int, materiaId: Int, hoy: LocalDate = AppClock.hoy()): EstadoAccesoMaterial = transaction {
    val materiaPropia = Materias.selectAll()
        .where { (Materias.id eq materiaId) and (Materias.usuarioId eq userId) }
        .firstOrNull() ?: return@transaction EstadoAccesoMaterial(false, MotivoBloqueoMaterial.MATERIA_NO_DISPONIBLE)

    val fechas = Examenes.selectAll()
        .where { Examenes.materiaId eq materiaPropia[Materias.id].value }
        .map { it[Examenes.fecha] }

    val examenesDelMes = fechas.filter { it.year == hoy.year && it.month == hoy.month }
    if (examenesDelMes.isEmpty()) {
        return@transaction EstadoAccesoMaterial(false, MotivoBloqueoMaterial.SIN_EXAMEN_MENSUAL)
    }
    if (examenesDelMes.any { it == hoy }) {
        return@transaction EstadoAccesoMaterial(false, MotivoBloqueoMaterial.EXAMEN_HOY, hoy)
    }

    EstadoAccesoMaterial(
        permitido = true,
        motivo = MotivoBloqueoMaterial.NINGUNO,
        proximoExamen = fechas.filter { !it.isBefore(hoy) }.minOrNull()
    )
}

suspend fun validarAccesoMateriales(call: ApplicationCall, userId: Int, materiaId: Int): Boolean {
    if (obtenerRol(call) != "ALUMNO") {
        call.respond(
            HttpStatusCode.Forbidden,
            mapOf("error" to "Los materiales de estudio son privados del alumno")
        )
        return false
    }

    val estado = evaluarAccesoMateriales(userId, materiaId)
    if (estado.permitido) return true

    when (estado.motivo) {
        MotivoBloqueoMaterial.MATERIA_NO_DISPONIBLE -> call.respond(
            HttpStatusCode.Forbidden,
            mapOf("error" to "La materia no pertenece al usuario")
        )
        MotivoBloqueoMaterial.SIN_EXAMEN_MENSUAL -> call.respond(
            HttpStatusCode.Locked,
            mapOf(
                "error" to "Registra la fecha de un examen para el mes actual antes de usar tarjetas o audios",
                "motivo" to "SIN_EXAMEN_MENSUAL"
            )
        )
        MotivoBloqueoMaterial.EXAMEN_HOY -> call.respond(
            HttpStatusCode.Locked,
            mapOf(
                "error" to "Hoy es día de examen. Los materiales de estudio permanecen bloqueados",
                "motivo" to "EXAMEN_HOY"
            )
        )
        MotivoBloqueoMaterial.NINGUNO -> Unit
    }
    return false
}
