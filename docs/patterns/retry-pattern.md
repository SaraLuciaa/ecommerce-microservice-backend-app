# Retry Pattern (Patrón de Reintento)

## Propósito y Beneficios
El patrón **Retry** permite que una aplicación maneje fallos transitorios (como una caída momentánea de la red o un servicio temporalmente no disponible) reintentando automáticamente una operación fallida un número específico de veces antes de rendirse.
*   **Resiliencia**: Aumenta la probabilidad de éxito en entornos distribuidos donde los fallos temporales son comunes.
*   **Estabilidad**: Evita que fallos momentáneos se propaguen y causen errores al usuario final.
*   **Configurabilidad**: Permite definir cuántas veces reintentar y cuánto esperar entre intentos (backoff).

## Implementación en el Proyecto
Se utiliza **Resilience4j** a través de Spring Cloud Circuit Breaker, específicamente su módulo de Retry.

### Dependencias
La funcionalidad de Retry viene incluida en la dependencia de Resilience4j que ya se usa para el Circuit Breaker:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Configuración
En `application.yml` (ejemplo de `favourite-service`), se configuran los parámetros del reintento:

```yaml
resilience4j:
  retry:
    instances:
      favouriteService:
        max-retry-attempts: 3          # Número máximo de intentos
        wait-duration: 2s              # Tiempo de espera inicial entre intentos
        enable-exponential-backoff: true # Aumentar el tiempo de espera exponencialmente
        exponential-backoff-multiplier: 2 # Multiplicador para el backoff
```

### Ejemplo de Código
Se anota el método con `@Retry`. Puede usarse junto con `@CircuitBreaker`.

```java
@Override
@CircuitBreaker(name = "favouriteService", fallbackMethod = "findAllFallback")
@Retry(name = "favouriteService") // Usa la configuración definida en application.yml
public List<FavouriteDto> findAll() {
    // Lógica que puede fallar (ej. llamadas HTTP)
    // ...
}
```

Si la operación falla, Resilience4j reintentará la ejecución según la configuración. Si todos los intentos fallan, se ejecutará el método `fallbackMethod` definido en el Circuit Breaker (o se lanzará la excepción si no hay fallback).
