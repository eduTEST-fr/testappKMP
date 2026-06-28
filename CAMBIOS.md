# Cambios aplicados

## 1. Bug raíz: las materias (y tarjetas/podcasts/exámenes/red de apoyo) no se veían

**Causa:** En el backend, varios endpoints `GET` armaban la respuesta así:

```kotlin
.map { mapOf(
    "id"         to it[Materias.id].value,   // Int
    "nombre"     to it[Materias.nombre],     // String
    "dificultad" to it[Materias.dificultad]  // Int
)}
```

Como el `mapOf` mezcla tipos (`Int` y `String`), Kotlin infiere `Map<String, Any>`.
`kotlinx.serialization` (el plugin de JSON que usa Ktor) **no sabe serializar `Any`**
sin un serializer especial — así que el servidor lanzaba una excepción 500 al
responder. Tu app atrapa esos errores en silencio (`catch (e: Exception) {}`),
por eso la pantalla se quedaba como si no hubiera materias, aunque ya estuvieran
guardadas en MySQL (el `POST` sí funcionaba porque esa respuesta solo tenía un
campo `Int`, sin mezcla de tipos).

Este mismo patrón rompía: `GET /materias`, `GET /materias/{id}/examenes`,
`GET /tarjetas/{materiaId}`, `POST /tarjetas/generar`, `GET /podcasts/{materiaId}`,
`POST /podcasts/generar` y `GET /red-apoyo`.

**Arreglo:** se agregaron data classes `@Serializable` (`MateriaResponse`,
`ExamenResponse`, `TarjetaResponse`, `PodcastResponse`, `RedApoyoResponse`, etc.)
en `backend/.../model/DTOs.kt`, y cada ruta ahora responde con esos tipos en vez
de `mapOf(...)` mixto. El JSON resultante es idéntico en forma y nombres de campo
al que ya esperaba tu app (el parseo por regex en el cliente no se tocó), así que
no rompe nada del lado de Android.

Archivos tocados: `DTOs.kt`, `MateriaRoutes.kt`, `ExamenRoutes.kt`,
`TarjetaRoutes.kt`, `PodcastRoutes.kt`, `RedApoyoRoutes.kt`.

## 2. "Audio" decía "Próximamente"

El botón "Audio" de la barra inferior, tanto en el Dashboard como en StudyCast,
estaba conectado a un diálogo fijo de "Próximamente" (`mostrarProximamente = true`),
aunque la función de Audio/Podcast ya estaba implementada (genera guion con IA,
lo convierte a voz con Orpheus TTS, lo guarda en MySQL y lo reproduce con
MediaPlayer en Android) dentro de la pestaña "Podcast" de StudyCast.

**Arreglo:** ahora el botón "Audio" navega directo a esa pestaña ya funcional:

- `App.kt` ahora recuerda qué pestaña de StudyCast abrir.
- `DashboardView.kt`: `onVerStudyCast` recibe la pestaña a abrir
  (`TabStudyCast.PODCAST` para "Audio", `TabStudyCast.CONSEJOS` para "StudyCast").
- `StudyCastView.kt`: acepta un parámetro `tabInicial` y su propio botón "Audio"
  ahora cambia a la pestaña Podcast en vez de no hacer nada.

"Peers" se dejó igual (sigue mostrando "Próximamente") porque no tiene ninguna
pantalla implementada en el frontend todavía — no había nada que conectar.

## 3. Recordatorio importante (no es un bug de código)

Para que **Consejo**, **Tarjetas** y **Podcast/Audio** funcionen de verdad en
producción, el backend necesita la variable de entorno `GROQ_API_KEY` configurada
en Railway (pestaña *Variables* del servicio `testappKMP`). Si no la has puesto,
esas tres funciones devolverán error aunque el resto de la app funcione bien.
También revisa que `JWT_SECRET` esté seteado (si no, usa un valor por defecto
`"dev_secret"`, lo cual funciona pero no es recomendable dejarlo así en producción).

## Qué NO se tocó

- La conexión a MySQL (`DatabaseFactory.kt`) se dejó intacta a propósito para no
  arriesgar que Railway deje de conectar. Sí vale la pena, cuando tengas tiempo,
  mover esa URL/contraseña hardcodeada a variables de entorno de Railway en vez
  de tenerla en el código fuente — pero eso es una mejora de seguridad, no un bug.
- No se tocó nada de autenticación, registro o login (ya funcionaban bien).
- No se cambiaron nombres de campos JSON, así que ningún regex existente en el
  cliente Android se rompe.

## 4. Rediseño visual completo: Dashboard, StudyCast, Audios, Perfil + Splash

Este round fue principalmente visual/de flujo, partiendo de los mockups
(`stitch_uniflow_smart_student_hub`) y los logos entregados, sin tocar la
lógica que ya funcionaba (login, registro, JWT, MySQL, generación con Groq,
TTS). Resumen de lo que cambió:

### Splash Screen (nuevo)
- `ui/SplashScreenView.kt`: logo real de EduFlow (recortado de los PNG
  entregados, con fondo transparente) sobre el beige de marca, barra de
  progreso animada y leyenda. `App.kt` arranca siempre en `Pantalla.SPLASH`
  y, al terminar, decide Login o Dashboard según `SesionStorage.haySesion()`.
- Los logos finales están en
  `shared/src/commonMain/composeResources/drawable/eduflow_logo.png`
  (logo + texto) y `eduflow_icon.png` (solo el ícono, usado en el menú lateral
  del Dashboard).

### Biblioteca de Estudio (antes "StudyCast")
- `ui/StudyCastView.kt` ahora es una biblioteca real: lista de materias como
  carpetas → al entrar, **Consejo de estudio** (igual que antes) +
  **subcarpetas de tarjetas por tema**.
- `ui/TarjetasView.kt` se reescribió: las tarjetas ahora tienen un campo
  `tema` (la "subcarpeta"). Se puede crear una subcarpeta nueva indicando
  nombre del tema + apuntes/contenido, y cada subcarpeta tiene un botón
  **"Marcar tema como estudiado"** que valida el progreso real.

### Biblioteca de Audios (nueva, antes el botón "Audio" solo abría StudyCast)
- `ui/AudiosView.kt`: misma lógica de carpetas que Estudio pero para los
  podcasts generados con IA (materia → subcarpetas por tema → episodios).
  Cada episodio tiene su reproductor y un botón **"Marcar como escuchado"**.
- `ui/AudioPlayerView.kt` cambió de firma: antes reproducía "el primer
  podcast de la materia"; ahora reproduce un episodio puntual por su URL,
  reutilizable desde cualquier subcarpeta. Actualizado en androidMain
  (MediaPlayer, igual que antes) y en los stubs de jsMain/wasmJsMain.

### Dashboard
- Accesos directos correctos: el botón "Audio" de la barra inferior ahora sí
  abre la Biblioteca de Audios (antes reabría StudyCast).
- El contador de "Pendientes" ahora refleja materias con tarjetas sin
  terminar de estudiar (antes solo contaba el total de materias).
- Mini-tarjeta de perfil (carrera/cuatrimestre) con acceso a "Gestionar
  perfil", y botón "Mi perfil" en el menú lateral.

### Perfil (nuevo, para la Red de Apoyo)
- `ui/PerfilView.kt` + `storage/PerfilStorage.kt`: carrera, cuatrimestre,
  ubicación, "sobre mí" y materias en las que destaca. Tal como se pidió, es
  **solo local** (no pega al backend); se guarda en el dispositivo con
  `multiplatform-settings`, igual que ya hacía `SesionStorage`.
- `ui/PeersView.kt` se dejó tal cual (sigue siendo solo visual) y ahora el
  avatar de su barra superior abre este Perfil.

### Backend (cambios aditivos, no rompen nada existente)
- `Tarjetas` y `Podcasts` ganaron dos columnas nuevas: `tema` (varchar,
  default `"General"`) y `completado` (boolean, default `false`). Como
  `DatabaseFactory` ya corre `SchemaUtils.createMissingTablesAndColumns` al
  iniciar, Railway las agrega solas la próxima vez que el backend arranque.
- `GenerarTarjetasRequest` ganó un campo opcional `tema` (si no llega, usa
  `"General"`, así que clientes viejos no se rompen).
- Rutas nuevas: `PUT /tarjetas/completar` (marca un tema completo) y
  `PUT /podcasts/{id}/completar` (marca un episodio escuchado). Las rutas
  existentes (`GET`/`POST`) no cambiaron de URL ni de comportamiento, solo
  devuelven `tema` y `completado` además de los campos que ya tenían.

### Qué NO se tocó
- Login, registro, JWT, conexión a MySQL: igual que antes.
- Los nombres de los campos JSON que ya existían no cambiaron — solo se
  agregaron campos nuevos, así que nada de lo viejo se rompe.
- `Red de Apoyo` sigue siendo solo visual, sin lógica nueva, tal como se pidió.

---

## EP10 — Carreras, Materias UPT y Agendado de Asesorías

### Catálogo UPT (Funcionalidad A)
- Nuevo `shared/.../data/CatalogoUPT.kt`: catálogo fijo de 5 carreras y sus
  materias por cuatrimestre (6+ para Sistemas Computacionales, 4+ para las
  demás). Vive en el frontend porque son datos estáticos, no hay tabla nueva.
- `ui/RegisterView.kt` y `ui/PerfilView.kt` reemplazan el campo de texto libre
  de carrera por `SelectorCarrera` (dropdown sobre el catálogo). Tras
  registrarse, se hace un `PUT /peers/perfil` con la carrera elegida.
- `PerfilView.kt`: "Materias en las que destaca" ahora se elige con checkboxes
  del catálogo de la carrera seleccionada, con tope de 5 materias. El backend
  sigue guardando el CSV igual que antes (`materiasDestaca`), sin tablas nuevas.

### Ver todos los asesores (Funcionalidad B)
- `GET /peers/asesores` ya existía en el backend (devuelve todos los ASESOR
  con promedio de estrellas); no se tocó.
- Nuevas pantallas: `AsesoresListaView.kt` (lista completa, ordenada por
  promedio) y `PerfilAsesorView.kt` (perfil público con botón "Agendar
  asesoría" si `permiteAsesoria` está activo).
- `PeersView.kt`: el botón "Ver todos los asesores" y las tarjetas de
  "Asesores Destacados" ahora navegan a estas pantallas.

### Agendado de asesorías (Funcionalidad C)
- 3 tablas nuevas: `asesor_disponibilidad`, `asesorias`, `notificaciones`
  (ver `migracion_EP10_asesorias.sql`; `DatabaseFactory` también las crea
  solas al iniciar el backend, igual que el resto del esquema).
- Rutas nuevas en `AsesoriaRoutes.kt`:
  `PUT /asesorias/disponibilidad`, `GET /asesorias/disponibilidad/mia`,
  `GET /asesorias/disponibilidad/{asesorId}`, `POST /asesorias`,
  `GET /asesorias/mis-asesorias`, `PUT /asesorias/{id}/aceptar`,
  `PUT /asesorias/{id}/cancelar`, `GET /notificaciones`,
  `PUT /notificaciones/{id}/leer`.
- `PerfilView.kt` (rol Asesor): nueva sección "Disponibilidad para
  asesorías" con toggles Lun–Vie y horas de inicio/fin.
- Nuevas pantallas: `AgendarAsesoriaView.kt` (Alumno elige día y horario
  libres), `MisAsesoriasView.kt` (Alumno y Asesor ven sus asesorías; el
  Asesor puede aceptar con mensaje/enlace/ubicación o cancelar) y
  `NotificacionesView.kt` (bandeja de avisos).
- `DashboardView.kt`: ícono de notificaciones con contador de no leídas en
  la barra superior, y acceso a "Mis Asesorías" desde el menú lateral.
- `App.kt`: nuevas pantallas en el enum `Pantalla` y su navegación:
  `ASESORES_LISTA`, `PERFIL_ASESOR`, `AGENDAR_ASESORIA`, `MIS_ASESORIAS`,
  `NOTIFICACIONES`.

### Qué NO se tocó
- Login, registro (estructura general), JWT, StudyCast, Audios: igual que
  antes.
- Las rutas y tablas de EP8/EP9 (Red de Apoyo) no cambiaron de comportamiento.

---

## EP10.1 — Correcciones reportadas tras prueba en dispositivo

1. **Catálogo UPT corregido**: se reemplazaron las 5 carreras inventadas por
   las 10 carreras reales de la Universidad Politécnica de Tulancingo
   (confirmadas en upt.edu.mx): Ing. en Sistemas Computacionales, Ing. Civil,
   Ing. en Robótica, Ing. Industrial, Ing. en Tecnologías de Manufactura,
   Ing. en Electrónica y Telecomunicaciones, Ing. Financiera, Lic. en Gestión
   Empresarial, Lic. en Negocios Internacionales, Lic. en Psicología. Además
   ahora son 10 cuatrimestres (duración real confirmada en el plan de
   estudios oficial), no 9.
2. **Selector de cuatrimestre**: se reemplazó el campo de texto libre por un
   dropdown (`SelectorCuatrimestre`) que solo permite elegir del 1 al 10, en
   `RegisterView` y `PerfilView`.
3. **Bug: el botón "Mi perfil" del Asesor no hacía nada.** En
   `PeersView.kt`, `PeersAsesorView` nunca recibía `onVerPerfil` y el botón
   solo cerraba el menú lateral sin navegar. Se agregó el parámetro y se
   conectó correctamente — ahora el Asesor sí entra a su perfil y puede
   editar materias, grado, especialidad y horarios de asesoría.
4. **Bug: no se podía calificar a un asesor.** En `SolicitudDetalleView.kt`
   la condición para mostrar el formulario de calificación tenía la lógica
   invertida (`!esSolicitante` en vez de `esSolicitante`), por lo que
   excluía justo a la persona que debía calificar (quien hizo la pregunta).
   Corregido.
5. **Eliminar respuestas desde Asesor**: se agregó `DELETE
   /peers/respuestas/{id}` en `PeersRoutes.kt` (Alumno solo puede borrar la
   propia; Asesor/Admin pueden borrar cualquiera). `SolicitudDetalleView`
   ahora separa el permiso de "eliminar solicitud" (solo Admin) del de
   "eliminar respuesta" (Admin, Asesor, o el propio autor de la respuesta),
   y usa la nueva ruta en vez de la de `/admin/...` que antes daba 403.
