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
