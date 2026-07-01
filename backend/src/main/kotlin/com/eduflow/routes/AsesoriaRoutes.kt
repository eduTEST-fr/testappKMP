package com.eduflow.routes

import com.eduflow.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import java.time.LocalDate

private fun crearNotificacion(usuarioId: Int, titulo: String, contenido: String) {
    Notificaciones.insert {
        it[Notificaciones.usuarioId] = usuarioId
        it[Notificaciones.titulo] = titulo
        it[Notificaciones.contenido] = contenido
    }
}

fun Routing.asesoriaRoutes() {

    // PUT /asesorias/disponibilidad — el asesor reemplaza su disponibilidad semanal completa
    put("/asesorias/disponibilidad") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@put
        }
        val rol = obtenerRol(call)
        if (rol != "ASESOR") {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Solo los asesores configuran disponibilidad"))
            return@put
        }
        val req = call.receive<GuardarDisponibilidadRequest>()
        transaction {
            AsesorDisponibilidad.deleteWhere { AsesorDisponibilidad.asesorId eq userId }
            req.horarios.forEach { h ->
                AsesorDisponibilidad.insert {
                    it[asesorId] = userId
                    it[diaSemana] = h.diaSemana
                    it[horaInicio] = h.horaInicio
                    it[horaFin] = h.horaFin
                }
            }
        }
        call.respond(mapOf("mensaje" to "Disponibilidad actualizada"))
    }

    // GET /asesorias/disponibilidad/mia — disponibilidad propia del asesor (para editar en su perfil)
    get("/asesorias/disponibilidad/mia") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val lista = transaction {
            AsesorDisponibilidad.selectAll()
                .where { AsesorDisponibilidad.asesorId eq userId }
                .orderBy(AsesorDisponibilidad.diaSemana, SortOrder.ASC)
                .map {
                    DisponibilidadDto(
                        id = it[AsesorDisponibilidad.id].value,
                        diaSemana = it[AsesorDisponibilidad.diaSemana],
                        horaInicio = it[AsesorDisponibilidad.horaInicio],
                        horaFin = it[AsesorDisponibilidad.horaFin]
                    )
                }
        }
        call.respond(lista)
    }

    // GET /asesorias/disponibilidad/{asesorId}?fecha=YYYY-MM-DD
    // Horarios del asesor; si se manda fecha, marca como ocupados los que ya tienen
    // una asesoría PENDIENTE o ACEPTADA en esa fecha.
    get("/asesorias/disponibilidad/{asesorId}") {
        val asesorId = call.parameters["asesorId"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val fechaStr = call.request.queryParameters["fecha"]
        val lista = transaction {
            val ocupadosIds: Set<Int> = if (fechaStr != null) {
                try {
                    val fecha = LocalDate.parse(fechaStr)
                    Asesorias.selectAll()
                        .where {
                            (Asesorias.asesorId eq asesorId) and
                                (Asesorias.fecha eq fecha) and
                                (Asesorias.estado neq "CANCELADA")
                        }
                        .map { it[Asesorias.disponibilidadId] }
                        .toSet()
                } catch (e: Exception) { emptySet() }
            } else emptySet()

            AsesorDisponibilidad.selectAll()
                .where { AsesorDisponibilidad.asesorId eq asesorId }
                .orderBy(AsesorDisponibilidad.diaSemana, SortOrder.ASC)
                .map {
                    val id = it[AsesorDisponibilidad.id].value
                    DisponibilidadDto(
                        id = id,
                        diaSemana = it[AsesorDisponibilidad.diaSemana],
                        horaInicio = it[AsesorDisponibilidad.horaInicio],
                        horaFin = it[AsesorDisponibilidad.horaFin],
                        ocupado = id in ocupadosIds
                    )
                }
        }
        call.respond(lista)
    }

    // POST /asesorias — el alumno agenda una asesoría (queda en PENDIENTE)
    post("/asesorias") {
        val alumnoId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<CrearAsesoriaRequest>()
        val fecha = try { LocalDate.parse(req.fecha) } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Fecha inválida"))
            return@post
        }

        val resultado = transaction {
            val disponible = AsesorDisponibilidad.selectAll()
                .where { AsesorDisponibilidad.id eq req.disponibilidadId }
                .firstOrNull()
            if (disponible == null) return@transaction "NO_EXISTE"

            val ocupado = Asesorias.selectAll()
                .where {
                    (Asesorias.disponibilidadId eq req.disponibilidadId) and
                        (Asesorias.fecha eq fecha) and
                        (Asesorias.estado neq "CANCELADA")
                }.count() > 0
            if (ocupado) return@transaction "OCUPADO"

            Asesorias.insert {
                it[asesorId] = req.asesorId
                it[Asesorias.alumnoId] = alumnoId
                it[disponibilidadId] = req.disponibilidadId
                it[Asesorias.fecha] = fecha
            }

            // Avisamos al asesor: tiene una nueva solicitud esperando respuesta.
            val nombreAlumno = Usuarios.selectAll().where { Usuarios.id eq alumnoId }
                .firstOrNull()?.get(Usuarios.nombre) ?: "Un alumno"
            crearNotificacion(
                usuarioId = req.asesorId,
                titulo = "Nueva solicitud de asesoría",
                contenido = "$nombreAlumno solicitó una asesoría contigo para el $fecha"
            )
            "OK"
        }

        when (resultado) {
            "NO_EXISTE" -> call.respond(HttpStatusCode.NotFound, mapOf("error" to "Horario no encontrado"))
            "OCUPADO" -> call.respond(HttpStatusCode.Conflict, mapOf("error" to "Ese horario ya fue tomado"))
            else -> call.respond(HttpStatusCode.Created, mapOf("mensaje" to "Solicitud de asesoría enviada"))
        }
    }

    // GET /asesorias/mis-asesorias — asesorías del usuario logueado (como alumno o asesor)
    get("/asesorias/mis-asesorias") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val lista = transaction {
            Asesorias.selectAll()
                .where { (Asesorias.asesorId eq userId) or (Asesorias.alumnoId eq userId) }
                .orderBy(Asesorias.fecha, SortOrder.DESC)
                .map { row -> mapearAsesoria(row) }
        }
        call.respond(lista)
    }

    // PUT /asesorias/{id}/aceptar — el asesor acepta y manda notificación al alumno
    put("/asesorias/{id}/aceptar") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@put
        }
        val asesoriaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@put
        }
        val req = call.receive<AceptarAsesoriaRequest>()

        val ok = transaction {
            val asesoria = Asesorias.selectAll()
                .where { (Asesorias.id eq asesoriaId) and (Asesorias.asesorId eq userId) }
                .firstOrNull() ?: return@transaction false

            Asesorias.update({ Asesorias.id eq asesoriaId }) {
                it[estado] = "ACEPTADA"
                it[mensajeAsesor] = req.mensajeAsesor
                it[enlace] = req.enlace
                it[ubicacion] = req.ubicacion
            }

            val nombreAsesor = Usuarios.selectAll().where { Usuarios.id eq userId }
                .firstOrNull()?.get(Usuarios.nombre) ?: "Tu asesor"
            val detalle = listOfNotNull(req.ubicacion, req.enlace, req.mensajeAsesor)
                .firstOrNull()?.let { " — $it" } ?: ""
            crearNotificacion(
                usuarioId = asesoria[Asesorias.alumnoId],
                titulo = "Asesoría confirmada",
                contenido = "$nombreAsesor aceptó tu solicitud de asesoría$detalle"
            )
            true
        }
        if (!ok) call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
        else call.respond(mapOf("mensaje" to "Asesoría aceptada"))
    }

    // PUT /asesorias/{id}/cancelar — el asesor cancela y notifica al alumno
    put("/asesorias/{id}/cancelar") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@put
        }
        val asesoriaId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@put
        }
        val ok = transaction {
            val asesoria = Asesorias.selectAll()
                .where { (Asesorias.id eq asesoriaId) and (Asesorias.asesorId eq userId) }
                .firstOrNull() ?: return@transaction false

            Asesorias.update({ Asesorias.id eq asesoriaId }) { it[estado] = "CANCELADA" }

            val nombreAsesor = Usuarios.selectAll().where { Usuarios.id eq userId }
                .firstOrNull()?.get(Usuarios.nombre) ?: "Tu asesor"
            crearNotificacion(
                usuarioId = asesoria[Asesorias.alumnoId],
                titulo = "Asesoría cancelada",
                contenido = "$nombreAsesor canceló la asesoría solicitada. Puedes agendar otro horario."
            )
            true
        }
        if (!ok) call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
        else call.respond(mapOf("mensaje" to "Asesoría cancelada"))
    }

    // GET /notificaciones — notificaciones del usuario logueado, más recientes primero
    get("/notificaciones") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val lista = transaction {
            Notificaciones.selectAll()
                .where { Notificaciones.usuarioId eq userId }
                .orderBy(Notificaciones.createdAt, SortOrder.DESC)
                .limit(50)
                .map {
                    NotificacionDto(
                        id = it[Notificaciones.id].value,
                        titulo = it[Notificaciones.titulo],
                        contenido = it[Notificaciones.contenido],
                        leida = it[Notificaciones.leida],
                        createdAt = it[Notificaciones.createdAt].toString()
                    )
                }
        }
        call.respond(lista)
    }

    // PUT /notificaciones/{id}/leer
    put("/notificaciones/{id}/leer") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@put
        }
        val notifId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@put
        }
        transaction {
            Notificaciones.update({ (Notificaciones.id eq notifId) and (Notificaciones.usuarioId eq userId) }) {
                it[leida] = true
            }
        }
        call.respond(mapOf("mensaje" to "Notificación marcada como leída"))
    }
}

// Convierte una fila de Asesorias (+ joins manuales) en su DTO de respuesta.
private fun mapearAsesoria(row: ResultRow): AsesoriaDto {
    val disponibilidadId = row[Asesorias.disponibilidadId]
    val disp = AsesorDisponibilidad.selectAll()
        .where { AsesorDisponibilidad.id eq disponibilidadId }.firstOrNull()
    val asesorId = row[Asesorias.asesorId]
    val alumnoId = row[Asesorias.alumnoId]
    val asesorRow = Usuarios.selectAll().where { Usuarios.id eq asesorId }.firstOrNull()
    val alumnoRow = Usuarios.selectAll().where { Usuarios.id eq alumnoId }.firstOrNull()
    return AsesoriaDto(
        id = row[Asesorias.id].value,
        asesor = AutorDto(
            id = asesorId,
            nombre = asesorRow?.get(Usuarios.nombre) ?: "",
            carrera = asesorRow?.get(Usuarios.carrera) ?: "",
            cuatrimestre = asesorRow?.get(Usuarios.cuatrimestre) ?: 1,
            rol = "ASESOR"
        ),
        alumno = AutorDto(
            id = alumnoId,
            nombre = alumnoRow?.get(Usuarios.nombre) ?: "",
            carrera = alumnoRow?.get(Usuarios.carrera) ?: "",
            cuatrimestre = alumnoRow?.get(Usuarios.cuatrimestre) ?: 1,
            rol = "ALUMNO"
        ),
        fecha = row[Asesorias.fecha].toString(),
        horaInicio = disp?.get(AsesorDisponibilidad.horaInicio) ?: "",
        horaFin = disp?.get(AsesorDisponibilidad.horaFin) ?: "",
        estado = row[Asesorias.estado],
        mensajeAsesor = row[Asesorias.mensajeAsesor] ?: "",
        enlace = row[Asesorias.enlace] ?: "",
        ubicacion = row[Asesorias.ubicacion] ?: "",
        createdAt = row[Asesorias.createdAt].toString()
    )
}
