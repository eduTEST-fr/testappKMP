# Notas de despliegue en Railway

1. Sube el contenido del ZIP final al repositorio que Railway utiliza para el backend.
2. Conserva las variables `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` y las variables JWT ya utilizadas por el proyecto.
3. Fuerza un nuevo despliegue del servicio backend.
4. Revisa los logs de arranque. `DatabaseFactory.init()` ejecuta `SchemaUtils.createMissingTablesAndColumns()` e incorpora:
   - `sesiones_estudio`
   - `asesorias_calificaciones`
   - columnas nuevas de `notificaciones`
5. Comprueba que el servicio responda antes de instalar la nueva aplicación.

## Verificación rápida

```sql
SHOW TABLES;
DESCRIBE sesiones_estudio;
DESCRIBE asesorias_calificaciones;
DESCRIBE notificaciones;
```

La tabla `notificaciones` debe contener `tipo`, `referencia_id` y `clave`. La columna `clave` permite deduplicar recordatorios diarios.

## Observación

`SchemaUtils` evita ejecutar SQL manual en una instalación normal. Si el usuario de MySQL no tiene permisos para alterar tablas, Railway mostrará el error en los logs y será necesario otorgar permisos o realizar la migración con un usuario autorizado.
