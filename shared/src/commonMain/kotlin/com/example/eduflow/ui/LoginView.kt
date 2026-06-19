//LoginView.kt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
fun LoginView(onLoginExitoso: () -> Unit, onIrARegistro: () -> Unit) {
    val scope    = rememberCoroutineScope()
    val client   = remember { HttpClient() }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verPass  by remember { mutableStateOf(false) }
    var error    by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFDDE8E0)
                ) {}
                Text(
                    "SF",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = VerdePrimario
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "StudyFlow",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = VerdePrimario
            )

            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        "Bienvenido de nuevo,",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrimario
                    )
                    Text(
                        "Autodidacta",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrimario
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Inicia sesión para acceder a tus flujos de estudio.",
                        fontSize = 13.sp,
                        color = TextoSecundario,
                        lineHeight = 19.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Correo Institucional",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoPrimario,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = {
                            Text("alumno@upt.edu.mx",
                                color = Color(0xFFBBBBBB), fontSize = 14.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = VerdePrimario,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor   = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Contraseña",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoPrimario,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (verPass) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { verPass = !verPass }) {
                                Text(
                                    if (verPass) "Ocultar" else "Ver",
                                    fontSize = 12.sp,
                                    color = VerdePrimario
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = VerdePrimario,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor   = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )

                    AnimatedVisibility(visible = error) {
                        Text(
                            "Correo o contraseña incorrectos",
                            color = Color(0xFFB00020),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            cargando = true
                            error = false
                            scope.launch {
                                try {
                                    val resp = client.post(
                                        "${ApiConfig.BASE_URL}/auth/login"
                                    ) {
                                        contentType(ContentType.Application.Json)
                                        setBody("""{"correo":"$email","password":"$password"}""")
                                    }.bodyAsText()

                                    val token = Regex(""""token":"([^"]+)"""")
                                        .find(resp)?.groupValues?.get(1) ?: ""
                                    val nom = Regex(""""nombre":"([^"]+)"""")
                                        .find(resp)?.groupValues?.get(1) ?: ""

                                    if (token.isNotEmpty()) {
                                        SesionStorage.guardarToken(token, nom)
                                        onLoginExitoso()
                                    } else {
                                        error = true
                                    }
                                } catch (e: Exception) {
                                    error = true
                                }
                                cargando = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VerdePrimario
                        ),
                        enabled = !cargando
                    ) {
                        if (cargando)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        else
                            Text(
                                "Iniciar Sesión  →",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = onIrARegistro,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("¿Nuevo en StudyFlow? Crear cuenta",
                            color = VerdePrimario, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Usa tu correo institucional @upt.edu.mx",
                fontSize = 11.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}
