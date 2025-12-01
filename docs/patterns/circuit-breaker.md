# Circuit Breaker (Cortocircuito)

## Propósito y Beneficios
El patrón **Circuit Breaker** previene que una aplicación intente realizar una operación que probablemente fallará, permitiendo que continue operando sin esperar a que se agote el tiempo de espera (timeout) de la operación fallida.
*   **Tolerancia a Fallos**: Evita fallos en cascada cuando un servicio dependiente no está disponible.
*   **Resiliencia**: Permite que el sistema se recupere y maneje errores de manera elegante (fallback).
*   **Monitoreo**: Proporciona información sobre el estado de las dependencias.

## Implementación en el Proyecto
Se utiliza **Resilience4j** a través de Spring Cloud Circuit Breaker.

### Dependencias
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Ejemplo de Código
En `favourite-service` u otros servicios, se anota el método susceptible a fallos:

```java
@CircuitBreaker(name = "userService", fallbackMethod = "fallbackMethod")
public UserDto getUserById(Integer userId) {
    // Llamada a servicio externo
    return userClient.getUserById(userId);
}

public UserDto fallbackMethod(Integer userId, Throwable t) {
    // Retornar respuesta por defecto o caché
    return new UserDto();
}
```
