# Feature Toggle (Interruptor de Funcionalidad)

## Propósito y Beneficios
Permite habilitar o deshabilitar funcionalidades del sistema dinámicamente sin necesidad de desplegar nuevo código.
*   **Despliegue Continuo**: Permite desplegar código incompleto o no probado en producción manteniéndolo oculto.
*   **Pruebas A/B**: Permite probar nuevas funcionalidades con un subconjunto de usuarios.
*   **Gestión de Riesgos**: Permite apagar rápidamente una funcionalidad problemática.

## Implementación en el Proyecto
Se implementa una solución personalizada ligera en `favourite-service` utilizando propiedades de configuración de Spring.

### Ejemplo de Código
En `FeatureToggleService.java`:

```java
@Service
@Slf4j
public class FeatureToggleService {

    @Value("${app.feature.fetch-details:true}")
    private boolean fetchDetailsEnabled;

    public boolean isFetchDetailsEnabled() {
        return fetchDetailsEnabled;
    }
}
```

Esta bandera se utiliza en la lógica de negocio para decidir si ejecutar o no cierta parte del código (por ejemplo, llamar a un servicio externo para obtener detalles adicionales).
