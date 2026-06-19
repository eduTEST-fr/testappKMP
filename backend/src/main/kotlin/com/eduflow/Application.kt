package com.eduflow

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.kotlinx.json.*
import com.eduflow.database.DatabaseFactory
import com.eduflow.routes.*

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CORS) {
        anyHost()
        allowHeader("Authorization")
        allowHeader("Content-Type")
    }

    DatabaseFactory.init()

    routing {
        authRoutes()
        materiaRoutes()
        examenRoutes()
        tarjetaRoutes()
        podcastRoutes()
        redApoyoRoutes()
        consejoRoutes()
    }
}
