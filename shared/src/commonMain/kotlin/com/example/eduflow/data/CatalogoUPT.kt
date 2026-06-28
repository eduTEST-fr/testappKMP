package com.example.eduflow.data

// Catálogo estático de carreras y materias de la UPT (Universidad Politécnica
// de Tulancingo). Vive en el frontend porque son datos fijos del plan de
// estudios, no información que cambie por usuario ni que necesite persistirse
// en la base de datos.

data class MateriaCatalogo(val nombre: String, val cuatrimestre: Int)

data class CarreraCatalogo(val nombre: String, val materias: List<MateriaCatalogo>)

object CatalogoUPT {

    val carreras: List<CarreraCatalogo> = listOf(
        CarreraCatalogo("Ingeniería en Sistemas Computacionales", materiasSistemas),
        CarreraCatalogo("Ingeniería en Robótica", materiasRobotica),
        CarreraCatalogo("Ingeniería Civil", materiasCivil),
        CarreraCatalogo("Licenciatura en Negocios", materiasNegocios),
        CarreraCatalogo("Licenciatura en Administración", materiasAdministracion),
    )

    val nombresCarreras: List<String> = carreras.map { it.nombre }

    fun materiasDe(carrera: String): List<MateriaCatalogo> =
        carreras.find { it.nombre == carrera }?.materias ?: emptyList()

    fun materiasPorCuatrimestre(carrera: String): Map<Int, List<MateriaCatalogo>> =
        materiasDe(carrera).groupBy { it.cuatrimestre }.toSortedMap()
}

// --- Ingeniería en Sistemas Computacionales: 6+ materias por cuatrimestre (1-9) ---
private val materiasSistemas: List<MateriaCatalogo> = listOf(
    // 1°
    MateriaCatalogo("Cálculo Diferencial", 1),
    MateriaCatalogo("Álgebra Lineal", 1),
    MateriaCatalogo("Fundamentos de Programación", 1),
    MateriaCatalogo("Taller de Sistemas Operativos", 1),
    MateriaCatalogo("Inglés I", 1),
    MateriaCatalogo("Habilidades del Pensamiento", 1),
    // 2°
    MateriaCatalogo("Cálculo Integral", 2),
    MateriaCatalogo("Programación Orientada a Objetos", 2),
    MateriaCatalogo("Estadística y Probabilidad", 2),
    MateriaCatalogo("Matemáticas Discretas", 2),
    MateriaCatalogo("Inglés II", 2),
    MateriaCatalogo("Taller de Ética", 2),
    // 3°
    MateriaCatalogo("Estructura de Datos", 3),
    MateriaCatalogo("Cálculo Vectorial", 3),
    MateriaCatalogo("Bases de Datos I", 3),
    MateriaCatalogo("Arquitectura de Computadoras", 3),
    MateriaCatalogo("Inglés III", 3),
    MateriaCatalogo("Contabilidad Financiera", 3),
    // 4°
    MateriaCatalogo("Bases de Datos II", 4),
    MateriaCatalogo("Programación Web", 4),
    MateriaCatalogo("Sistemas Operativos", 4),
    MateriaCatalogo("Ecuaciones Diferenciales", 4),
    MateriaCatalogo("Redes de Computadoras I", 4),
    MateriaCatalogo("Inglés IV", 4),
    // 5°
    MateriaCatalogo("Programación Móvil", 5),
    MateriaCatalogo("Redes de Computadoras II", 5),
    MateriaCatalogo("Ingeniería de Software I", 5),
    MateriaCatalogo("Lenguajes y Autómatas", 5),
    MateriaCatalogo("Sistemas Distribuidos", 5),
    MateriaCatalogo("Inglés V", 5),
    // 6°
    MateriaCatalogo("Ingeniería de Software II", 6),
    MateriaCatalogo("Inteligencia Artificial", 6),
    MateriaCatalogo("Seguridad Informática", 6),
    MateriaCatalogo("Programación de Dispositivos Móviles", 6),
    MateriaCatalogo("Administración de Proyectos de TI", 6),
    MateriaCatalogo("Inglés VI", 6),
    // 7°
    MateriaCatalogo("Minería de Datos", 7),
    MateriaCatalogo("Desarrollo Web Avanzado", 7),
    MateriaCatalogo("Cómputo en la Nube", 7),
    MateriaCatalogo("Calidad de Software", 7),
    MateriaCatalogo("Arquitectura de Software", 7),
    MateriaCatalogo("Innovación y Emprendimiento", 7),
    // 8°
    MateriaCatalogo("Inteligencia de Negocios", 8),
    MateriaCatalogo("Aprendizaje Automático", 8),
    MateriaCatalogo("Internet de las Cosas", 8),
    MateriaCatalogo("Auditoría Informática", 8),
    MateriaCatalogo("Gestión de Proyectos de Software", 8),
    MateriaCatalogo("Residencias Profesionales I", 8),
    // 9°
    MateriaCatalogo("Estancia / Estadía Profesional", 9),
    MateriaCatalogo("Taller de Titulación", 9),
    MateriaCatalogo("Seminario de Investigación", 9),
    MateriaCatalogo("Ética Profesional", 9),
    MateriaCatalogo("Gestión de la Innovación", 9),
    MateriaCatalogo("Residencias Profesionales II", 9),
)

// --- Ingeniería en Robótica: 4+ materias representativas por cuatrimestre ---
private val materiasRobotica: List<MateriaCatalogo> = listOf(
    MateriaCatalogo("Cálculo Diferencial", 1),
    MateriaCatalogo("Álgebra Lineal", 1),
    MateriaCatalogo("Dibujo Asistido por Computadora", 1),
    MateriaCatalogo("Fundamentos de Programación", 1),

    MateriaCatalogo("Cálculo Integral", 2),
    MateriaCatalogo("Estática", 2),
    MateriaCatalogo("Circuitos Eléctricos", 2),
    MateriaCatalogo("Programación Orientada a Objetos", 2),

    MateriaCatalogo("Dinámica", 3),
    MateriaCatalogo("Electrónica Analógica", 3),
    MateriaCatalogo("Cálculo Vectorial", 3),
    MateriaCatalogo("Sistemas Digitales", 3),

    MateriaCatalogo("Electrónica Digital", 4),
    MateriaCatalogo("Mecanismos", 4),
    MateriaCatalogo("Microcontroladores", 4),
    MateriaCatalogo("Ecuaciones Diferenciales", 4),

    MateriaCatalogo("Control Clásico", 5),
    MateriaCatalogo("Neumática e Hidráulica", 5),
    MateriaCatalogo("Sensores y Actuadores", 5),
    MateriaCatalogo("Manufactura Asistida por Computadora", 5),

    MateriaCatalogo("Robótica Industrial", 6),
    MateriaCatalogo("Control Digital", 6),
    MateriaCatalogo("Visión Artificial", 6),
    MateriaCatalogo("Sistemas Embebidos", 6),

    MateriaCatalogo("Inteligencia Artificial Aplicada", 7),
    MateriaCatalogo("Diseño Mecatrónico", 7),
    MateriaCatalogo("Redes Industriales", 7),
    MateriaCatalogo("Manufactura Avanzada", 7),

    MateriaCatalogo("Automatización Industrial", 8),
    MateriaCatalogo("Robótica Móvil", 8),
    MateriaCatalogo("Gestión de Proyectos", 8),
    MateriaCatalogo("Residencias Profesionales I", 8),

    MateriaCatalogo("Estancia / Estadía Profesional", 9),
    MateriaCatalogo("Taller de Titulación", 9),
    MateriaCatalogo("Ética Profesional", 9),
    MateriaCatalogo("Residencias Profesionales II", 9),
)

// --- Ingeniería Civil: 4+ materias representativas por cuatrimestre ---
private val materiasCivil: List<MateriaCatalogo> = listOf(
    MateriaCatalogo("Cálculo Diferencial", 1),
    MateriaCatalogo("Álgebra Lineal", 1),
    MateriaCatalogo("Dibujo de Construcción", 1),
    MateriaCatalogo("Topografía I", 1),

    MateriaCatalogo("Cálculo Integral", 2),
    MateriaCatalogo("Estática", 2),
    MateriaCatalogo("Topografía II", 2),
    MateriaCatalogo("Geología Aplicada", 2),

    MateriaCatalogo("Mecánica de Materiales", 3),
    MateriaCatalogo("Cálculo Vectorial", 3),
    MateriaCatalogo("Mecánica de Suelos I", 3),
    MateriaCatalogo("Materiales de Construcción", 3),

    MateriaCatalogo("Mecánica de Suelos II", 4),
    MateriaCatalogo("Análisis Estructural I", 4),
    MateriaCatalogo("Ecuaciones Diferenciales", 4),
    MateriaCatalogo("Hidráulica Básica", 4),

    MateriaCatalogo("Análisis Estructural II", 5),
    MateriaCatalogo("Cimentaciones", 5),
    MateriaCatalogo("Hidrología", 5),
    MateriaCatalogo("Costos y Presupuestos", 5),

    MateriaCatalogo("Concreto Reforzado", 6),
    MateriaCatalogo("Estructuras de Acero", 6),
    MateriaCatalogo("Vías Terrestres I", 6),
    MateriaCatalogo("Instalaciones Hidrosanitarias", 6),

    MateriaCatalogo("Vías Terrestres II", 7),
    MateriaCatalogo("Puentes", 7),
    MateriaCatalogo("Administración de la Construcción", 7),
    MateriaCatalogo("Impacto Ambiental", 7),

    MateriaCatalogo("Obras Hidráulicas", 8),
    MateriaCatalogo("Construcción Sustentable", 8),
    MateriaCatalogo("Gestión de Proyectos de Construcción", 8),
    MateriaCatalogo("Residencias Profesionales I", 8),

    MateriaCatalogo("Estancia / Estadía Profesional", 9),
    MateriaCatalogo("Taller de Titulación", 9),
    MateriaCatalogo("Ética Profesional", 9),
    MateriaCatalogo("Residencias Profesionales II", 9),
)

// --- Licenciatura en Negocios: 4+ materias representativas por cuatrimestre ---
private val materiasNegocios: List<MateriaCatalogo> = listOf(
    MateriaCatalogo("Fundamentos de Negocios", 1),
    MateriaCatalogo("Matemáticas para Negocios", 1),
    MateriaCatalogo("Comunicación Efectiva", 1),
    MateriaCatalogo("Inglés I", 1),

    MateriaCatalogo("Microeconomía", 2),
    MateriaCatalogo("Contabilidad Financiera", 2),
    MateriaCatalogo("Estadística para Negocios", 2),
    MateriaCatalogo("Inglés II", 2),

    MateriaCatalogo("Macroeconomía", 3),
    MateriaCatalogo("Mercadotecnia", 3),
    MateriaCatalogo("Derecho Empresarial", 3),
    MateriaCatalogo("Inglés III", 3),

    MateriaCatalogo("Finanzas Corporativas", 4),
    MateriaCatalogo("Comercio Internacional", 4),
    MateriaCatalogo("Comportamiento Organizacional", 4),
    MateriaCatalogo("Inglés IV", 4),

    MateriaCatalogo("Negociación y Ventas", 5),
    MateriaCatalogo("Logística y Cadena de Suministro", 5),
    MateriaCatalogo("Mercadotecnia Digital", 5),
    MateriaCatalogo("Inglés V", 5),

    MateriaCatalogo("Modelos de Negocio", 6),
    MateriaCatalogo("Finanzas Internacionales", 6),
    MateriaCatalogo("Análisis de Datos para Negocios", 6),
    MateriaCatalogo("Inglés VI", 6),

    MateriaCatalogo("Innovación y Emprendimiento", 7),
    MateriaCatalogo("Estrategia de Negocios", 7),
    MateriaCatalogo("Ética Empresarial", 7),
    MateriaCatalogo("Gestión del Talento", 7),

    MateriaCatalogo("Plan de Negocios", 8),
    MateriaCatalogo("Negocios Digitales", 8),
    MateriaCatalogo("Gestión de Proyectos", 8),
    MateriaCatalogo("Residencias Profesionales I", 8),

    MateriaCatalogo("Estancia / Estadía Profesional", 9),
    MateriaCatalogo("Taller de Titulación", 9),
    MateriaCatalogo("Seminario de Investigación", 9),
    MateriaCatalogo("Residencias Profesionales II", 9),
)

// --- Licenciatura en Administración: 4+ materias representativas por cuatrimestre ---
private val materiasAdministracion: List<MateriaCatalogo> = listOf(
    MateriaCatalogo("Fundamentos de Administración", 1),
    MateriaCatalogo("Matemáticas Administrativas", 1),
    MateriaCatalogo("Comunicación Organizacional", 1),
    MateriaCatalogo("Inglés I", 1),

    MateriaCatalogo("Proceso Administrativo", 2),
    MateriaCatalogo("Contabilidad General", 2),
    MateriaCatalogo("Estadística Administrativa", 2),
    MateriaCatalogo("Inglés II", 2),

    MateriaCatalogo("Administración de Recursos Humanos", 3),
    MateriaCatalogo("Derecho Laboral", 3),
    MateriaCatalogo("Microeconomía", 3),
    MateriaCatalogo("Inglés III", 3),

    MateriaCatalogo("Administración Financiera", 4),
    MateriaCatalogo("Macroeconomía", 4),
    MateriaCatalogo("Mercadotecnia", 4),
    MateriaCatalogo("Inglés IV", 4),

    MateriaCatalogo("Administración de Operaciones", 5),
    MateriaCatalogo("Desarrollo Organizacional", 5),
    MateriaCatalogo("Comportamiento del Consumidor", 5),
    MateriaCatalogo("Inglés V", 5),

    MateriaCatalogo("Planeación Estratégica", 6),
    MateriaCatalogo("Administración Pública", 6),
    MateriaCatalogo("Auditoría Administrativa", 6),
    MateriaCatalogo("Inglés VI", 6),

    MateriaCatalogo("Gestión de la Calidad", 7),
    MateriaCatalogo("Innovación y Emprendimiento", 7),
    MateriaCatalogo("Ética Profesional", 7),
    MateriaCatalogo("Sistemas de Información Administrativa", 7),

    MateriaCatalogo("Alta Dirección", 8),
    MateriaCatalogo("Gestión de Proyectos", 8),
    MateriaCatalogo("Negocios Internacionales", 8),
    MateriaCatalogo("Residencias Profesionales I", 8),

    MateriaCatalogo("Estancia / Estadía Profesional", 9),
    MateriaCatalogo("Taller de Titulación", 9),
    MateriaCatalogo("Seminario de Investigación", 9),
    MateriaCatalogo("Residencias Profesionales II", 9),
)
