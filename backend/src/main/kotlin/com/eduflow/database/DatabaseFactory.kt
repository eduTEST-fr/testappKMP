package com.eduflow.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.eduflow.model.*

object DatabaseFactory {
    fun init() {
        val host     = System.getenv("MYSQLHOST")     ?: "mysql.railway.internal"
        val port     = System.getenv("MYSQLPORT")     ?: "3306"
        val database = System.getenv("MYSQLDATABASE") ?: "railway"
        val user     = System.getenv("MYSQLUSER")     ?: "root"
        val password = System.getenv("MYSQLPASSWORD") ?: "qkEQHAIbXvEaICkzBdxKChtiDQFkSniM"

        Database.connect(
            url      ="jdbc:mysql://root:qkEQHAIbXvEaICkzBdxKChtiDQFkSniM@thomas.proxy.rlwy.net:12461/railway",
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
