# EduFlow — mejoras de materias y audios

## Cambios principales

### Portadas de materias

- La portada se genera localmente con Compose Canvas.
- El diseño se selecciona por palabras clave del nombre de la materia.
- El formulario de nueva materia muestra una vista previa inmediata.
- El Dashboard usa una versión compacta de la misma portada.
- La biblioteca de audios conserva la versión amplia con categoría y nombre.

### Reproductor de audios

En Android, cada episodio incluye:

- Reproducir y pausar.
- Retroceder 10 segundos.
- Adelantar 10 segundos.
- Barra de progreso.
- Tiempo transcurrido y duración total.
- Reintento en caso de error.
- Carga bajo demanda: el MP3 se descarga únicamente al abrir el reproductor.
- Pausa automática del episodio anterior.
- Marcado automático como escuchado al finalizar.

### Revisión lógica aplicada

- Las rutas de podcasts ahora exigen JWT.
- Se valida que la materia, examen y episodio pertenezcan al usuario.
- El nombre de la materia se limpia antes de guardarse.
- La dificultad se valida en el rango de 1 a 10.
- El audio se almacena temporalmente en la caché interna, sin permiso de almacenamiento.

## Railway

El backend no requiere una tabla nueva ni una migración manual. Se conserva la tabla `podcasts` y su columna `audio_bytes`.

Después de subir los cambios al repositorio, vuelve a desplegar el servicio backend para aplicar la validación de rutas. La aplicación está configurada con:

```text
https://testappkmp-production-e13d.up.railway.app
```

## Prueba recomendada

1. Crear una materia y comprobar que la portada cambia mientras se escribe el nombre.
2. Guardar la materia y verificar la misma identidad visual en Dashboard y Audio.
3. Generar o abrir un StudyCast existente.
4. Probar reproducir, pausar, retroceder y adelantar 10 segundos.
5. Iniciar un segundo episodio y comprobar que el primero se pausa.
6. Terminar un episodio y verificar que aparece como escuchado.
7. Cerrar y volver a abrir la aplicación para confirmar que los episodios siguen disponibles desde MySQL.

## Archivos modificados

```text
androidApp/src/main/AndroidManifest.xml
backend/src/main/kotlin/com/eduflow/routes/MateriaRoutes.kt
backend/src/main/kotlin/com/eduflow/routes/PodcastRoutes.kt
shared/src/commonMain/kotlin/com/example/eduflow/config/ApiConfig.kt
shared/src/commonMain/kotlin/com/example/eduflow/ui/MateriaIlustracion.kt
shared/src/commonMain/kotlin/com/example/eduflow/ui/DashboardView.kt
shared/src/commonMain/kotlin/com/example/eduflow/ui/AudiosView.kt
shared/src/commonMain/kotlin/com/example/eduflow/ui/AudioPlayerView.kt
shared/src/androidMain/kotlin/com/example/eduflow/ui/AudioPlayerView.kt
shared/src/jsMain/kotlin/com/example/eduflow/ui/AudioPlayerView.kt
shared/src/wasmJsMain/kotlin/com/example/eduflow/ui/AudioPlayerView.kt
```
