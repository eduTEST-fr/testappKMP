# Guía de pruebas y evidencias

## 1. Preparación

1. Abre el proyecto final en Android Studio.
2. Sincroniza Gradle.
3. Verifica que `ApiConfig.BASE_URL` apunte al backend desplegado en Railway.
4. Despliega primero el backend actualizado para que `SchemaUtils.createMissingTablesAndColumns()` cree o complete las tablas.
5. Inicia sesión con un usuario de tipo alumno que tenga al menos una materia registrada.

Antes de actualizar una base de datos de producción, es recomendable realizar un respaldo desde Railway.

## 2. Prueba de la interfaz

1. Entra al Dashboard.
2. Comprueba que, junto al botón `+`, aparece el botón verde con reloj de arena.
3. Presiónalo.
4. Selecciona una materia.
5. Verifica que aparezcan su dificultad, historial y tiempo acumulado.
6. Continúa y comprueba la meta ideal.
7. Revisa la animación que indica cómo colocar el teléfono.

## 3. Prueba en emulador

1. Presiona **Iniciar sesión**.
2. Acepta la autorización informativa.
3. Usa **Boca abajo** para iniciar el cronómetro.
4. Usa **Boca arriba** para pausarlo.
5. Repite ambos estados y confirma que el tiempo se conserva.
6. Finaliza la sesión.
7. Comprueba el mensaje de guardado correcto.
8. Vuelve a seleccionar la materia y verifica que la sesión aparezca en el historial.

## 4. Prueba en teléfono físico

1. Instala la aplicación en un dispositivo con sensor de proximidad.
2. Inicia una sesión.
3. Coloca el teléfono con la pantalla sobre una superficie plana.
4. Espera al menos 300 ms y confirma que el estado cambie a **ESTUDIO ACTIVO**.
5. Levanta el teléfono y confirma que cambie a **CRONÓMETRO PAUSADO**.
6. Vuelve a colocarlo y verifica que reanude desde el tiempo acumulado.
7. Levántalo y pulsa **Finalizar sesión**.

La lectura exacta puede variar por fabricante. La implementación considera cercano cualquier valor menor al rango máximo informado por el sensor, que es el criterio habitual para sensores binarios de proximidad.

## 5. Prueba del backend

Después de guardar una sesión, consulta MySQL en Railway:

```sql
SELECT id, usuario_id, materia_id, fecha,
       duracion_segundos, meta_minutos, created_at
FROM sesiones_estudio
ORDER BY id DESC;
```

También puede probarse el historial desde la aplicación. El backend rechaza materias que no pertenecen al usuario autenticado.

## 6. Prueba de notificaciones

### Peers

- Usuario A publica una pregunta.
- Usuario B recibe **Nueva pregunta en Peers**.
- Usuario B responde.
- Usuario A recibe el aviso de respuesta.
- Usuario A califica la respuesta.
- Usuario B recibe el aviso de calificación.

### Asesorías

- Alumno solicita una asesoría.
- Asesor recibe la nueva solicitud.
- Asesor acepta o cancela.
- Alumno recibe el resultado.
- Alumno califica una asesoría aceptada.
- Asesor recibe la calificación.

### Exámenes

1. Registra un examen próximo.
2. Usa una materia de dificultad alta, por ejemplo 9.
3. Al generar recordatorios dentro de los siete días previos, debe crearse una notificación con la meta sugerida.
4. La clave diaria evita que se duplique el mismo recordatorio.

En Android 13 o posterior debe autorizarse el permiso de notificaciones. Con la aplicación en segundo plano, `WorkManager` puede tardar alrededor de 15 minutos porque Android controla la ejecución exacta.

## 7. Capturas recomendadas para la actividad

### Captura 1 — Archivos y directorios

Muestra en Android Studio:

- `commonMain/.../sensor/`
- `androidMain/.../sensor/ProximitySensorManager.kt`
- `commonMain/.../ui/SesionEstudioView.kt`
- `backend/.../routes/SesionRoutes.kt`
- `backend/.../model/Tables.kt`

### Captura 2 — Interfaz emulada

Muestra la sesión activa con:

- Materia seleccionada.
- Cronómetro visible.
- Estado **ESTUDIO ACTIVO**.
- Progreso respecto a la meta.

### Captura 3 — Autorización

Muestra el cuadro **Autorizar uso del sensor**, donde se explica que:

- Solo se detecta si el teléfono está boca abajo.
- No se recopilan imágenes, audio, ubicación ni datos personales.
- `TYPE_PROXIMITY` no requiere un permiso oficial de Android.

### Capturas adicionales

- Botón verde en el Dashboard.
- Selección de materia e historial.
- Animación de colocación.
- Resumen guardado.
- Registro en MySQL.
- Notificación en la barra de Android.
