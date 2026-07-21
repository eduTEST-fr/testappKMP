# Guía breve de despliegue y pruebas finales

## Despliegue

1. Sube esta versión al repositorio conectado con Railway.
2. Vuelve a desplegar el servicio del backend.
3. Conserva las variables de MySQL, `JWT_SECRET` y `GROQ_API_KEY`.
4. Opcionalmente agrega:

```env
APP_TIME_ZONE=America/Mexico_City
```

5. No ejecutes SQL manual: no se agregó una tabla nueva.
6. Sincroniza Gradle en Android Studio, genera nuevamente la app e instálala en el dispositivo.

## Pruebas indispensables

### Materias y examen mensual

1. Crea una materia.
2. Comprueba que no se pueda guardar sin dificultad, tipo y fecha de examen.
3. Selecciona una fecha del mes actual y confirma que la materia se cree.
4. Usa una materia antigua sin examen del mes y verifica que aparezca la solicitud obligatoria.

### Bloqueo de materiales

1. Configura temporalmente un examen con la fecha de hoy.
2. Comprueba que Tarjetas muestre el aviso de examen.
3. Comprueba que Audios muestre el mismo bloqueo.
4. Intenta reproducir un episodio existente y verifica que el backend no entregue el archivo.
5. Cambia la fecha a un día futuro y confirma que ambos módulos vuelvan a estar disponibles.

### Recordatorios

1. Registra un examen próximo.
2. Abre la aplicación o ejecuta la sincronización de notificaciones.
3. Verifica la notificación en la barra de Android.
4. Confirma que el mensaje mencione dificultad, tiempo recomendado y recurso de repaso.

### Perfil y avatares

1. En una cuenta de alumno, abre `Mi perfil`, activa edición y toca la fotografía.
2. Selecciona otro animal, guarda y vuelve al Inicio.
3. Comprueba el mismo avatar en encabezado, menú, perfil y Comunidad.
4. En una cuenta de asesor, cambia el grado y verifica que el avatar se actualice automáticamente.
5. Edita únicamente la especialidad y confirma que el grado no se reinicie.

### Panel del asesor

1. Ingresa como asesor.
2. Revisa métricas, solicitudes recientes y accesos rápidos.
3. Confirma que no aparezcan accesos a tarjetas o audios.
4. Una llamada manual a esas rutas con token de asesor debe devolver `403`.

### Navegación Android

1. Entra a Perfil, Notificaciones, Tarjetas y una subcarpeta de Audios.
2. Usa el gesto Atrás.
3. Comprueba que primero cierre el diálogo o regrese al nivel anterior.
4. Desde una pantalla secundaria no debe cerrar la aplicación.
5. Desde la pantalla raíz, Atrás conserva el comportamiento normal de Android.

### Formularios

1. Abre la creación de un audio y de un conjunto de tarjetas.
2. Verifica que el diálogo tenga fondo blanco, acciones visibles y se cierre con Atrás.

## Consulta opcional en MySQL

```sql
SELECT id, nombre, dificultad, usuario_id
FROM materias
ORDER BY id DESC;

SELECT id, materia_id, nombre, fecha
FROM examenes
ORDER BY fecha DESC;
```
