package com.example.eduflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.eduflow.data.CatalogoUPT
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

@Composable
fun RegisterView(onRegistroExitoso: () -> Unit, onVolver: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }

    var nombre       by remember { mutableStateOf("") }
    var matricula    by remember { mutableStateOf("") }
    var correo       by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var carrera      by remember { mutableStateOf(CatalogoUPT.nombresCarreras.first()) }
    var cuatrimestre by remember { mutableStateOf(1) }
    var esAsesor     by remember { mutableStateOf(false) }
    var codigoAsesor by remember { mutableStateOf("") }
    var cargando     by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Beige)
        .windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(Modifier.height(48.dp))

            EduFlowMark(modifier = Modifier.size(68.dp))

            Spacer(Modifier.height(12.dp))
            Text("EduFlow", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
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

                    CampoTexto("Nombre completo", nombre, "Ej: Juan Pérez") { nombre = it }
                    Spacer(Modifier.height(12.dp))
                    CampoTexto("Matrícula", matricula, "Ej: 210301001") { matricula = it }
                    Spacer(Modifier.height(12.dp))
                    CampoTexto("Correo institucional", correo, "alumno@upt.edu.mx") { correo = it }
                    Spacer(Modifier.height(12.dp))

                    SelectorCarrera(carrera) { carrera = it }
                    Spacer(Modifier.height(12.dp))

                    SelectorCuatrimestre(cuatrimestre) { cuatrimestre = it }
                    Spacer(Modifier.height(12.dp))

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

                    Spacer(Modifier.height(16.dp))

                    // Toggle ¿eres asesor?
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { esAsesor = !esAsesor; errorMsg = "" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = esAsesor,
                            onCheckedChange = { esAsesor = it; errorMsg = "" },
                            colors = CheckboxDefaults.colors(checkedColor = VerdePrimario)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Registrarme como Asesor", fontSize = 13.sp, color = TextoPrimario)
                    }

                    AnimatedVisibility(visible = esAsesor) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Text("Código de asesor", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = TextoPrimario,
                                modifier = Modifier.padding(bottom = 6.dp))
                            OutlinedTextField(
                                value = codigoAsesor,
                                onValueChange = { codigoAsesor = it; errorMsg = "" },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                placeholder = { Text("Código proporcionado por coordinación",
                                    color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VerdePrimario,
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                            Text(
                                "Si el código es incorrecto se creará una cuenta de Alumno.",
                                fontSize = 11.sp, color = TextoSecundario,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

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
                                            val codigoPart = if (esAsesor && codigoAsesor.isNotBlank())
                                                ",\"codigoAsesor\":\"${codigoAsesor.trim()}\"" else ""
                                            val respuesta = client.post(
                                                "${ApiConfig.BASE_URL}/auth/register"
                                            ) {
                                                contentType(ContentType.Application.Json)
                                                setBody("""{"matricula":"$matricula","correo":"$correo","password":"$password","nombre":"$nombre"$codigoPart}""")
                                            }

                                            val resp = respuesta.bodyAsText()

                                            if (respuesta.status == HttpStatusCode.NotFound) {
                                                errorMsg = "No se encontró el servidor (revisa ApiConfig.BASE_URL)"
                                                cargando = false
                                                return@launch
                                            }

                                            val token = Regex(""""token":"([^"]+)"""")
                                                .find(resp)?.groupValues?.get(1) ?: ""
                                            val nom = Regex(""""nombre":"([^"]+)"""")
                                                .find(resp)?.groupValues?.get(1) ?: nombre
                                            val rolResp = Regex(""""rol":"([^"]+)"""")
                                                .find(resp)?.groupValues?.get(1) ?: "ALUMNO"

                                            if (token.isNotEmpty()) {
                                                SesionStorage.guardarToken(token, nom, rolResp)
                                                try {
                                                    client.put("${ApiConfig.BASE_URL}/peers/perfil") {
                                                        header("Authorization", "Bearer $token")
                                                        contentType(ContentType.Application.Json)
                                                        setBody("{\"carrera\":\"${escaparJson(carrera)}\",\"cuatrimestre\":$cuatrimestre}")
                                                    }
                                                } catch (e: Exception) { /* el perfil se puede completar despues */ }
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

                    TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
                        Text("¿Ya tienes cuenta? Iniciar sesión",
                            color = VerdePrimario, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

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

// Selector de carrera UPT: lista desplegable con las carreras del catálogo fijo.
@Composable
fun SelectorCarrera(carreraSeleccionada: String, onSeleccionar: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Text("Carrera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
    Box {
        OutlinedTextField(
            value = carreraSeleccionada,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clickable { expandido = true },
            enabled = false,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color(0xFFE0E0E0),
                disabledTextColor = TextoPrimario,
                disabledContainerColor = Color.White
            )
        )
        // Capa transparente clickeable encima del campo deshabilitado para abrir el menú
        Box(modifier = Modifier.matchParentSize().clickable { expandido = true })
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            com.example.eduflow.data.CatalogoUPT.nombresCarreras.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, fontSize = 13.sp) },
                    onClick = { onSeleccionar(opcion); expandido = false }
                )
            }
        }
    }
}

// Selector de cuatrimestre: únicamente del 1 al 10 (duración real del plan de
// estudios de la UPT), para no permitir valores fuera de rango como texto libre.
@Composable
fun SelectorCuatrimestre(cuatrimestreSeleccionado: Int, onSeleccionar: (Int) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Text("Cuatrimestre", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = TextoPrimario, modifier = Modifier.padding(bottom = 6.dp))
    Box {
        OutlinedTextField(
            value = "${cuatrimestreSeleccionado}° cuatrimestre",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clickable { expandido = true },
            enabled = false,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color(0xFFE0E0E0),
                disabledTextColor = TextoPrimario,
                disabledContainerColor = Color.White
            )
        )
        Box(modifier = Modifier.matchParentSize().clickable { expandido = true })
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            com.example.eduflow.data.CatalogoUPT.cuatrimestres.forEach { n ->
                DropdownMenuItem(
                    text = { Text("${n}° cuatrimestre", fontSize = 13.sp) },
                    onClick = { onSeleccionar(n); expandido = false }
                )
            }
        }
    }
}
