# Repository Pattern (Patrón Repositorio)

## Propósito y Beneficios
Abstrae la capa de acceso a datos, proporcionando una interfaz tipo colección para acceder a los objetos del dominio.
*   **Desacoplamiento**: La lógica de negocio no depende de la tecnología de persistencia subyacente (SQL, NoSQL).
*   **Testabilidad**: Facilita la creación de mocks para pruebas unitarias.
*   **Productividad**: Spring Data JPA genera automáticamente la implementación de consultas comunes.

## Implementación en el Proyecto
Se utiliza **Spring Data JPA**.

### Dependencias
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### Ejemplo de Código
Se define una interfaz que extiende `JpaRepository`:

```java
public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Métodos CRUD básicos ya están disponibles
    
    // Se pueden definir consultas personalizadas por nombre de método
    List<Product> findByCategory(String category);
}
```
