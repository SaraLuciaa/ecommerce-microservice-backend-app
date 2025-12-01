# API Gateway (Puerta de Enlace de API)

## Propósito y Beneficios
El **API Gateway** actúa como un punto de entrada único para todas las solicitudes de los clientes externos hacia los microservicios.
*   **Enrutamiento Centralizado**: Dirige las peticiones al servicio correspondiente basándose en la ruta.
*   **Seguridad y Autenticación**: Puede manejar la autenticación y autorización en un solo lugar.
*   **Simplificación para el Cliente**: El cliente no necesita conocer la arquitectura interna ni los múltiples endpoints de los microservicios.

## Implementación en el Proyecto
Este proyecto utiliza **Spring Cloud Gateway**.

### Dependencias
El módulo `api-gateway` incluye:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

### Configuración
Las rutas se definen típicamente en `application.yml`. El Gateway se integra con Eureka para enrutar usando los nombres de los servicios (Load Balancing):

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true # Permite usar el nombre del servicio en la URL
      routes:
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
```
