# Service Discovery (Descubrimiento de Servicios)

## Propósito y Beneficios
El patrón de **Service Discovery** permite que los microservicios se encuentren y comuniquen entre sí sin necesidad de conocer sus direcciones IP y puertos exactos (que pueden cambiar dinámicamente).
*   **Desacoplamiento**: Los servicios no necesitan configuración estática de las ubicaciones de otros servicios.
*   **Escalabilidad**: Nuevas instancias de servicios se registran automáticamente y están disponibles para su uso.
*   **Alta Disponibilidad**: El registro de servicios mantiene un control de qué instancias están activas (heartbeats).

## Implementación en el Proyecto
Este proyecto utiliza **Netflix Eureka** como servidor de descubrimiento.

### Dependencias
En los microservicios (como `favourite-service`, `api-gateway`, etc.), se incluye la dependencia del cliente Eureka:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Configuración
El servidor de descubrimiento se encuentra en el módulo `service-discovery`. Los clientes se configuran para registrarse en él, usualmente en `application.yml` o `bootstrap.yml`:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    register-with-eureka: true
    fetch-registry: true
```
