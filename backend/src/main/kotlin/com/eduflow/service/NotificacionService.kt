package com.eduflow.service

import com.eduflow.model.Notificaciones
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/** Utilidad central para que todos los módulos generen avisos consistentes. */
object NotificacionService {
    fun crear(
        usuarioId: Int,
        titulo: String,
        contenido: String,
        tipo: String = "GENERAL",
        referenciaId: Int? = null,
        clave: String? = null
    ): Boolean {
        if (clave != null) {
            val existe = Notificaciones.selectAll()
                .where { Notificaciones.clave eq clave }
                .limit(1)
                .any()
            if (existe) return false
        }

        Notificaciones.insert {
            it[Notificaciones.usuarioId] = usuarioId
            it[Notificaciones.titulo] = titulo.take(200)
            it[Notificaciones.contenido] = contenido
            it[Notificaciones.tipo] = tipo.take(40)
            it[Notificaciones.referenciaId] = referenciaId
            it[Notificaciones.clave] = clave
        }
        return true
    }
}
