package com.eduflow.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.eduflow.model.*

object DatabaseFactory {
    fun init() {
        // Railway inyecta estas variables automáticamente al conectar el servicio MySQL.
        // Los valores de respaldo (?:) solo sirven para pruebas locales, nunca se usan en producción.
        val host     = System.getenv("MYSQLHOST")     ?: "thomas.proxy.rlwy.net"
        val port     = System.getenv("MYSQLPORT")     ?: "12461"
        val database = System.getenv("MYSQLDATABASE") ?: "railway"
        val user     = System.getenv("MYSQLUSER")     ?: "root"
        val password = System.getenv("MYSQLPASSWORD") ?: "qkEQHAIbXvEaICkzBdxKChtiDQFkSniM"

        // IMPORTANTE: la URL se construye con las variables de arriba.
        // Antes estaba hardcodeada con un host/puerto fijo y eso es lo que
        // causaba inconsistencias si Railway cambiaba el proxy o las credenciales.
        Database.connect(
            url      = "jdbc:mysql://$host:$port/$database",
            driver   = "com.mysql.cj.jdbc.Driver",
            user     = user,
            password = password
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Usuarios, Materias, Examenes,
                Tarjetas, Podcasts, RedApoyo
            )
        }
    }
}
