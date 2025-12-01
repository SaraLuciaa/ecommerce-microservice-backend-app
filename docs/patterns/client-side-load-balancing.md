# Client-Side Load Balancing (Balanceo de Carga del Lado del Cliente)

## Propósito y Beneficios
En lugar de depender de un balanceador de carga de hardware o servidor centralizado, el cliente (el microservicio que hace la llamada) decide a qué instancia del servicio destino llamar.
*   **Eficiencia**: Elimina un salto de red extra (el balanceador central).
*   **Inteligencia**: El cliente puede usar algoritmos de balanceo más sofisticados basados en métricas locales.
*   **Integración con Service Discovery**: Obtiene la lista de instancias disponibles directamente de Eureka.

## Implementación en el Proyecto
Se utiliza **Spring Cloud LoadBalancer**.

### Dependencias
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### Funcionamiento
Cuando se usa junto con **Feign** o `RestTemplate` anotado con `@LoadBalanced`, Spring Cloud LoadBalancer intercepta la petición. Consulta a Eureka por las instancias disponibles del servicio destino (ej. `product-service`) y selecciona una usando un algoritmo (por defecto Round Robin).

```java
// En Feign, esto es transparente:
@FeignClient(name = "product-service") // El nombre se resuelve a una lista de IPs
public interface ProductClient { ... }
```
