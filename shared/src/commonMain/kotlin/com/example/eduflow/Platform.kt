package com.example.eduflow

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform