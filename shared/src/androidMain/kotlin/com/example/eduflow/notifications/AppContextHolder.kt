package com.example.eduflow.notifications

import android.content.Context

// MainActivity llama a init() una sola vez, en onCreate, antes de usar
// cualquier cosa relacionada a notificaciones. Guardamos el contexto de la
// aplicación (no el de la Activity) para evitar fugas de memoria.
object AppContextHolder {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
