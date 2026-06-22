package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.storage.PerfilStorage
import com.example.eduflow.storage.SesionStorage

// Perfil del usuario para la Red de Apoyo: carrera, cuatrimestre y materias
// en las que destaca. Es solo visual/local por ahora (sin backend), tal
// como se pidió; todo se guarda en el dispositivo via PerfilStorage.
@Composable
fun PerfilView(onVolver: () -> Unit) {
    var editando by remember { mutableStateOf(false) }
    var carrera      by remember { mutableStateOf(PerfilStorage.obtenerCarrera()) }
    var cuatrimestre by remember { mutableStateOf(PerfilStorage.obtenerCuatrimestre()) }
    var ubicacion     by remember { mutableStateOf(PerfilStorage.obtenerUbicacion()) }
    var bio           by remember { mutableStateOf(PerfilStorage.obtenerBio()) }
    var materias      by remember { mutableStateOf(PerfilStorage.obtenerMateriasDestacadas()) }
    var nuevaMateria  by remember { mutableStateOf("") }

    val nombre = SesionStorage.obtenerNombre()

    Box(
        modifier = Modifier.fillMaxSize().background(Beige).windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("Mi Perfil", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }

            // Portada
            Box(
                modifier = Modifier.fillMaxWidth().height(110.dp)
                    .background(Color(0xFFDDE8E0))
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Box(
                    modifier = Modifier.offset(y = (-44).dp)
                ) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(VerdePrimario),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(nombre.take(1).uppercase(), fontSize = 30.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Column(modifier = Modifier.offset(y = (-28).dp)) {
                    Text(nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)

                    if (editando) {
                        Spacer(Modifier.height(10.dp))
                        CampoTexto("Carrera", carrera, "Ej: Ing. en Sistemas") { carrera = it }
                        Spacer(Modifier.height(8.dp))
                        CampoTexto("Cuatrimestre", cuatrimestre, "Ej: 5to Cuatrimestre") { cuatrimestre = it }
                        Spacer(Modifier.height(8.dp))
                        CampoTexto("Ubicación", ubicacion, "Ej: Tulancingo, Hidalgo") { ubicacion = it }
                    } else {
                        Text("🎓  $carrera", fontSize = 13.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 6.dp))
                        Text("📘  $cuatrimestre", fontSize = 13.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 3.dp))
                        Text("📍  $ubicacion", fontSize = 13.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 3.dp))
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (editando) {
                                PerfilStorage.guardarPerfil(carrera.trim(), cuatrimestre.trim(), ubicacion.trim(), bio.trim())
                                PerfilStorage.guardarMateriasDestacadas(materias)
                            }
                            editando = !editando
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                    ) {
                        Text(if (editando) "Guardar cambios" else "Editar Perfil",
                            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Sobre mí
                Text("Sobre mí", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = TextoPrimario, modifier = Modifier.padding(bottom = 8.dp))
                if (editando) {
                    OutlinedTextField(
                        value = bio, onValueChange = { bio = it },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Cuéntale a la comunidad sobre ti...",
                            color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdePrimario,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                } else {
                    Text(
                        bio.ifBlank { "Aún no agregas una descripción. Toca \"Editar Perfil\" para contar algo sobre ti." },
                        fontSize = 13.sp, color = TextoSecundario, lineHeight = 19.sp
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text("Materias en las que destaca", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = TextoPrimario, modifier = Modifier.padding(bottom = 10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (materias.isEmpty()) {
                            Text("Aún no agregas materias destacadas.",
                                fontSize = 12.sp, color = TextoSecundario)
                        } else {
                            FlowFilas(materias) { materia ->
                                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFDDE8E0)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(materia, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                            color = VerdePrimario)
                                        if (editando) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("×", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                                color = VerdePrimario,
                                                modifier = Modifier.clickable {
                                                    materias = materias - materia
                                                })
                                        }
                                    }
                                }
                            }
                        }

                        if (editando) {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = nuevaMateria, onValueChange = { nuevaMateria = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    placeholder = { Text("Ej: Cálculo Multivariado",
                                        color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VerdePrimario,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp), color = VerdePrimario,
                                    modifier = Modifier.clickable {
                                        val m = nuevaMateria.trim()
                                        if (m.isNotEmpty() && m !in materias) materias = materias + m
                                        nuevaMateria = ""
                                    }
                                ) {
                                    Text("Agregar", color = Color.White, fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// Distribuye chips en filas que van saltando de linea segun el ancho
// disponible (sin depender de librerias de layout adicionales).
@Composable
private fun FlowFilas(items: List<String>, contenido: @Composable (String) -> Unit) {
    Column {
        var fila = mutableListOf<String>()
        val filas = mutableListOf<List<String>>()
        var anchoFila = 0
        items.forEach { item ->
            val pesoAprox = item.length + 4
            if (anchoFila + pesoAprox > 32 && fila.isNotEmpty()) {
                filas.add(fila); fila = mutableListOf(); anchoFila = 0
            }
            fila.add(item); anchoFila += pesoAprox
        }
        if (fila.isNotEmpty()) filas.add(fila)

        filas.forEach { f ->
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                f.forEach { contenido(it) }
            }
        }
    }
}
