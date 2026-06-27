package com.eduflow.routes

import com.eduflow.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Routing.peersRoutes() {

    // GET /peers/solicitudes — solicitudes abiertas, más recientes primero
    get("/peers/solicitudes") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val lista = transaction {
            PeersSolicitudes
                .innerJoin(Usuarios, { PeersSolicitudes.autorId }, { Usuarios.id })
                .selectAll()
                .where { PeersSolicitudes.estado eq "ABIERTA" }
                .orderBy(PeersSolicitudes.createdAt, SortOrder.DESC)
                .limit(30)
                .map {
                    SolicitudDto(
                        id = it[PeersSolicitudes.id].value,
                        titulo = it[PeersSolicitudes.titulo],
                        descripcion = it[PeersSolicitudes.descripcion],
                        materia = it[PeersSolicitudes.materia] ?: "",
                        estado = it[PeersSolicitudes.estado],
                        tieneImagen = !it[PeersSolicitudes.imagenBase64].isNullOrEmpty(),
                        createdAt = it[PeersSolicitudes.createdAt].toString(),
                        autor = AutorDto(
                            id = it[Usuarios.id].value,
                            nombre = it[Usuarios.nombre],
                            carrera = it[Usuarios.carrera] ?: "",
                            cuatrimestre = it[Usuarios.cuatrimestre],
                            rol = it[Usuarios.rol]
                        )
                    )
                }
        }
        call.respond(lista)
    }

    // POST /peers/solicitudes — crear nueva solicitud
    post("/peers/solicitudes") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<CrearSolicitudRequest>()
        val id = transaction {
            PeersSolicitudes.insertAndGetId {
                it[autorId] = userId
                it[titulo] = req.titulo
                it[descripcion] = req.descripcion
                it[imagenBase64] = req.imagenBase64
                it[materia] = req.materia
            }.value
        }
        call.respond(HttpStatusCode.Created, mapOf("id" to id, "mensaje" to "Solicitud creada"))
    }

    // POST /peers/solicitudes/{id}/cerrar
    // ALUMNO: solo cierra las propias. ASESOR/ADMIN: cierra cualquiera.
    post("/peers/solicitudes/{id}/cerrar") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val rol = obtenerRol(call)
        val solicitudId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@post
        }
        val updated = transaction {
            if (rol == "ASESOR" || rol == "ADMIN") {
                PeersSolicitudes.update({ PeersSolicitudes.id eq solicitudId }) {
                    it[estado] = "CERRADA"
                }
            } else {
                PeersSolicitudes.update({
                    (PeersSolicitudes.id eq solicitudId) and (PeersSolicitudes.autorId eq userId)
                }) { it[estado] = "CERRADA" }
            }
        }
        if (updated == 0)
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
        else
            call.respond(mapOf("mensaje" to "Solicitud cerrada"))
    }

    // DELETE /peers/solicitudes/{id} — eliminar solicitud propia
    delete("/peers/solicitudes/{id}") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@delete
        }
        val solicitudId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@delete
        }
        val deleted = transaction {
            PeersSolicitudes.deleteWhere {
                (PeersSolicitudes.id eq solicitudId) and (PeersSolicitudes.autorId eq userId)
            }
        }
        if (deleted == 0)
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
        else
            call.respond(mapOf("mensaje" to "Solicitud eliminada"))
    }

    // GET /peers/solicitudes/{id} — detalle + respuestas
    get("/peers/solicitudes/{id}") {
        val solicitudId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val detalle = transaction {
            val sol = PeersSolicitudes
                .innerJoin(Usuarios, { PeersSolicitudes.autorId }, { Usuarios.id })
                .selectAll()
                .where { PeersSolicitudes.id eq solicitudId }
                .firstOrNull() ?: return@transaction null

            val respuestas = PeersRespuestas
                .innerJoin(Usuarios, { PeersRespuestas.autorId }, { Usuarios.id })
                .selectAll()
                .where { PeersRespuestas.solicitudId eq solicitudId }
                .orderBy(PeersRespuestas.createdAt, SortOrder.ASC)
                .map {
                    RespuestaDto(
                        id = it[PeersRespuestas.id].value,
                        contenido = it[PeersRespuestas.contenido],
                        imagenBase64 = it[PeersRespuestas.imagenBase64] ?: "",
                        createdAt = it[PeersRespuestas.createdAt].toString(),
                        autor = AutorDto(
                            id = it[Usuarios.id].value,
                            nombre = it[Usuarios.nombre],
                            cuatrimestre = it[Usuarios.cuatrimestre],
                            rol = it[Usuarios.rol]
                        )
                    )
                }

            SolicitudDetalleDto(
                id = sol[PeersSolicitudes.id].value,
                titulo = sol[PeersSolicitudes.titulo],
                descripcion = sol[PeersSolicitudes.descripcion],
                imagenBase64 = sol[PeersSolicitudes.imagenBase64] ?: "",
                materia = sol[PeersSolicitudes.materia] ?: "",
                estado = sol[PeersSolicitudes.estado],
                createdAt = sol[PeersSolicitudes.createdAt].toString(),
                autor = AutorDto(
                    id = sol[Usuarios.id].value,
                    nombre = sol[Usuarios.nombre],
                    cuatrimestre = sol[Usuarios.cuatrimestre],
                    rol = sol[Usuarios.rol]
                ),
                respuestas = respuestas
            )
        }
        if (detalle == null) call.respond(HttpStatusCode.NotFound)
        else call.respond(detalle)
    }

    // POST /peers/solicitudes/{id}/responder
    // ALUMNO: cuatrimestre >= autor. ASESOR/ADMIN: sin restricción.
    post("/peers/solicitudes/{id}/responder") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val rolUsuario = obtenerRol(call)
        val solicitudId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@post
        }
        val req = call.receive<ResponderSolicitudRequest>()

        if (rolUsuario == "ALUMNO") {
            val puede = transaction {
                val cuatriRespondedor = Usuarios.selectAll()
                    .where { Usuarios.id eq userId }
                    .firstOrNull()?.get(Usuarios.cuatrimestre) ?: 1
                val cuatriAutor = PeersSolicitudes
                    .innerJoin(Usuarios, { PeersSolicitudes.autorId }, { Usuarios.id })
                    .selectAll()
                    .where { PeersSolicitudes.id eq solicitudId }
                    .firstOrNull()?.get(Usuarios.cuatrimestre) ?: 1
                cuatriRespondedor >= cuatriAutor
            }
            if (!puede) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to
                    "Solo puedes responder solicitudes de alumnos de tu cuatrimestre o inferior"))
                return@post
            }
        }

        val id = transaction {
            PeersRespuestas.insertAndGetId {
                it[PeersRespuestas.solicitudId] = solicitudId
                it[autorId] = userId
                it[contenido] = req.contenido
                it[imagenBase64] = req.imagenBase64
            }.value
        }
        call.respond(HttpStatusCode.Created, mapOf("id" to id, "mensaje" to "Respuesta publicada"))
    }

    // GET /peers/mentores/destacados
    get("/peers/mentores/destacados") {
        val mentores = transaction {
            PeersCalificaciones
                .innerJoin(Usuarios, { PeersCalificaciones.asesorId }, { Usuarios.id })
                .select(
                    PeersCalificaciones.asesorId,
                    Usuarios.nombre, Usuarios.carrera, Usuarios.cuatrimestre,
                    Usuarios.materiasDestaca, Usuarios.rol, Usuarios.avatarId,
                    PeersCalificaciones.estrellas.avg(),
                    PeersCalificaciones.asesorId.count()
                )
                .groupBy(PeersCalificaciones.asesorId)
                .orderBy(PeersCalificaciones.estrellas.avg(), SortOrder.DESC)
                .limit(10)
                .map { row ->
                    val uid = row[PeersCalificaciones.asesorId]
                    val permiteAsesoria = AsesoresPerfil.selectAll()
                        .where { AsesoresPerfil.usuarioId eq uid }
                        .firstOrNull()?.get(AsesoresPerfil.permiteAsesoria) ?: false
                    MentorDto(
                        id = uid,
                        nombre = row[Usuarios.nombre],
                        carrera = row[Usuarios.carrera] ?: "",
                        cuatrimestre = row[Usuarios.cuatrimestre],
                        materiasDestaca = row[Usuarios.materiasDestaca] ?: "",
                        rol = row[Usuarios.rol],
                        avatarId = row[Usuarios.avatarId],
                        promedio = row[PeersCalificaciones.estrellas.avg()]?.toDouble() ?: 0.0,
                        totalCalif = row[PeersCalificaciones.asesorId.count()].toInt(),
                        permiteAsesoria = permiteAsesoria
                    )
                }
        }
        call.respond(mentores)
    }

    // POST /peers/calificar — calificar a un mentor (1-5 estrellas)
    post("/peers/calificar") {
        val alumnoId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@post
        }
        val req = call.receive<CalificarMentorRequest>()
        if (req.estrellas !in 1..5) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estrellas debe ser de 1 a 5"))
            return@post
        }
        transaction {
            PeersCalificaciones.deleteWhere {
                (PeersCalificaciones.alumnoId eq alumnoId) and
                    (PeersCalificaciones.solicitudId eq req.solicitudId)
            }
            PeersCalificaciones.insert {
                it[asesorId] = req.asesorId
                it[PeersCalificaciones.alumnoId] = alumnoId
                it[solicitudId] = req.solicitudId
                it[estrellas] = req.estrellas
            }
        }
        call.respond(mapOf("mensaje" to "Calificación registrada"))
    }

    // GET /peers/perfil/{id}
    get("/peers/perfil/{id}") {
        val targetId = call.parameters["id"]?.toIntOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            return@get
        }
        val perfil = transaction {
            val u = Usuarios.selectAll().where { Usuarios.id eq targetId }.firstOrNull()
                ?: return@transaction null
            val asesor = AsesoresPerfil.selectAll()
                .where { AsesoresPerfil.usuarioId eq targetId }.firstOrNull()
            PerfilDto(
                id = u[Usuarios.id].value,
                nombre = u[Usuarios.nombre],
                carrera = u[Usuarios.carrera] ?: "",
                cuatrimestre = u[Usuarios.cuatrimestre],
                sobreMi = u[Usuarios.sobreMi] ?: "",
                materiasDestaca = u[Usuarios.materiasDestaca] ?: "",
                rol = u[Usuarios.rol],
                avatarId = u[Usuarios.avatarId],
                grado = asesor?.get(AsesoresPerfil.grado) ?: "",
                especialidad = asesor?.get(AsesoresPerfil.especialidad) ?: "",
                permiteAsesoria = asesor?.get(AsesoresPerfil.permiteAsesoria) ?: false
            )
        }
        if (perfil == null) call.respond(HttpStatusCode.NotFound)
        else call.respond(perfil)
    }

    // GET /peers/perfil
    get("/peers/perfil") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@get
        }
        val perfil = transaction {
            val u = Usuarios.selectAll().where { Usuarios.id eq userId }.firstOrNull()
                ?: return@transaction null
            val asesor = AsesoresPerfil.selectAll()
                .where { AsesoresPerfil.usuarioId eq userId }.firstOrNull()
            PerfilDto(
                id = u[Usuarios.id].value,
                nombre = u[Usuarios.nombre],
                carrera = u[Usuarios.carrera] ?: "",
                cuatrimestre = u[Usuarios.cuatrimestre],
                sobreMi = u[Usuarios.sobreMi] ?: "",
                materiasDestaca = u[Usuarios.materiasDestaca] ?: "",
                rol = u[Usuarios.rol],
                avatarId = u[Usuarios.avatarId],
                grado = asesor?.get(AsesoresPerfil.grado) ?: "",
                especialidad = asesor?.get(AsesoresPerfil.especialidad) ?: "",
                permiteAsesoria = asesor?.get(AsesoresPerfil.permiteAsesoria) ?: false
            )
        }
        if (perfil == null) call.respond(HttpStatusCode.NotFound)
        else call.respond(perfil)
    }

    // PUT /peers/perfil
    put("/peers/perfil") {
        val userId = obtenerUserId(call) ?: run {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
            return@put
        }
        val rolUsuario = obtenerRol(call)
        val req = call.receive<ActualizarPerfilRequest>()
        transaction {
            Usuarios.update({ Usuarios.id eq userId }) {
                req.carrera?.let { v -> it[carrera] = v }
                req.cuatrimestre?.let { v -> it[cuatrimestre] = v }
                req.sobreMi?.let { v -> it[sobreMi] = v }
                req.materiasDestaca?.let { v -> it[materiasDestaca] = v }
                req.avatarId?.let { v -> it[avatarId] = v }
            }
            if (rolUsuario == "ASESOR" &&
                (req.grado != null || req.especialidad != null || req.permiteAsesoria != null)) {
                val existe = AsesoresPerfil.selectAll()
                    .where { AsesoresPerfil.usuarioId eq userId }.count() > 0
                if (existe) {
                    AsesoresPerfil.update({ AsesoresPerfil.usuarioId eq userId }) {
                        req.grado?.let { v -> it[grado] = v }
                        req.especialidad?.let { v -> it[especialidad] = v }
                        req.permiteAsesoria?.let { v -> it[permiteAsesoria] = v }
                    }
                } else {
                    AsesoresPerfil.insert {
                        it[usuarioId] = userId
                        it[grado] = req.grado ?: "LICENCIATURA"
                        it[especialidad] = req.especialidad
                        it[permiteAsesoria] = req.permiteAsesoria ?: false
                    }
                }
            }
        }
        call.respond(mapOf("mensaje" to "Perfil actualizado"))
    }

    // GET /peers/asesores — TODOS los asesores ordenados por calificación promedio
    get("/peers/asesores") {
        val asesores = transaction {
            val subquery = PeersCalificaciones
                .select(PeersCalificaciones.asesorId,
                    PeersCalificaciones.estrellas.avg().alias("prom"),
                    PeersCalificaciones.asesorId.count().alias("total"))
                .groupBy(PeersCalificaciones.asesorId)
                .alias("califs")

            Usuarios.selectAll()
                .where { Usuarios.rol eq "ASESOR" }
                .orderBy(Usuarios.nombre, SortOrder.ASC)
                .map { u ->
                    val uid = u[Usuarios.id].value
                    val calif = PeersCalificaciones
                        .selectAll().where { PeersCalificaciones.asesorId eq uid }
                    val prom = calif.mapNotNull { it[PeersCalificaciones.estrellas].toDouble() }
                        .let { if (it.isEmpty()) 0.0 else it.average() }
                    val total = calif.count().toInt()
                    val ap = AsesoresPerfil.selectAll()
                        .where { AsesoresPerfil.usuarioId eq uid }.firstOrNull()
                    MentorDto(
                        id = uid,
                        nombre = u[Usuarios.nombre],
                        carrera = u[Usuarios.carrera] ?: "",
                        cuatrimestre = u[Usuarios.cuatrimestre],
                        materiasDestaca = u[Usuarios.materiasDestaca] ?: "",
                        rol = u[Usuarios.rol],
                        avatarId = u[Usuarios.avatarId],
                        promedio = prom,
                        totalCalif = total,
                        permiteAsesoria = ap?.get(AsesoresPerfil.permiteAsesoria) ?: false
                    )
                }
                .sortedByDescending { it.promedio }
        }
        call.respond(asesores)
    }
}
