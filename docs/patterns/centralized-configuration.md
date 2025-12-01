# Centralized Configuration (Configuración Centralizada)

## Propósito y Beneficios
Este patrón externaliza la configuración de todos los microservicios en un servicio central.
*   **Consistencia**: Asegura que la configuración sea consistente entre múltiples instancias.
*   **Cambios Dinámicos**: Permite cambiar la configuración sin necesidad de reconstruir o reiniciar los servicios (si se usa `@RefreshScope`).
*   **Gestión Simplificada**: Un solo lugar para gestionar propiedades de entorno (dev, prod, test).

## Implementación en el Proyecto
Se utiliza **Spring Cloud Config Server**.

### Dependencias
El servidor (`cloud-config`) usa `spring-cloud-config-server`. Los clientes usan:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

### Configuración
Los microservicios apuntan al servidor de configuración en su `bootstrap.yml` (o `application.yml`):

```yaml
spring:
  cloud:
    config:
      uri: http://localhost:8888
      name: favourite-service # Nombre del archivo de configuración a buscar
```
