package com.example.eduflow.data

// Catalogo estatico de carreras y materias de la Universidad Politecnica de
// Tulancingo (UPT, Hidalgo). Las carreras y la duracion (10 cuatrimestres)
// vienen de la oferta educativa oficial: https://www.upt.edu.mx -- Ingenierias:
// Civil, Robotica, Industrial, Sistemas Computacionales, Tecnologias de
// Manufactura, Electronica y Telecomunicaciones. Licenciaturas: Gestion
// Empresarial, Negocios Internacionales, Ingenieria Financiera, Psicologia.
// Vive en el frontend porque son datos fijos del plan de estudios, no
// informacion que cambie por usuario ni que necesite persistirse en la BD.

data class MateriaCatalogo(val nombre: String, val cuatrimestre: Int)

data class CarreraCatalogo(val nombre: String, val materias: List<MateriaCatalogo>)

object CatalogoUPT {

    const val CUATRIMESTRE_MIN = 1
    const val CUATRIMESTRE_MAX = 10

    val carreras: List<CarreraCatalogo> = listOf(
        CarreraCatalogo("Ingeniería en Sistemas Computacionales", materiasSistemas),
        CarreraCatalogo("Ingeniería Civil", materiasCivil),
        CarreraCatalogo("Ingeniería en Robótica", materiasRobotica),
        CarreraCatalogo("Ingeniería Industrial", materiasIndustrial),
        CarreraCatalogo("Ingeniería en Tecnologías de Manufactura", materiasManufactura),
        CarreraCatalogo("Ingeniería en Electrónica y Telecomunicaciones", materiasElectronica),
        CarreraCatalogo("Ingeniería Financiera", materiasFinanciera),
        CarreraCatalogo("Licenciatura en Gestión Empresarial", materiasGestionEmpresarial),
        CarreraCatalogo("Licenciatura en Negocios Internacionales", materiasNegociosInternacionales),
        CarreraCatalogo("Licenciatura en Psicología", materiasPsicologia),
    )

    val nombresCarreras: List<String> = carreras.map { it.nombre }
    val cuatrimestres: List<Int> = (CUATRIMESTRE_MIN..CUATRIMESTRE_MAX).toList()

    fun materiasDe(carrera: String): List<MateriaCatalogo> =
        carreras.find { it.nombre == carrera }?.materias ?: emptyList()

    fun materiasPorCuatrimestre(carrera: String): Map<Int, List<MateriaCatalogo>> =
        materiasDe(carrera).groupBy { it.cuatrimestre }.toSortedMap()
}

private fun m(nombre: String, cuatrimestre: Int) = MateriaCatalogo(nombre, cuatrimestre)

// --- Ingenieria en Sistemas Computacionales: 10 cuatrimestres, 6 materias c/u ---
private val materiasSistemas: List<MateriaCatalogo> = listOf(
    m("Calculo Diferencial", 1), m("Algebra Lineal", 1), m("Fundamentos de Programacion", 1),
    m("Taller de Sistemas Operativos", 1), m("Ingles I", 1), m("Habilidades del Pensamiento", 1),

    m("Calculo Integral", 2), m("Programacion Orientada a Objetos", 2), m("Estadistica y Probabilidad", 2),
    m("Matematicas Discretas", 2), m("Ingles II", 2), m("Taller de Etica", 2),

    m("Estructura de Datos", 3), m("Calculo Vectorial", 3), m("Bases de Datos I", 3),
    m("Arquitectura de Computadoras", 3), m("Ingles III", 3), m("Contabilidad Financiera", 3),

    m("Bases de Datos II", 4), m("Programacion Web", 4), m("Sistemas Operativos", 4),
    m("Ecuaciones Diferenciales", 4), m("Redes de Computadoras I", 4), m("Ingles IV", 4),

    m("Programacion Movil", 5), m("Redes de Computadoras II", 5), m("Ingenieria de Software I", 5),
    m("Lenguajes y Automatas", 5), m("Sistemas Distribuidos", 5), m("Ingles V", 5),

    m("Ingenieria de Software II", 6), m("Inteligencia Artificial", 6), m("Seguridad Informatica", 6),
    m("Programacion de Dispositivos Moviles", 6), m("Administracion de Proyectos de TI", 6), m("Ingles VI", 6),

    m("Mineria de Datos", 7), m("Desarrollo Web Avanzado", 7), m("Computo en la Nube", 7),
    m("Calidad de Software", 7), m("Arquitectura de Software", 7), m("Ingles VII", 7),

    m("Inteligencia de Negocios", 8), m("Aprendizaje Automatico", 8), m("Internet de las Cosas", 8),
    m("Auditoria Informatica", 8), m("Gestion de Proyectos de Software", 8), m("Ingles VIII", 8),

    m("Computo Paralelo y Distribuido", 9), m("Topicos Avanzados de Programacion", 9),
    m("Innovacion y Emprendimiento Tecnologico", 9), m("Estadia I", 9), m("Etica Profesional", 9), m("Ingles IX", 9),

    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10),
    m("Gestion de la Innovacion", 10),
)

// --- Ingenieria Civil ---
private val materiasCivil: List<MateriaCatalogo> = listOf(
    m("Calculo Diferencial", 1), m("Algebra Lineal", 1), m("Dibujo de Construccion", 1), m("Topografia I", 1),
    m("Calculo Integral", 2), m("Estatica", 2), m("Topografia II", 2), m("Geologia Aplicada", 2),
    m("Mecanica de Materiales", 3), m("Calculo Vectorial", 3), m("Mecanica de Suelos I", 3), m("Materiales de Construccion", 3),
    m("Mecanica de Suelos II", 4), m("Analisis Estructural I", 4), m("Ecuaciones Diferenciales", 4), m("Hidraulica Basica", 4),
    m("Analisis Estructural II", 5), m("Cimentaciones", 5), m("Hidrologia", 5), m("Costos y Presupuestos", 5),
    m("Concreto Reforzado", 6), m("Estructuras de Acero", 6), m("Vias Terrestres I", 6), m("Instalaciones Hidrosanitarias", 6),
    m("Vias Terrestres II", 7), m("Puentes", 7), m("Administracion de la Construccion", 7), m("Impacto Ambiental", 7),
    m("Obras Hidraulicas", 8), m("Construccion Sustentable", 8), m("Gestion de Proyectos de Construccion", 8), m("Topicos de Geotecnia", 8),
    m("Planeacion Urbana", 9), m("Supervision de Obra", 9), m("Estadia I", 9), m("Etica Profesional", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Normatividad de la Construccion", 10),
)

// --- Ingenieria en Robotica ---
private val materiasRobotica: List<MateriaCatalogo> = listOf(
    m("Calculo Diferencial", 1), m("Algebra Lineal", 1), m("Dibujo Asistido por Computadora", 1), m("Fundamentos de Programacion", 1),
    m("Calculo Integral", 2), m("Estatica", 2), m("Circuitos Electricos", 2), m("Programacion Orientada a Objetos", 2),
    m("Dinamica", 3), m("Electronica Analogica", 3), m("Calculo Vectorial", 3), m("Sistemas Digitales", 3),
    m("Electronica Digital", 4), m("Mecanismos", 4), m("Microcontroladores", 4), m("Ecuaciones Diferenciales", 4),
    m("Control Clasico", 5), m("Neumatica e Hidraulica", 5), m("Sensores y Actuadores", 5), m("Manufactura Asistida por Computadora", 5),
    m("Robotica Industrial", 6), m("Control Digital", 6), m("Vision Artificial", 6), m("Sistemas Embebidos", 6),
    m("Inteligencia Artificial Aplicada", 7), m("Diseno Mecatronico", 7), m("Redes Industriales", 7), m("Manufactura Avanzada", 7),
    m("Automatizacion Industrial", 8), m("Robotica Movil", 8), m("Gestion de Proyectos", 8), m("Topicos de Mecatronica", 8),
    m("Programacion de Robots Industriales", 9), m("Estadia I", 9), m("Etica Profesional", 9), m("Sistemas SCADA", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Innovacion Tecnologica", 10),
)

// --- Ingenieria Industrial ---
private val materiasIndustrial: List<MateriaCatalogo> = listOf(
    m("Calculo Diferencial", 1), m("Algebra Lineal", 1), m("Dibujo Industrial", 1), m("Fundamentos de Ingenieria Industrial", 1),
    m("Calculo Integral", 2), m("Estatica", 2), m("Estudio del Trabajo I", 2), m("Quimica Industrial", 2),
    m("Probabilidad y Estadistica", 3), m("Calculo Vectorial", 3), m("Estudio del Trabajo II", 3), m("Materiales Industriales", 3),
    m("Investigacion de Operaciones I", 4), m("Ecuaciones Diferenciales", 4), m("Higiene y Seguridad Industrial", 4), m("Costos Industriales", 4),
    m("Investigacion de Operaciones II", 5), m("Control Estadistico de Calidad", 5), m("Administracion de Operaciones", 5), m("Ergonomia", 5),
    m("Planeacion y Control de la Produccion", 6), m("Logistica y Cadena de Suministro", 6), m("Mantenimiento Industrial", 6), m("Gestion de Calidad", 6),
    m("Simulacion de Sistemas", 7), m("Diseno de Plantas Industriales", 7), m("Sistemas de Manufactura", 7), m("Gestion Ambiental", 7),
    m("Manufactura Esbelta", 8), m("Automatizacion de Procesos", 8), m("Gestion de Proyectos Industriales", 8), m("Topicos de Calidad", 8),
    m("Cadena de Valor", 9), m("Estadia I", 9), m("Etica Profesional", 9), m("Innovacion y Mejora Continua", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Indicadores de Productividad", 10),
)

// --- Ingenieria en Tecnologias de Manufactura ---
private val materiasManufactura: List<MateriaCatalogo> = listOf(
    m("Calculo Diferencial", 1), m("Algebra Lineal", 1), m("Dibujo Asistido por Computadora", 1), m("Procesos de Manufactura I", 1),
    m("Calculo Integral", 2), m("Estatica", 2), m("Procesos de Manufactura II", 2), m("Ciencia de los Materiales", 2),
    m("Metrologia y Normalizacion", 3), m("Calculo Vectorial", 3), m("Manufactura Asistida por Computadora", 3), m("Maquinas y Herramientas", 3),
    m("Diseno de Procesos", 4), m("Ecuaciones Diferenciales", 4), m("Control de Calidad", 4), m("Sistemas Neumaticos e Hidraulicos", 4),
    m("Manufactura Esbelta", 5), m("Automatizacion de Procesos", 5), m("Programacion CNC", 5), m("Mantenimiento Industrial", 5),
    m("Manufactura Aditiva", 6), m("Sistemas de Manufactura Flexible", 6), m("Diseno de Herramentales", 6), m("Gestion de Calidad", 6),
    m("Robotica Aplicada a la Manufactura", 7), m("Planeacion de la Produccion", 7), m("Logistica Industrial", 7), m("Topicos de Manufactura Digital", 7),
    m("Manufactura Sustentable", 8), m("Sistemas de Manufactura Inteligente", 8), m("Gestion de Proyectos", 8), m("Mejora Continua", 8),
    m("Estadia I", 9), m("Innovacion Tecnologica", 9), m("Etica Profesional", 9), m("Cadena de Suministro", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Industria 4.0", 10),
)

// --- Ingenieria en Electronica y Telecomunicaciones ---
private val materiasElectronica: List<MateriaCatalogo> = listOf(
    m("Calculo Diferencial", 1), m("Algebra Lineal", 1), m("Circuitos Electricos I", 1), m("Fundamentos de Programacion", 1),
    m("Calculo Integral", 2), m("Circuitos Electricos II", 2), m("Electronica Analogica I", 2), m("Matematicas para Telecomunicaciones", 2),
    m("Calculo Vectorial", 3), m("Electronica Analogica II", 3), m("Sistemas Digitales", 3), m("Senales y Sistemas", 3),
    m("Ecuaciones Diferenciales", 4), m("Electronica de Potencia", 4), m("Microcontroladores", 4), m("Teoria de Comunicaciones", 4),
    m("Sistemas de Comunicaciones Analogicas", 5), m("Procesamiento Digital de Senales", 5), m("Redes de Datos I", 5), m("Antenas y Propagacion", 5),
    m("Sistemas de Comunicaciones Digitales", 6), m("Redes de Datos II", 6), m("Comunicaciones Moviles", 6), m("Sistemas Embebidos", 6),
    m("Redes Opticas", 7), m("Comunicaciones Satelitales", 7), m("Seguridad en Redes", 7), m("Topicos de IoT", 7),
    m("Redes de Nueva Generacion", 8), m("Sistemas de Radiofrecuencia", 8), m("Gestion de Proyectos de Telecomunicaciones", 8), m("Regulacion de Telecomunicaciones", 8),
    m("Estadia I", 9), m("Innovacion Tecnologica", 9), m("Etica Profesional", 9), m("Redes 5G y Tecnologias Emergentes", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Auditoria de Telecomunicaciones", 10),
)

// --- Ingenieria Financiera ---
private val materiasFinanciera: List<MateriaCatalogo> = listOf(
    m("Matematicas Financieras I", 1), m("Fundamentos de Economia", 1), m("Contabilidad Financiera I", 1), m("Ingles I", 1),
    m("Matematicas Financieras II", 2), m("Microeconomia", 2), m("Contabilidad Financiera II", 2), m("Ingles II", 2),
    m("Estadistica Financiera", 3), m("Macroeconomia", 3), m("Derecho Financiero", 3), m("Ingles III", 3),
    m("Analisis Financiero", 4), m("Mercados Financieros", 4), m("Costos para la Toma de Decisiones", 4), m("Ingles IV", 4),
    m("Administracion Financiera I", 5), m("Valuacion de Instrumentos Financieros", 5), m("Banca y Sistema Financiero Mexicano", 5), m("Ingles V", 5),
    m("Administracion Financiera II", 6), m("Riesgo Financiero", 6), m("Finanzas Corporativas", 6), m("Ingles VI", 6),
    m("Ingenieria Economica", 7), m("Mercado de Derivados", 7), m("Planeacion Financiera Estrategica", 7), m("Fiscal y Tributacion", 7),
    m("Finanzas Internacionales", 8), m("Modelos de Inversion", 8), m("Gestion de Portafolios", 8), m("Auditoria Financiera", 8),
    m("Estadia I", 9), m("Innovacion Financiera y Fintech", 9), m("Etica Profesional", 9), m("Analisis de Proyectos de Inversion", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Gobierno Corporativo", 10),
)

// --- Licenciatura en Gestion Empresarial ---
private val materiasGestionEmpresarial: List<MateriaCatalogo> = listOf(
    m("Fundamentos de Administracion", 1), m("Matematicas para Negocios", 1), m("Contabilidad General", 1), m("Ingles I", 1),
    m("Proceso Administrativo", 2), m("Microeconomia", 2), m("Derecho Empresarial", 2), m("Ingles II", 2),
    m("Administracion de Recursos Humanos", 3), m("Macroeconomia", 3), m("Estadistica para Negocios", 3), m("Ingles III", 3),
    m("Mercadotecnia", 4), m("Administracion Financiera", 4), m("Comportamiento Organizacional", 4), m("Ingles IV", 4),
    m("Administracion de Operaciones", 5), m("Costos y Presupuestos", 5), m("Negociacion y Ventas", 5), m("Ingles V", 5),
    m("Planeacion Estrategica", 6), m("Gestion de la Calidad", 6), m("Mercadotecnia Digital", 6), m("Ingles VI", 6),
    m("Desarrollo Organizacional", 7), m("Logistica y Cadena de Suministro", 7), m("Auditoria Administrativa", 7), m("Etica Empresarial", 7),
    m("Innovacion y Emprendimiento", 8), m("Gestion de Proyectos", 8), m("Plan de Negocios", 8), m("Gestion del Talento", 8),
    m("Estadia I", 9), m("Negocios Digitales", 9), m("Etica Profesional", 9), m("Alta Direccion", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Modelos de Negocio", 10),
)

// --- Licenciatura en Negocios Internacionales ---
private val materiasNegociosInternacionales: List<MateriaCatalogo> = listOf(
    m("Fundamentos de Negocios", 1), m("Matematicas para Negocios", 1), m("Comunicacion Efectiva", 1), m("Ingles I", 1),
    m("Microeconomia", 2), m("Contabilidad Financiera", 2), m("Estadistica para Negocios", 2), m("Ingles II", 2),
    m("Macroeconomia", 3), m("Mercadotecnia Internacional", 3), m("Derecho Internacional de los Negocios", 3), m("Ingles III", 3),
    m("Finanzas Internacionales", 4), m("Comercio Exterior", 4), m("Comportamiento Organizacional", 4), m("Ingles IV", 4),
    m("Negociacion Internacional", 5), m("Logistica Internacional", 5), m("Mercadotecnia Digital", 5), m("Frances I", 5),
    m("Aduanas y Tratados Internacionales", 6), m("Finanzas Corporativas", 6), m("Analisis de Mercados Globales", 6), m("Frances II", 6),
    m("Innovacion y Emprendimiento", 7), m("Estrategia de Negocios Internacionales", 7), m("Etica Empresarial", 7), m("Gestion del Talento", 7),
    m("Plan de Negocios Internacional", 8), m("Cadenas Globales de Valor", 8), m("Gestion de Proyectos", 8), m("Relaciones Comerciales Internacionales", 8),
    m("Estadia I", 9), m("Negocios Digitales", 9), m("Etica Profesional", 9), m("Mercados Emergentes", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Comercio Electronico Global", 10),
)

// --- Licenciatura en Psicologia ---
private val materiasPsicologia: List<MateriaCatalogo> = listOf(
    m("Introduccion a la Psicologia", 1), m("Biologia del Comportamiento", 1), m("Historia de la Psicologia", 1), m("Ingles I", 1),
    m("Psicologia del Desarrollo I", 2), m("Procesos Cognitivos Basicos", 2), m("Teorias de la Personalidad", 2), m("Ingles II", 2),
    m("Psicologia del Desarrollo II", 3), m("Psicologia Social", 3), m("Estadistica Aplicada a la Psicologia", 3), m("Ingles III", 3),
    m("Psicometria", 4), m("Psicopatologia I", 4), m("Tecnicas de Entrevista", 4), m("Ingles IV", 4),
    m("Psicopatologia II", 5), m("Evaluacion Psicologica", 5), m("Psicologia Educativa", 5), m("Etica Profesional", 5),
    m("Psicologia Clinica I", 6), m("Psicologia Organizacional", 6), m("Intervencion Psicoeducativa", 6), m("Investigacion Psicologica I", 6),
    m("Psicologia Clinica II", 7), m("Psicoterapia", 7), m("Seleccion y Capacitacion de Personal", 7), m("Investigacion Psicologica II", 7),
    m("Neuropsicologia", 8), m("Psicologia Familiar y de Pareja", 8), m("Desarrollo Organizacional", 8), m("Practicas Profesionales I", 8),
    m("Psicologia Comunitaria", 9), m("Estadia I", 9), m("Practicas Profesionales II", 9), m("Diagnostico Psicologico Integral", 9),
    m("Estadia Profesional", 10), m("Taller de Titulacion", 10), m("Seminario de Investigacion", 10), m("Practicas Profesionales III", 10),
)
