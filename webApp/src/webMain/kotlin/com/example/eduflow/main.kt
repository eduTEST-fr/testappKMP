package com.example.eduflow

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsModule

// Fuerza la carga del módulo @js-joda/timezone en el bundle antes de que
// arranque la app. Sin esta referencia explícita, kotlinx-datetime no
// puede resolver TimeZone.currentSystemDefault() en el navegador y lanza
// IllegalTimeZoneException (ver docs oficiales de kotlinx-datetime para wasmJs).
@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule
private val jsJodaTz = JsJodaTimeZoneModule

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App()
    }
}