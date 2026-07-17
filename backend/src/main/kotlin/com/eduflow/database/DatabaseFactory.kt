package com.eduflow.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.eduflow.model.*

object DatabaseFactory {
    fun init() {
        // Railway inyecta estas variables al enlazar el servicio de MySQL.
        // Los valores de respaldo son solo para correr el backend en local.
        val host     = System.getenv("MYSQLHOST")     ?: "localhost"
        val port     = System.getenv("MYSQLPORT")     ?: "3306"
        val database = System.getenv("MYSQLDATABASE") ?: "railway"
        val user     = System.getenv("MYSQLUSER")     ?: "root"
        val password = System.getenv("MYSQLPASSWORD") ?: ""

        // La URL de conexión se arma con las variables de arriba en vez de
        // quedar fija, así sigue funcionando si Railway cambia el proxy o las credenciales.
        Database.connect(
            url      = "jdbc:mysql://$host:$port/$database",
            driver   = "com.mysql.cj.jdbc.Driver",
            user     = user,
            password = password
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Usuarios, Materias, Examenes,
                Tarjetas, Podcasts, RedApoyo,
                AsesoresPerfil, PeersSolicitudes, PeersRespuestas, PeersCalificaciones,
                AsesorDisponibilidad, Asesorias, SesionesEstudio, AsesoriasCalificaciones, Notificaciones
            )

            // Migración manual: la columna vieja 'audio_url' (TEXT/LONGTEXT con base64)
            // ya no se usa -- el audio ahora se guarda como bytes binarios reales en
            // 'audio_bytes' (LONGBLOB), mucho mas liviano y sin truncamiento.
            // SchemaUtils no elimina ni convierte columnas existentes, por eso este
            // ALTER se ejecuta a mano. Es seguro re-ejecutarlo: si la columna vieja
            // ya no existe, simplemente no hace nada.
            try {
                exec("ALTER TABLE podcasts DROP COLUMN audio_url")
            } catch (e: Exception) { /* la columna ya no existe o la tabla es nueva */ }
            try {
                exec("ALTER TABLE podcasts MODIFY audio_bytes LONGBLOB")
            } catch (e: Exception) { /* columna recien creada por createMissingTablesAndColumns ya es BLOB/LONGBLOB segun el driver */ }
        }
    }
}
