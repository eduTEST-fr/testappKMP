# Mejora de identidad visual y avatares

## Cambios realizados

- El inicio de sesión, el registro y los menús laterales ahora reutilizan el recurso oficial `eduflow_icon.png`, el mismo símbolo usado por la identidad de la aplicación.
- Se añadió el componente compartido `EduFlowBrandIcon` para evitar volver a dibujar o sustituir el logotipo en cada pantalla.
- Se conservaron los avatares automáticos de asesores por grado académico.
- Se rediseñaron los seis avatares de alumno para que sean más reconocibles y tengan un elemento académico distinto:
  - Búho lector con lentes y libro.
  - Zorro matemático con hoja y lápiz.
  - Gato investigador con lentes y cuaderno.
  - Conejo de apuntes con nota y lápiz.
  - Panda científico con bata y matraz.
  - Mapache graduado con birrete y diploma.
- Los ids existentes (`student_buho`, `student_zorro`, etc.) se conservaron para mantener compatibilidad con los perfiles guardados en MySQL.
- Se simplificaron los textos del selector de avatar y del panel de asesor para que expliquen una función real y no parezcan texto promocional.
- Se corrigió la llamada a `drawArc` del icono de audios utilizando el parámetro nombrado `style`, evitando el error de compilación reportado.

## Archivos principales modificados

- `shared/src/commonMain/kotlin/com/example/eduflow/ui/StudyUiComponents.kt`
- `shared/src/commonMain/kotlin/com/example/eduflow/ui/AvatarIcons.kt`
- `shared/src/commonMain/kotlin/com/example/eduflow/ui/LoginView.kt`
- `shared/src/commonMain/kotlin/com/example/eduflow/ui/RegisterView.kt`
- `shared/src/commonMain/kotlin/com/example/eduflow/ui/DashboardView.kt`
- `shared/src/commonMain/kotlin/com/example/eduflow/ui/PeersView.kt`

La URL del backend se mantiene en:

`https://testappkmp-production-e13d.up.railway.app`
