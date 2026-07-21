# EduFlow — mejoras finales de experiencia y control académico

## Alcance

Esta versión conserva la estructura KMP, Compose, Ktor, JWT, Railway y MySQL del proyecto. Los cambios se concentraron en materias, exámenes, materiales de estudio, perfiles, navegación y panel del asesor.

## Cambios principales

### 1. Control mensual de exámenes

- Al crear una materia se solicita obligatoriamente:
  - nombre;
  - dificultad del 1 al 10;
  - tipo de evaluación;
  - fecha dentro del mes actual.
- Al comenzar un mes nuevo, EduFlow detecta las materias sin evaluación registrada y solicita completar la fecha.
- Una falla temporal de red ya no se interpreta como “falta de examen”; el aviso aparece únicamente después de validar el calendario con el servidor.

### 2. Bloqueo de tarjetas y audios

- Sin examen del mes, las tarjetas y los audios permanecen bloqueados.
- En la fecha exacta del examen, ambos materiales permanecen bloqueados durante el día.
- La restricción se valida en la interfaz y también en el backend.
- Las rutas de generación, listado, reproducción y marcado de progreso verifican JWT, propiedad de la materia y rol `ALUMNO`.
- El archivo de audio también está protegido; no puede reproducirse mediante su endpoint durante el bloqueo.

### 3. Recordatorios de estudio

El backend genera avisos según dificultad y cercanía del examen:

- dificultad 9–10: desde 7 días antes;
- dificultad 7–8: desde 5 días antes;
- dificultad 5–6: desde 4 días antes;
- dificultad 3–4: desde 3 días antes;
- dificultad 1–2: desde 2 días antes.

La recomendación diaria parte de `20 + dificultad × 5` minutos y se ajusta por cercanía, con límite de 90 minutos. Android convierte las notificaciones no leídas en avisos de la barra mediante el sistema ya existente.

### 4. Controles más intuitivos

Se sustituyeron campos libres por controles definidos para:

- dificultad de la materia;
- tipo de examen;
- fecha del examen;
- grado académico del asesor;
- carrera y cuatrimestre, usando el catálogo existente.

### 5. Identidad visual y avatares

- El cuadro `SF` del acceso fue reemplazado por una marca vectorial propia de EduFlow.
- Se eliminaron las letras usadas como avatar en encabezados, menú y perfil.
- Los alumnos pueden elegir entre seis avatares académicos de animales dibujados con Compose Canvas.
- El avatar del asesor se asigna automáticamente según su grado:
  - Licenciatura;
  - Maestría;
  - Doctorado.
- Se conserva compatibilidad con los identificadores antiguos `avatar_1` a `avatar_6`.

### 6. Panel del asesor

El asesor cuenta con un panel diferenciado que muestra:

- preguntas abiertas;
- asesorías pendientes;
- asesorías aceptadas;
- actividad gestionada;
- accesos a perfil y agenda;
- solicitudes recientes de la comunidad.

El panel no expone tarjetas ni audios de alumnos. El backend también rechaza estas rutas para cuentas que no tengan rol `ALUMNO`.

### 7. Navegación

- El gesto o botón Atrás de Android recorre el historial interno de EduFlow.
- Los diálogos y subcarpetas se cierran antes de abandonar el módulo.
- Las pantallas principales evitan acumular ciclos innecesarios en el historial.
- La aplicación solo se cierra al usar Atrás desde la pantalla raíz.
- Los botones visibles de regreso se hicieron más grandes y claros.
- La navegación inferior utiliza únicamente: `Inicio`, `Tarjetas`, `Audios` y `Comunidad`.

### 8. Ventanas de creación

Los formularios para generar audios y tarjetas ahora utilizan diálogos blancos, jerarquía visual clara, acciones separadas y mensajes breves. Se eliminó el panel gris que aparecía detrás del contenido.

### 9. Fecha consistente en Railway

Railway puede ejecutar en UTC. El backend ahora usa una zona académica configurable para evitar que el bloqueo cambie varias horas antes o después del día real:

```env
APP_TIME_ZONE=America/Mexico_City
```

La variable es opcional porque ese valor ya funciona como predeterminado.

## Configuración conservada

```text
https://testappkmp-production-e13d.up.railway.app
```

## Base de datos

Esta entrega no requiere una tabla nueva ni SQL manual. Las materias y los exámenes continúan utilizando las tablas existentes. `SchemaUtils.createMissingTablesAndColumns()` mantiene el esquema necesario al iniciar el backend.
