# DTO Pattern (Objeto de Transferencia de Datos)

## Propósito y Beneficios
El patrón **DTO** se utiliza para transferir datos entre subsistemas (por ejemplo, entre el controlador REST y el servicio, o entre microservicios) sin exponer las entidades de dominio internas.
*   **Seguridad**: Oculta campos sensibles de la base de datos.
*   **Desacoplamiento**: Permite cambiar la estructura de la base de datos sin romper la API pública.
*   **Optimización**: Permite enviar solo los datos necesarios, reduciendo el tamaño de la respuesta.

## Implementación en el Proyecto
Se utilizan clases POJO simples, potenciadas por **Lombok** para reducir el código repetitivo (boilerplate).

### Ejemplo de Código
```java
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductDto implements Serializable {
    
    private Integer productId;
    private String productTitle;
    private Double priceUnit;
    
    @JsonProperty("category")
    @JsonInclude(Include.NON_NULL)
    private CategoryDto categoryDto;
}
```

Las entidades JPA se mapean a estos DTOs antes de ser devueltas por la API.
