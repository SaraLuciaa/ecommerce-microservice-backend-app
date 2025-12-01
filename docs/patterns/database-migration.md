# Database Migration (Migración de Base de Datos)

## Propósito y Beneficios
Gestiona los cambios en el esquema de la base de datos de manera controlada, versionada y reproducible.
*   **Consistencia**: Asegura que todos los entornos (dev, test, prod) tengan la misma estructura de base de datos.
*   **Historial**: Mantiene un registro de todos los cambios aplicados.
*   **Automatización**: Las migraciones se ejecutan automáticamente al iniciar la aplicación.

## Implementación en el Proyecto
Se utiliza **Flyway**.

### Dependencias
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Estructura
Los scripts SQL de migración se colocan en `src/main/resources/db/migration`. Siguen una convención de nombres específica, por ejemplo:
*   `V1__init_schema.sql`
*   `V2__add_column_users.sql`

Flyway detecta estos archivos y los aplica en orden secuencial al arrancar el microservicio.
