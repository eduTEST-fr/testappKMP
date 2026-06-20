package com.example.eduflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.config.ApiConfig
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

@Composable
fun RegisterView(onRegistroExitoso: () -> Unit, onVolver: () -> Unit) {
    val scope    = rememberCoroutineScope()
    val client   = remember { HttpClient() }

    var nombre      by remember { mutableStateOf("") }
    var matricula   by remember { mutableStateOf("") }
    var correo      by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var cargando    by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(Modifier.height(48.dp))

            // Logo
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFDDE8E0)), contentAlignment = Alignment.Center) {
                Text("SF", fontSize = 22.sp, fontWeight = FontWeight.Black, color = VerdePrimario)
            }

            Spacer(Modifier.height(12.dp))
            Text("StudyFlow", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
            Spacer(Modifier.height(28.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Crear cuenta", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = TextoPrimario)
                    Text("Ingresa tus datos institucionales",
                        fontSize = 13.sp, color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))

                    // Campo: Nombre completo
                    CampoTexto("Nombre completo", nombre, "Ej: Juan Pérez") { nombre = it }
                    Spacer(Modifier.height(12.dp))

                    // Campo: Matrícula
                    CampoTexto("Matrícula", matricula, "Ej: 210301001") { matricula = it }
                    Spacer(Modifier.height(12.dp))

                    // Campo: Correo institucional
                    CampoTexto("Correo institucional", correo, "alumno@upt.edu.mx") { correo = it }
                    Spacer(Modifier.height(12.dp))

                    // Campo: Contraseña
                    Text("Contraseña", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = "" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("Mínimo 6 caracteres",
                            color = Color(0xFFBBBBBB), fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdePrimario,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    AnimatedVisibility(visible = errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when {
                                nombre.isEmpty()    -> errorMsg = "Escribe tu nombre"
                                matricula.isEmpty() -> errorMsg = "Escribe tu matrícula"
                                !correo.endsWith("@upt.edu.mx") ->
                                    errorMsg = "El correo debe ser @upt.edu.mx"
                                password.length < 6 -> errorMsg = "Mínimo 6 caracteres"
                                else -> {
                                    cargando = true
                                    scope.launch {
                                        try {
                                            val respuesta = client.post(
                                                "${ApiConfig.BASE_URL}/auth/register"
                                            ) {
                                                contentType(ContentType.Application.Json)
                                                setBody("""{"matricula":"$matricula",""" +
                                                    """"correo":"$correo",""" +
                                                    """"password":"$password",""" +
                                                    """"nombre":"$nombre"}""")
                                            }

                                            val resp = respuesta.bodyAsText()

                                            // Si la URL del backend está mal configurada,
                                            // el servidor responde 404 y lo detectamos aquí
                                            // en vez de mostrar un error genérico confuso.
                                            if (respuesta.status == HttpStatusCode.NotFound) {
                                                errorMsg = "No se encontró el servidor (revisa ApiConfig.BASE_URL)"
                                                cargando = false
                                                return@launch
                                            }

                                            // Extrae token y nombre del JSON
                                            val token = Regex(""""token":"([^"]+)"""")
                                                .find(resp)?.groupValues?.get(1) ?: ""
                                            val nom = Regex(""""nombre":"([^"]+)"""")
                                                .find(resp)?.groupValues?.get(1) ?: nombre

                                            if (token.isNotEmpty()) {
                                                SesionStorage.guardarToken(token, nom)
                                                onRegistroExitoso()
                                            } else {
                                                val err = Regex(""""error":"([^"]+)"""")
                                                    .find(resp)?.groupValues?.get(1)
                                                errorMsg = err ?: "Error al registrar"
                                            }
                                        } catch (e: Exception) {
                                            errorMsg = "Sin conexión al servidor"
                                        }
                                        cargando = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario),
                        enabled = !cargando
                    ) {
                        if (cargando)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        else
                            Text("Crear cuenta", color = Color.White,
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(onClick = onVolver,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("¿Ya tienes cuenta? Iniciar sesión",
                            color = VerdePrimario, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// Componente auxiliar reutilizable para campos de texto
@Composable
fun CampoTexto(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), singleLine = true,
        placeholder = { Text(placeholder, color = Color(0xFFBBBBBB), fontSize = 14.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VerdePrimario,
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}
