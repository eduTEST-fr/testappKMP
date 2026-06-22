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
import com.example.eduflow.storage.SesionStorage

// Vista de Red de Apoyo. Por ahora es solo navegable / visual (sin backend
// real todavia), fiel a los mockups de Terra: header verde, mentor
// destacado, lista de ayudantes y solicitudes recientes con filtro.
@Composable
fun PeersView(
    onVolver: () -> Unit,
    onVerStudyCast: () -> Unit,
    onVerAudios: () -> Unit,
    onVerPerfil: () -> Unit
) {
    var filtroActivo by remember { mutableStateOf("Todas") }
    val filtros = listOf("Todas", "Cálculo", "Ecuaciones Diferenciales", "Física")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(0.dp)) {
                    Text("←", fontSize = 20.sp, color = VerdePrimario)
                }
                Spacer(Modifier.weight(1f))
                Text("EduFlow", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = VerdePrimario)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDDE8E0))
                        .clickable { onVerPerfil() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(SesionStorage.obtenerNombre().take(1).uppercase(),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VerdePrimario)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 90.dp)
            ) {

                // Card verde de encabezado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VerdePrimario)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tu Red de Apoyo Académico", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White,
                            lineHeight = 26.sp)
                        Text(
                            "Conecta con mentores expertos en áreas técnicas y resuelve " +
                                "tus dudas en un entorno colaborativo y humano.",
                            fontSize = 13.sp, color = Color.White.copy(0.85f),
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White
                            ) {
                                Text("Buscar Mentor", fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = VerdePrimario,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text("Publicar Solicitud", fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text("Mentores Destacados", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario,
                    modifier = Modifier.padding(bottom = 12.dp))

                // Mentor destacado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(VerdePrimario.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("AR", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = VerdePrimario)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFE3C2)
                                ) {
                                    Text("Sistemas Operativos", fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold, color = Color(0xFF8B6914),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Text("Dr. Alejandro Ruiz", fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold, color = TextoPrimario,
                                    modifier = Modifier.padding(top = 6.dp))
                            }
                            Text("★ 4.9", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B6914))
                        }
                        Text(
                            "Experto en gestión de procesos, memoria virtual y " +
                                "arquitectura de kernel. Más de 10 años de experiencia.",
                            fontSize = 12.sp, color = TextoSecundario, lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = VerdePrimario
                        ) {
                            Text("Agendar Mentoría", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Mentor secundario, fila compacta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDE8E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CM", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = VerdePrimario)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Electrónica", fontSize = 11.sp, color = VerdePrimario,
                                fontWeight = FontWeight.SemiBold)
                            Text("Ing. Carla Méndez", fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEEEEE)
                        ) {
                            Text("Ver Perfil", fontSize = 11.sp, color = TextoSecundario,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Filtro por materia
                Text("Filtrar por materia", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = TextoSecundario,
                    modifier = Modifier.padding(bottom = 10.dp))

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    filtros.forEach { filtro ->
                        Surface(
                            modifier = Modifier.padding(end = 8.dp)
                                .clickable { filtroActivo = filtro },
                            shape = RoundedCornerShape(20.dp),
                            color = if (filtroActivo == filtro) VerdePrimario
                                    else Color(0xFFEEEEEE)
                        ) {
                            Text(filtro, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (filtroActivo == filtro) Color.White else TextoSecundario,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text("Solicitudes Recientes", fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = TextoPrimario,
                    modifier = Modifier.padding(bottom = 10.dp))

                SolicitudCard(
                    materia = "Σ", titulo = "Duda: Ecuaciones Diferenciales Lineales",
                    descripcion = "No logro entender el método del factor integrante " +
                        "para ecuaciones no exactas. ¿Alguien tiene algún truco visual?",
                    autor = "Mateo G.", tiempo = "Hace 2h"
                )
                Spacer(Modifier.height(10.dp))
                SolicitudCard(
                    materia = "Σ", titulo = "Ayuda con Transformada de Laplace",
                    descripcion = "Ecuaciones Diferenciales: problemas con la resolución " +
                        "de circuitos RLC usando Laplace. ¡Examen el lunes!",
                    autor = "Sofía L.", tiempo = "Hace 5h"
                )

                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEAE0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("¿Necesitas ayuda tú también?", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = TextoPrimario)
                        Text("Publica tu duda y recibe apoyo de la comunidad.",
                            fontSize = 12.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = VerdePrimario
                        ) {
                            Text("Crear Solicitud", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        }
                    }
                }
            }
        }

        // Bottom nav
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = Beige,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem(label = "Dashboard", selected = false,
                    symbol = "⊞", onClick = onVolver)
                BottomNavItem(label = "StudyCast", selected = false,
                    symbol = "▶", onClick = onVerStudyCast)
                BottomNavItem(label = "Audio", selected = false,
                    symbol = "♪", onClick = onVerAudios)
                BottomNavItem(label = "Peers", selected = true, symbol = "⊙")
            }
        }
    }
}

@Composable
private fun SolicitudCard(
    materia: String, titulo: String, descripcion: String,
    autor: String, tiempo: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDDE8E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(materia, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = VerdePrimario)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = TextoPrimario, lineHeight = 18.sp)
                }
                Text(tiempo, fontSize = 10.sp, color = TextoSecundario)
            }
            Text(descripcion, fontSize = 12.sp, color = TextoSecundario,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 42.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(autor, fontSize = 11.sp, color = TextoSecundario,
                    fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(8.dp), color = VerdePrimario) {
                    Text("Ayudar ahora", fontSize = 11.sp, color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}
