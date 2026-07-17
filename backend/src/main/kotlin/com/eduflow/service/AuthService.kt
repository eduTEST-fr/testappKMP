package com.eduflow.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

object AuthService {
    private val secret = System.getenv("JWT_SECRET") ?: "dev_secret"
    private val algorithm = Algorithm.HMAC256(secret)

    fun hashPassword(plain: String): String =
        BCrypt.hashpw(plain, BCrypt.gensalt())

    fun verificarPassword(plain: String, hash: String): Boolean =
        BCrypt.checkpw(plain, hash)

    fun generarToken(userId: Int, rol: String = "ALUMNO"): String = JWT.create()
        .withClaim("userId", userId)
        .withClaim("rol", rol)
        .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000)) // 24h
        .sign(algorithm)

    fun obtenerRol(token: String): String = try {
        JWT.require(algorithm).build().verify(token).getClaim("rol").asString() ?: "ALUMNO"
    } catch (e: Exception) { "ALUMNO" }

    fun verificarToken(token: String): Int? = try {
        JWT.require(algorithm).build().verify(token).getClaim("userId").asInt()
    } catch (e: Exception) { null }
}
