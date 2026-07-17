# EduFlow — Implementación final de notificaciones y sesiones de estudio

Esta entrega parte del **commit 8 (ajustes finales)** y agrega únicamente los módulos solicitados. La navegación, autenticación, materias, StudyCast, Audio, Peers, perfiles y funciones existentes se conservan.

## Commit 9 — Notificaciones completas

### Objetivo
Centralizar los eventos importantes de EduFlow en la tabla `notificaciones` y convertir las notificaciones nuevas en avisos de la barra de Android.

### Flujos cubiertos

- Nueva pregunta publicada en Peers.
- Nueva respuesta a una pregunta.
- Cierre de una pregunta por un asesor o administrador.
- Calificación de una respuesta en Peers.
- Nueva solicitud de asesoría.
- Asesoría aceptada.
- Asesoría cancelada.
- Calificación de una asesoría.
- Recordatorio de asesoría aceptada para hoy o mañana.
- Recordatorio de examen según dificultad y cercanía.
- Confirmación de sesión de estudio guardada.

### Funcionamiento

1. El backend crea una fila persistente en MySQL por cada evento.
2. Android consulta las notificaciones mediante `WorkManager`.
3. Los registros nuevos y no leídos se muestran con `NotificationCompat` en la barra del sistema.
4. En Android 13 o posterior se solicita `POST_NOTIFICATIONS` al iniciar la aplicación.
5. Los recordatorios usan una clave única por usuario, evento y día para evitar duplicados.

### Frecuencia

- Al iniciar sesión se programa una revisión inmediata.
- Mientras la aplicación permanece abierta se solicita sincronización periódica.
- En segundo plano Android utiliza el intervalo mínimo permitido por `WorkManager`, que es aproximadamente 15 minutos. El sistema operativo puede ajustar el momento exacto para ahorrar batería.

Esta solución conserva el mecanismo existente del proyecto. No utiliza Firebase Cloud Messaging, por lo que los avisos son persistentes y confiables, pero no deben describirse como notificaciones push instantáneas con la aplicación completamente cerrada.

## Commit 10 — Sensor de proximidad y backend

### Arquitectura KMP

```text
shared/src/
├── commonMain/kotlin/com/example/eduflow/
│   ├── api/SesionesEstudioApi.kt
│   └── sensor/
│       ├── SensorRepository.kt
│       ├── SensorFilter.kt
│       ├── SesionEstudioViewModel.kt
│       ├── StudyRecommendation.kt
│       └── StudyTimer.kt
├── androidMain/kotlin/com/example/eduflow/sensor/
│   └── ProximitySensorManager.kt
├── jsMain/kotlin/com/example/eduflow/sensor/
│   └── SensorRepository.js.kt
└── wasmJsMain/kotlin/com/example/eduflow/sensor/
    └── SensorRepository.wasmJs.kt
```

La capa compartida contiene la lógica de negocio. Solamente `androidMain` accede a `SensorManager`, `SensorEventListener` y `Sensor.TYPE_PROXIMITY`.

### Flujo del cronómetro

- Al pulsar **Iniciar sesión**, se registra el listener.
- Una lectura cercana estable durante 300 ms inicia o reanuda el intervalo.
- Una lectura lejana estable durante 300 ms pausa el intervalo.
- El tiempo se calcula con timestamps, no incrementando una variable cada segundo.
- Al finalizar, se consolidan los intervalos activos y se envían al backend.

### Persistencia

Se agregó la tabla `sesiones_estudio`:

```text
id
usuario_id
materia_id
fecha
duracion_segundos
meta_minutos
created_at
```

Se guardan segundos para conservar precisión en pruebas cortas. El backend valida el JWT, comprueba que la materia pertenezca al usuario y limita una sesión a un máximo razonable de 12 horas.

### Endpoints

- `POST /sesiones`: registra una sesión.
- `GET /sesiones/materia/{id}`: devuelve las últimas sesiones y el tiempo total acumulado.

### Permiso del sensor

`TYPE_PROXIMITY` no requiere un permiso peligroso en tiempo de ejecución. No se agregó ubicación porque sería incorrecto. El manifiesto declara el hardware como opcional:

```xml
<uses-feature
    android:name="android.hardware.sensor.proximity"
    android:required="false" />
```

## Commit 11 — Interfaz de sesiones de estudio

### Acceso

En el Dashboard se agregó un botón verde con un reloj de arena blanco junto al botón existente para agregar materias.

### Flujo visual

1. Selección de una materia real del usuario.
2. Consulta del historial reciente y tiempo total.
3. Cálculo del tiempo ideal de estudio.
4. Presentación de instrucciones animadas para colocar el teléfono boca abajo.
5. Autorización informativa interna de EduFlow.
6. Cronómetro controlado por el sensor.
7. Pausa y reanudación automáticas al levantar o volver a colocar el teléfono.
8. Resumen y guardado en Railway/MySQL.

### Meta recomendada

La fórmula es sencilla y explicable:

```text
base = 20 + (dificultad × 5)
```

Se agregan:

- 15 minutos cuando el examen está a 0–2 días.
- 10 minutos cuando está a 3–5 días.
- Sin aumento cuando no existe examen próximo.
- Límite máximo de 90 minutos.

Ejemplo: dificultad 9 y examen en 5 días:

```text
20 + (9 × 5) + 10 = 75 minutos
```

### Animación

La instrucción se dibuja con `Canvas` de Compose. Muestra el teléfono de frente, una flecha de giro, el descenso hacia una mesa y la confirmación al quedar boca abajo. No usa una mano ni un GIF externo.

### Emulador

Cuando el emulador no contiene sensor de proximidad, la pantalla habilita los controles **Boca arriba** y **Boca abajo**. Estos sirven para demostrar la interfaz sin sustituir la implementación real utilizada en Android físico.
