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
