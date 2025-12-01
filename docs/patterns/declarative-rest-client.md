# Declarative REST Client (Cliente REST Declarativo)

## Propósito y Beneficios
Simplifica la invocación de servicios REST externos o internos mediante la definición de interfaces, en lugar de escribir código repetitivo con `RestTemplate` o `WebClient`.
*   **Legibilidad**: El código es más limpio y fácil de entender.
*   **Mantenibilidad**: Se define la estructura de la API en una interfaz Java.
*   **Integración**: Se integra fácilmente con Load Balancers y Circuit Breakers.

## Implementación en el Proyecto
Se utiliza **Spring Cloud OpenFeign**.

### Dependencias
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### Ejemplo de Código
Se define una interfaz anotada con `@FeignClient`:

```java
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{productId}")
    ProductDto getProductById(@PathVariable("productId") Integer productId);
}
```

Luego se inyecta y usa como cualquier otro bean de Spring:

```java
@Autowired
private ProductClient productClient;

public void someMethod() {
    ProductDto product = productClient.getProductById(1);
}
```
