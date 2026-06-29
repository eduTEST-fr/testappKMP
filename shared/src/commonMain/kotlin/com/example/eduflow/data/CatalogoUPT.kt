package com.example.eduflow.data

// Catalogo de carreras y materias de la Universidad Politecnica de Tulancingo
// (UPT, Hidalgo). Tomado EXACTAMENTE de la carga academica oficial por
// programa educativo proporcionada (documento "Carga academica por programa
// educativo"). No se modifico ninguna materia ni cuatrimestre: se respeta el
// nombre y la posicion exacta de cada una, incluyendo los cuatrimestres que
// son unicamente "Estadia" sin otras materias.
//
// Vive en el frontend porque son datos fijos del plan de estudios, no
// informacion que cambie por usuario ni que necesite persistirse en la BD.

data class MateriaCatalogo(val nombre: String, val cuatrimestre: Int)

data class CarreraCatalogo(val nombre: String, val materias: List<MateriaCatalogo>)

object CatalogoUPT {

    const val CUATRIMESTRE_MIN = 1
    const val CUATRIMESTRE_MAX = 10

    val carreras: List<CarreraCatalogo> = listOf(
        CarreraCatalogo("Ingeniería en Sistemas Computacionales", materiasSistemas),
        CarreraCatalogo("Ingeniería Industrial", materiasIndustrial),
        CarreraCatalogo("Ingeniería Civil", materiasCivil),
        CarreraCatalogo("Lic. en Ingeniería en Tecnologías de la Información e Innovación Digital", materiasTiId),
    )

    val nombresCarreras: List<String> = carreras.map { it.nombre }
    val cuatrimestres: List<Int> = (CUATRIMESTRE_MIN..CUATRIMESTRE_MAX).toList()

    fun materiasDe(carrera: String): List<MateriaCatalogo> =
        carreras.find { it.nombre == carrera }?.materias ?: emptyList()

    fun materiasPorCuatrimestre(carrera: String): Map<Int, List<MateriaCatalogo>> =
        materiasDe(carrera).groupBy { it.cuatrimestre }.toSortedMap()
}

private fun m(nombre: String, cuatrimestre: Int) = MateriaCatalogo(nombre, cuatrimestre)

// --- Ingeniería en Sistemas Computacionales ---
private val materiasSistemas: List<MateriaCatalogo> = listOf(
    m("Inglés I", 1), m("Química Básica", 1), m("Álgebra Lineal", 1),
    m("Introducción a la Programación", 1), m("Introducción a las Tecnologías de Información", 1),
    m("Herramientas Ofimáticas", 1), m("Expresión Oral y Escrita I", 1),

    m("Inglés II", 2), m("Desarrollo Humano y Valores", 2), m("Funciones Matemáticas", 2),
    m("Física", 2), m("Electricidad y Magnetismo", 2), m("Matemáticas Básicas para Computación", 2),
    m("Arquitectura de Computadoras", 2),

    m("Inglés III", 3), m("Inteligencia Emocional y Manejo de Conflictos", 3), m("Cálculo Diferencial", 3),
    m("Probabilidad y Estadística", 3), m("Programación", 3), m("Introducción a Redes", 3),
    m("Mantenimiento a Equipo de Cómputo", 3),

    m("Inglés IV", 4), m("Habilidades Cognitivas y Creatividad", 4), m("Cálculo Integral", 4),
    m("Ingeniería de Software", 4), m("Estructura de Datos", 4), m("Ruteo y Conmutación", 4),
    m("Estancia I", 4),

    m("Inglés V", 5), m("Ética Profesional", 5), m("Matemáticas para Ingeniería I", 5),
    m("Física para Ingeniería", 5), m("Fundamentos de Programación Orientada a Objetos", 5),
    m("Escalamiento de Redes", 5), m("Base de Datos", 5),

    m("Inglés VI", 6), m("Habilidades Gerenciales", 6), m("Matemáticas para Ingeniería II", 6),
    m("Sistemas Operativos", 6), m("Programación Orientada a Objetos", 6), m("Interconexión de Redes", 6),
    m("Administración de Base de Datos", 6),

    m("Inglés VII", 7), m("Liderazgo de Equipos de Alto Desempeño", 7),
    m("Formulación de Proyectos de Tecnologías de Información", 7), m("Lenguajes y Autómatas", 7),
    m("Programación Web", 7), m("Ingeniería de Requisitos", 7), m("Estancia II", 7),

    m("Inglés VIII", 8), m("Tecnologías de Virtualización", 8),
    m("Administración de Proyectos de Tecnologías de Información", 8),
    m("Tecnologías y Aplicaciones en Internet", 8), m("Diseño de Interfaces", 8),
    m("Sistemas Inteligentes", 8), m("Gestión de Desarrollo de Software", 8),

    m("Inglés IX", 9), m("Inteligencia de Negocios", 9),
    m("Desarrollo de Negocios para Tecnologías de Información", 9), m("Sistemas Embebidos", 9),
    m("Programación Móvil", 9), m("Seguridad Informática", 9), m("Expresión Oral y Escrita II", 9),

    m("Estadía Profesional", 10),
)

// --- Ingeniería Industrial ---
private val materiasIndustrial: List<MateriaCatalogo> = listOf(
    m("Inglés I", 1), m("Valores del Ser", 1), m("Probabilidad y Estadística", 1),
    m("Cálculo Diferencial", 1), m("Ingeniería Industrial", 1), m("Dibujo para Ingeniería", 1),
    m("Química y Procesos Termodinámicos", 1),

    m("Inglés II", 2), m("Inteligencia Emocional", 2), m("Control Estadístico de la Calidad", 2),
    m("Cálculo Integral", 2), m("Seguridad e Higiene Industrial", 2), m("Mecánica Clásica", 2),
    m("Química y Tecnología de los Materiales", 2),

    m("Inglés III", 3), m("Desarrollo Interpersonal", 3), m("Álgebra Lineal", 3),
    m("Ecuaciones Diferenciales", 3), m("Electricidad y Magnetismo", 3), m("Metrología", 3),
    m("Procesos de Fabricación", 3),

    m("Inglés IV", 4), m("Habilidades del Pensamiento", 4), m("Lógica de Programación", 4),
    m("Estadística Industrial", 4), m("Análisis y Enfoque de Sistemas", 4), m("Ingeniería de Métodos", 4),
    m("Estancia", 4),

    m("Inglés V", 5), m("Habilidades Organizacionales", 5), m("Administración de la Producción", 5),
    m("Investigación de Operaciones", 5), m("Ingeniería de Planta", 5), m("Estudio del Trabajo", 5),
    m("Fundamentos de Ingeniería Electrónica", 5),

    m("Inglés VI", 6), m("Ética Profesional", 6), m("Planeación de la Producción", 6),
    m("Análisis de Decisiones", 6), m("Automatización y Control", 6), m("Ergonomía", 6),
    m("Seis Sigma y Análisis de Falla", 6),

    m("Inglés VII", 7), m("Ingeniería Económica", 7), m("Sistemas de Manufactura", 7),
    m("Proceso Administrativo y Planeación Estratégica", 7), m("Contabilidad Industrial", 7),
    m("Ingeniería en Diseño y Desarrollo del Producto", 7), m("Estancia", 7),

    m("Inglés VIII", 8), m("Administración de la Calidad Total", 8),
    m("Aplicación de la Robótica en la Manufactura", 8), m("Simulación de Sistemas Productivos", 8),
    m("Proceso Textil", 8), m("Logística", 8), m("Análisis Financiero", 8),

    m("Inglés IX", 9), m("Sistemas de Gestión de la Calidad", 9),
    m("Evaluación y Administración de Proyectos", 9), m("Industria Sustentable", 9),
    m("Tecnología de los Alimentos", 9), m("Administración de Recursos Humanos", 9),
    m("Manufactura de Clase Mundial", 9),

    m("Estadía", 10),
)

// --- Ingeniería Civil ---
private val materiasCivil: List<MateriaCatalogo> = listOf(
    m("Inglés I", 1), m("Valores del Ser", 1), m("Fundamentos de Física", 1),
    m("Química de los Materiales Constructivos", 1), m("Técnicas de Dibujo", 1),
    m("Cálculo Diferencial e Integral", 1), m("Álgebra Lineal", 1),

    m("Inglés II", 2), m("Inteligencia Emocional", 2), m("Estática", 2),
    m("Procesos de Construcción Ligera", 2), m("Dibujo Constructivo", 2), m("Cálculo Vectorial", 2),
    m("Topografía", 2),

    m("Inglés III", 3), m("Desarrollo Interpersonal", 3), m("Estructuras Isostáticas", 3),
    m("Procesos de Construcción Pesada", 3), m("Dibujo en Tres Dimensiones", 3),
    m("Ecuaciones Diferenciales", 3), m("Topografía Avanzada", 3),

    m("Inglés IV", 4), m("Habilidades del Pensamiento", 4), m("Hidráulica", 4),
    m("Comportamiento de Suelos", 4), m("Dinámica", 4), m("Probabilidad y Estadística", 4),
    m("Estancia I", 4),

    m("Inglés V", 5), m("Habilidades Organizacionales", 5), m("Redes de Conducción Hidráulica", 5),
    m("Mecánica de Suelos", 5), m("Mecánica de Materiales", 5), m("Cuantificación y Volumetría de Obra", 5),
    m("Control de Calidad en Obra", 5),

    m("Inglés VI", 6), m("Ética Profesional", 6), m("Hidráulica de Canales Abiertos", 6),
    m("Geotecnia", 6), m("Comportamiento de Elementos Estructurales", 6),
    m("Costos y Presupuestos de Obra", 6), m("Supervisión de Obra", 6),

    m("Inglés VII", 7), m("Ingeniería de Tránsito", 7), m("Hidrología", 7),
    m("Diseño de Pavimentos", 7), m("Estructuras Hiperestáticas", 7), m("Geomática", 7),
    m("Estancia II", 7),

    m("Inglés VIII", 8), m("Dinámica Estructural", 8), m("Redes de Agua Potable y Alcantarillado", 8),
    m("Cimentaciones Superficiales", 8), m("Análisis Estructural Matricial", 8),
    m("Estructuras de Mampostería", 8), m("Administración de Proyectos", 8),

    m("Inglés IX", 9), m("Vías de Comunicación", 9), m("Obras Hidráulicas", 9),
    m("Cimentaciones Profundas", 9), m("Estructuras de Concreto", 9), m("Estructuras de Acero", 9),
    m("Planeación y Evaluación de Proyectos", 9),

    m("Estadía", 10),
)

// --- Licenciatura en Ingeniería en Tecnologías de la Información e Innovación Digital ---
// Nota: en este programa el cuatrimestre 6 tambien es Estadia (ademas del 10),
// tal como viene en la carga academica oficial; se respeta tal cual.
private val materiasTiId: List<MateriaCatalogo> = listOf(
    m("Inglés I", 1), m("Desarrollo Humano y Valores", 1), m("Fundamentos Matemáticos", 1),
    m("Fundamentos de Redes", 1), m("Física", 1), m("Fundamentos de Programación", 1),
    m("Comunicación y Habilidades Digitales", 1),

    m("Inglés II", 2), m("Habilidades Socioemocionales y Manejo de Conflictos", 2),
    m("Cálculo Diferencial", 2), m("Conmutación y Enrutamiento de Redes", 2),
    m("Probabilidad y Estadística", 2), m("Programación Estructurada", 2), m("Sistemas Operativos", 2),

    m("Inglés III", 3), m("Desarrollo del Pensamiento y Toma de Decisiones", 3), m("Cálculo Integral", 3),
    m("Tópicos de Calidad para el Diseño de Software", 3), m("Bases de Datos", 3),
    m("Programación Orientada a Objetos", 3), m("Proyecto Integrador I", 3),

    m("Inglés IV", 4), m("Ética Profesional", 4), m("Cálculo de Varias Variables", 4),
    m("Aplicaciones Web", 4), m("Estructura de Datos", 4), m("Desarrollo de Aplicaciones Móviles", 4),
    m("Análisis y Diseño de Software", 4),

    m("Inglés V", 5), m("Liderazgo de Equipos de Alto Desempeño", 5), m("Ecuaciones Diferenciales", 5),
    m("Aplicaciones Web Orientadas a Servicios", 5), m("Bases de Datos Avanzadas", 5),
    m("Estándares y Métricas para el Desarrollo de Software", 5), m("Proyecto Integrador II", 5),

    m("Estadía", 6),

    m("Inglés VI", 7), m("Habilidades Gerenciales", 7), m("Formulación de Proyectos de Tecnología", 7),
    m("Fundamentos de Inteligencia Artificial", 7), m("Ética y Legislación en Tecnologías de la Información", 7),
    m("Optativa I", 7), m("Seguridad Informática", 7),

    m("Inglés VII", 8), m("Electrónica Digital", 8), m("Gestión de Proyectos de Tecnología", 8),
    m("Programación para Inteligencia Artificial", 8), m("Administración de Servidores", 8),
    m("Optativa II", 8), m("Informática Forense", 8),

    m("Inglés VIII", 9), m("Internet de las Cosas", 9), m("Evaluación de Proyectos de Tecnología", 9),
    m("Ciencia de Datos", 9), m("Tecnologías Disruptivas", 9), m("Optativa III", 9),
    m("Proyecto Integrador III", 9),

    m("Estadía", 10),
)
