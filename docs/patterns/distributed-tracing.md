# Distributed Tracing (Rastreo Distribuido)

## Propósito y Beneficios
En una arquitectura de microservicios, una sola solicitud de usuario puede pasar por múltiples servicios. El **Rastreo Distribuido** permite seguir el flujo de esa solicitud a través de todo el sistema.
*   **Depuración**: Facilita encontrar dónde ocurrió un error en una cadena de llamadas.
*   **Monitoreo de Latencia**: Permite identificar cuellos de botella y servicios lentos.
*   **Visibilidad**: Proporciona una visión clara de las dependencias entre servicios.

## Implementación en el Proyecto
Se utiliza **Spring Cloud Sleuth** para generar trazas e IDs, y **Zipkin** para visualizar y almacenar estas trazas.

### Dependencias
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

### Configuración
En `application.yml`, se configura la URL del servidor Zipkin y el porcentaje de muestreo:

```yaml
spring:
  zipkin:
    base-url: http://localhost:9411
  sleuth:
    sampler:
      probability: 1.0 # Muestrear el 100% de las peticiones (para desarrollo)
```
