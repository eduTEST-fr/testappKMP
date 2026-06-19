// ui/HorarioView.kt
package com.example.eduflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.api.ConsejosApi
import com.example.eduflow.service.HorarioService
import com.example.eduflow.storage.HorarioStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val VerdePrimario = Color(0xFF2D5A3D)
val Beige = Color(0xFFF5F0E8)
val TextoPrimario = Color(0xFF1A1A1A)
val TextoSecundario = Color(0xFF6B6B6B)

@Composable
fun HorarioView() {
    val scope = rememberCoroutineScope()
    val servicio = HorarioService()
    var consejo by remember { mutableStateOf("") }
    var recomendacion by remember { mutableStateOf("") }
    var materias by remember { mutableStateOf(HorarioStorage.obtenerMaterias()) }
    var nombreMateria by remember { mutableStateOf("") }
    var dificultad by remember { mutableStateOf("") }
    var materiaGuardada by remember { mutableStateOf("") }
    var dificultadGuardada by remember { mutableStateOf("") }
    var mostrarCard by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "StudyFlow",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = VerdePrimario,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Registro de Materia",
            fontSize = 18.sp,
            color = TextoSecundario,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Nombre de la materia",
            fontSize = 14.sp,
            color = TextoPrimario,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        TextField(
            value = nombreMateria,
            onValueChange = { nombreMateria = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = VerdePrimario,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Text(
            text = "Nivel de dificultad (1-10)",
            fontSize = 14.sp,
            color = TextoPrimario,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        TextField(
            value = dificultad,
            onValueChange = { dificultad = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = VerdePrimario,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Button(
            onClick = {
                if (nombreMateria.isNotEmpty() && dificultad.isNotEmpty()) {
                    materiaGuardada = nombreMateria
                    dificultadGuardada = dificultad
                    recomendacion = servicio.generarRecomendacion(
                        nombreMateria,
                        dificultad.toIntOrNull() ?: 1
                    )
                    mostrarCard = true

                    HorarioStorage.guardarMateria(
                        nombreMateria,
                        dificultad.toIntOrNull() ?: 1
                    )
                    materias = HorarioStorage.obtenerMaterias()

                } else {
                    mostrarCard = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VerdePrimario
            )
        ) {
            Text(
                text = "Registrar Materia",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (mostrarCard) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VerdePrimario),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Materia Registrada",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(text = "Materia: $materiaGuardada", color = Color.White, fontSize = 14.sp)
                    Text(text = "Dificultad: $dificultadGuardada / 10", color = Color.White, fontSize = 14.sp)
                    Text(
                        text = recomendacion,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Consejo: $consejo",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        if (materias.isNotEmpty()) {
            Text(
                text = "Materias guardadas:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = VerdePrimario,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            materias.forEach { (nombre, dificultad) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = nombre,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = VerdePrimario
                        )
                        Text(
                            text = "Dificultad: $dificultad / 10",
                            fontSize = 12.sp,
                            color = TextoSecundario
                        )
                    }
                }
            }
        }

        scope.launch {
            consejo = withContext(Dispatchers.IO) {
                ConsejosApi().obtenerConsejo()
            }
        }
    }
}