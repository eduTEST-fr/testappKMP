package com.example.eduflow.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduflow.config.ApiConfig
import com.example.eduflow.data.CatalogoUPT
import com.example.eduflow.storage.PerfilStorage
import com.example.eduflow.storage.SesionStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PerfilApiDto(
    val id: Int,
    val nombre: String,
    val carrera: String = "",
    val cuatrimestre: Int = 1,
    val sobreMi: String = "",
    val materiasDestaca: String = "",
    val rol: String = "ALUMNO",
    val avatarId: String = "avatar_1",
    val grado: String = "",
    val especialidad: String = "",
    val permiteAsesoria: Boolean = false
)

@Serializable
private data class DisponibilidadApiDto(
    val id: Int = 0,
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String,
    val ocupado: Boolean = false
)

private val NOMBRES_DIA = mapOf(1 to "Lunes", 2 to "Martes", 3 to "Miércoles", 4 to "Jueves", 5 to "Viernes")

// Estado de un día de la semana en el editor de disponibilidad del Asesor.
private data class DispDiaUI(
    val dia: Int,
    val activo: Boolean = false,
    val horaInicio: String = "09:00",
    val horaFin: String = "10:00"
)

private val jsonParser = Json { ignoreUnknownKeys = true }

// Perfil del usuario para la Red de Apoyo: carrera, cuatrimestre, materias
// destacadas y avatar. Antes esto vivia solo en PerfilStorage (local en el
// dispositivo); ahora se sincroniza con el backend (GET/PUT /peers/perfil)
// para que otros usuarios puedan ver el perfil real. PerfilStorage se
// mantiene como respaldo mientras carga o si falla la conexión.
@Composable
fun PerfilView(onVolver: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val client = remember { HttpClient() }
    val token  = SesionStorage.obtenerToken() ?: ""
    val nombre = SesionStorage.obtenerNombre()

    var editando      by remember { mutableStateOf(false) }
    var cargando      by remember { mutableStateOf(true) }
    var guardando     by remember { mutableStateOf(false) }
    var errorMsg       by remember { mutableStateOf("") }

    var carrera        by remember { mutableStateOf(PerfilStorage.obtenerCarrera()) }
    var cuatrimestre    by remember { mutableStateOf(PerfilStorage.obtenerCuatrimestre()) }
    var bio              by remember { mutableStateOf(PerfilStorage.obtenerBio()) }
    var materias         by remember { mutableStateOf(PerfilStorage.obtenerMateriasDestacadas()) }
    var avatarId          by remember { mutableStateOf("avatar_1") }
    var rol                by remember { mutableStateOf("ALUMNO") }
    var grado                by remember { mutableStateOf("") }
    var especialidad          by remember { mutableStateOf("") }
    var permiteAsesoria        by remember { mutableStateOf(false) }
    var disponibilidad         by remember { mutableStateOf(
        listOf(1, 2, 3, 4, 5).map { DispDiaUI(dia = it) }
    ) }

    suspend fun cargarDisponibilidad() {
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/asesorias/disponibilidad/mia") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val lista = jsonParser.decodeFromString<List<DisponibilidadApiDto>>(resp)
            disponibilidad = listOf(1, 2, 3, 4, 5).map { dia ->
                val existente = lista.find { it.diaSemana == dia }
                if (existente != null) DispDiaUI(dia, true, existente.horaInicio, existente.horaFin)
                else DispDiaUI(dia)
            }
        } catch (e: Exception) { /* sin disponibilidad guardada aun */ }
    }

    suspend fun cargarPerfil() {
        cargando = true
        try {
            val resp = client.get("${ApiConfig.BASE_URL}/peers/perfil") {
                header("Authorization", "Bearer $token")
            }.bodyAsText()
            val dto = jsonParser.decodeFromString<PerfilApiDto>(resp)
            carrera = dto.carrera.ifBlank { CatalogoUPT.nombresCarreras.first() }
            cuatrimestre = dto.cuatrimestre.toString()
            bio = dto.sobreMi
            materias = dto.materiasDestaca.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            avatarId = dto.avatarId
            rol = dto.rol
            grado = dto.grado
            especialidad = dto.especialidad
            permiteAsesoria = dto.permiteAsesoria
            if (dto.rol == "ASESOR") cargarDisponibilidad()
        } catch (e: Exception) { errorMsg = "No se pudo cargar el perfil desde el servidor." }
        cargando = false
    }

    LaunchedEffect(Unit) { cargarPerfil() }

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

            if (cargando) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdePrimario)
                }
                return@Column
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Box(modifier = Modifier.offset(y = (-44).dp)) {
                    Box(
                        modifier = Modifier.size(88.dp)
                            .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarIcono(avatarId = avatarId, sizeDp = 80)
                    }
                }

                Column(modifier = Modifier.offset(y = (-28).dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextoPrimario)
                        if (rol != "ALUMNO") {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = VerdePrimario) {
                                Text(if (rol == "ASESOR") "Asesor" else "Admin",
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }

                    if (editando) {
                        Spacer(Modifier.height(12.dp))
                        Text("Elige tu icono de perfil", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = TextoPrimario, modifier = Modifier.padding(bottom = 8.dp))
                        SelectorAvatar(avatarSeleccionado = avatarId, onSeleccionar = { avatarId = it })
                        Spacer(Modifier.height(14.dp))
                        SelectorCarrera(carrera) { carrera = it }
                        Spacer(Modifier.height(8.dp))
                        SelectorCuatrimestre(cuatrimestre.toIntOrNull()?.coerceIn(1, 10) ?: 1) {
                            cuatrimestre = it.toString()
                        }
                        if (rol == "ASESOR") {
                            Spacer(Modifier.height(8.dp))
                            CampoTexto("Grado académico", grado, "Ej: Licenciatura") { grado = it }
                            Spacer(Modifier.height(8.dp))
                            CampoTexto("Especialidad", especialidad, "Ej: Cálculo y Álgebra") { especialidad = it }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = permiteAsesoria, onCheckedChange = { permiteAsesoria = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = VerdePrimario)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Disponible para dar asesorías", fontSize = 13.sp, color = TextoPrimario)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Disponibilidad para asesorías", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = TextoPrimario,
                                modifier = Modifier.padding(bottom = 8.dp))
                            disponibilidad.forEachIndexed { idx, dia ->
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F4EE))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = dia.activo,
                                                onCheckedChange = { activo ->
                                                    disponibilidad = disponibilidad.toMutableList().also {
                                                        it[idx] = dia.copy(activo = activo)
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(checkedTrackColor = VerdePrimario)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(NOMBRES_DIA[dia.dia] ?: "", fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold, color = TextoPrimario)
                                        }
                                        if (dia.activo) {
                                            Spacer(Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = dia.horaInicio,
                                                    onValueChange = { v ->
                                                        disponibilidad = disponibilidad.toMutableList().also {
                                                            it[idx] = dia.copy(horaInicio = v)
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f), singleLine = true,
                                                    label = { Text("Inicio", fontSize = 11.sp) },
                                                    placeholder = { Text("09:00", fontSize = 12.sp) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = VerdePrimario,
                                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                                    )
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                OutlinedTextField(
                                                    value = dia.horaFin,
                                                    onValueChange = { v ->
                                                        disponibilidad = disponibilidad.toMutableList().also {
                                                            it[idx] = dia.copy(horaFin = v)
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f), singleLine = true,
                                                    label = { Text("Fin", fontSize = 11.sp) },
                                                    placeholder = { Text("10:00", fontSize = 12.sp) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = VerdePrimario,
                                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Carrera: $carrera", fontSize = 13.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 6.dp))
                        Text("Cuatrimestre: ${cuatrimestre}°", fontSize = 13.sp, color = TextoSecundario,
                            modifier = Modifier.padding(top = 3.dp))
                        if (rol == "ASESOR" && especialidad.isNotBlank()) {
                            Text("Especialidad: $especialidad", fontSize = 13.sp, color = TextoSecundario,
                                modifier = Modifier.padding(top = 3.dp))
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = Color(0xFFB00020), fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (editando) {
                                guardando = true; errorMsg = ""
                                scope.launch {
                                    try {
                                        client.put("${ApiConfig.BASE_URL}/peers/perfil") {
                                            header("Authorization", "Bearer $token")
                                            contentType(ContentType.Application.Json)
                                            setBody(
                                                "{\"carrera\":\"${escaparJson(carrera)}\"," +
                                                "\"cuatrimestre\":${cuatrimestre.toIntOrNull() ?: 1}," +
                                                "\"sobreMi\":\"${escaparJson(bio)}\"," +
                                                "\"materiasDestaca\":\"${escaparJson(materias.joinToString(","))}\"," +
                                                "\"avatarId\":\"$avatarId\"" +
                                                if (rol == "ASESOR")
                                                    ",\"grado\":\"${escaparJson(grado)}\"," +
                                                    "\"especialidad\":\"${escaparJson(especialidad)}\"," +
                                                    "\"permiteAsesoria\":$permiteAsesoria}"
                                                else "}"
                                            )
                                        }
                                        if (rol == "ASESOR") {
                                            val horariosJson = disponibilidad.filter { it.activo }.joinToString(",") {
                                                "{\"diaSemana\":${it.dia}," +
                                                "\"horaInicio\":\"${escaparJson(it.horaInicio)}\"," +
                                                "\"horaFin\":\"${escaparJson(it.horaFin)}\"}"
                                            }
                                            client.put("${ApiConfig.BASE_URL}/asesorias/disponibilidad") {
                                                header("Authorization", "Bearer $token")
                                                contentType(ContentType.Application.Json)
                                                setBody("{\"horarios\":[$horariosJson]}")
                                            }
                                        }
                                        // Respaldo local para que el perfil cargue rapido offline
                                        PerfilStorage.guardarPerfil(carrera.trim(), cuatrimestre.trim(), "", bio.trim())
                                        PerfilStorage.guardarMateriasDestacadas(materias)
                                        editando = false
                                    } catch (e: Exception) {
                                        errorMsg = "No se pudo guardar. Revisa tu conexión."
                                    }
                                    guardando = false
                                }
                            } else {
                                editando = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !guardando,
                        colors = ButtonDefaults.buttonColors(containerColor = VerdePrimario)
                    ) {
                        if (guardando)
                            CircularProgressIndicator(color = Color.White,
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else
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
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Elige hasta 5 materias del catálogo de tu carrera",
                                fontSize = 12.sp, color = TextoSecundario,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val catalogo = CatalogoUPT.materiasPorCuatrimestre(carrera)
                            Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                                catalogo.forEach { (cuatri, lista) ->
                                    Text("${cuatri}° cuatrimestre", fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold, color = VerdePrimario,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                                    lista.forEach { m ->
                                        val seleccionada = m.nombre in materias
                                        val limiteAlcanzado = materias.size >= 5 && !seleccionada
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable(enabled = !limiteAlcanzado) {
                                                    materias = if (seleccionada) materias - m.nombre
                                                               else materias + m.nombre
                                                }
                                        ) {
                                            Checkbox(
                                                checked = seleccionada,
                                                onCheckedChange = {
                                                    materias = if (seleccionada) materias - m.nombre
                                                               else materias + m.nombre
                                                },
                                                enabled = !limiteAlcanzado,
                                                colors = CheckboxDefaults.colors(checkedColor = VerdePrimario)
                                            )
                                            Text(m.nombre, fontSize = 13.sp,
                                                color = if (limiteAlcanzado) Color(0xFFBBBBBB) else TextoPrimario)
                                        }
                                    }
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
