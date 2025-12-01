# Change Management y Release Notes

El manual Change Management y Release Notes tiene como objetivo establecer un proceso formal, controlada y auditable para:

- Gestionar cambios en el software.
- Crear etiquetas de releases de forma automáticamente.
- Establecer estrategias de rollback en caso de fallas críticas.

Por lo anterior, este manual permite: 

- Evitar cambios desordenados que puedan romper funcionalidades críticas.
- Asegurar trazabilidad, permitiendo entender qué se cambió, cuándo y por quién.
- Garantizar recuperabilidad, porque incluso un cambio bien hecho puede fallar y debe poder revertirse.

Todo esto, permite proteger la estabilidad del sistema cuando este crece y múltiples personas interactúan con el mismo código.

## Flujo de Change Management

El flujo se divide en etapas porque cada una atiende un riesgo distinto del ciclo de vida del software. Saltarse una etapa incrementa la probabilidad de introducir errores en producción o afectar módulos relacionados.

### Creación de cambio (Request for Change - RFC)

Un cambio inicia con isssue o un ticket. Este debe incluir: descripción, justificación, riesgos asociados, validación requerida y criterios de aceptación. De esta forma, se evitan cambios no documentados, se permite evaluar el impacto y asegurar la trazabilidad.

### Evaluación del cambio

El equipo revisa la descripción, el impacto y los riesgos. Esta etapa existe porque no todos los cambios deben aprobarse; algunos pueden afectar la arquitectura, la seguridad o el rendimiento. Si se aprueba, se clasifica como feature, bugfix, enhancement o hotfix, lo que ayuda a determinar su prioridad y tratamiento.

### Creación de rama específica

El cambio a realizar debe aislarse del resto del código para evitar conflictos con otros trabajos, revisar solo lo que pertenece a esa tarea y revertirlo fácilmente si algo sale mal. Por ello, cuando la RFC está aprobada, se crea una rama con nombre semántico:

- `feature/***`
- `bugfix/***`
- `hotfix/***`

### **Commit semántico**

Los commits deben seguir la convención **Conventional Commits**, que define una estructura clara para comunicar qué tipo de cambio se está realizando. La sintaxis usa prefijos como:

- `feat:` para nuevas funcionalidades
- `fix:` para corrección de defectos
- `refactor:` para cambios internos sin alterar comportamiento
- `docs:` para actualizaciones de documentación

Esta convención no es estética; cumple una función crítica en el proceso de Change Management.

Al mantener un formato uniforme, el historial del repositorio se vuelve más legible y permite automatizar tareas como:

- determinar si un release debe ser **MAJOR**, **MINOR** o **PATCH**
- generar **Release Notes** sin intervención humana
- identificar el propósito de cada commit sin revisar código
- integrar bots o workflows de GitHub que dependan del tipo de cambio

Sin commits semánticos el versionado inteligente y el control automatizado dejan de funcionar, lo que afecta directamente la integridad del proceso de releases.

### **Revisión técnica (Code Review)**

Una vez creado el Pull Request hacia `develop`, comienza la revisión técnica.

Aquí se evalúa si el cambio cumple los criterios de aceptación, respeta los estándares del proyecto, introduce riesgos o efectos secundarios, las pruebas unitarias y de integración están completas y el código es mantenible y consistente con la arquitectura.

Esta etapa evita que cambios defectuosos o incompletos entren al flujo principal.

### **Merge en `develop`**

Cuando la revisión es satisfactoria, la rama del cambio se fusiona en `develop`. Este entorno sirve para **integrar el trabajo diario del equipo**, sin requerir un nivel de estabilidad total. 

El objetivo de `develop` es permitir que varias tareas convivan mientras se construye un incremento funcional.

Cada cambio aprobado en RFC debe integrarse a `develop` mediante un PR individual. No se agrupan varios cambios en un solo PR hacia develop porque eso rompe la trazabilidad.

### **Promoción de `develop` a `stage`**

Llegar a `stage` no es automático. El equipo decide **cuándo** promover `develop` a `stage`. Normalmente ocurre cuando se completa un conjunto de funcionalidades del sprint, se obtiene un incremento significativo, se requiere validar una versión candidata a release, el Product Owner debe revisar funcionalidad.

**Revisión y aprobación en `stage`:**

- se ejecutan pruebas funcionales completas
- se validan integraciones entre módulos
- se prueban con datos más cercanos a producción
- el Product Owner realiza validación de negocio
- los testers realizan pruebas de regresión
- se detectan problemas antes de llegar a producción

Esta etapa reduce el riesgo de fallas en main porque controla calidad en un entorno casi idéntico al productivo.

### **Promoción de `stage` a `main`**

El paso final solo se realiza una vez que `stage` es estable y cumple:

- funcionalidad validada
- pruebas automatizadas exitosas
- revisiones técnicas completadas
- criterios de negocio aprobados
- no existen defectos críticos

Mover `stage` a `main` representa que el incremento ya es un **Release Candidate aprobado.**

**Revisión y aprobación en `main`:**

- se verifica que la versión es estable
- se evalúa el impacto en producción
- se asegura que el despliegue no afecta datos o usuarios
- se valida el plan de rollback
- se aprueban los cambios desde el punto de vista operativo

### **Merge a `main` y generación del Release**

Al fusionar `stage` en `main`:

- GitHub Actions crea **automáticamente un tag semántico**
- se generan **Release Notes**
- se empaqueta la versión final
- se activa el pipeline para producción
- se registra una nueva entrega formal del sistema

Esta automatización elimina errores humanos y asegura que solo versiones validadas llegan a producción.

# **Rollback formal**

El rollback es el mecanismo mediante el cual se revierte un despliegue fallido y se restaura una versión anterior del sistema.

Este proceso existe porque ningún flujo de Change Management es infalible: incluso después de pasar por `develop`, `stage` y `main`, pueden aparecer fallas en producción que afecten la operación o la experiencia del usuario.

El rollback debe ser un procedimiento **planificado, documentado y reproducible**, no una reacción improvisada.

## **Cuándo ejecutar un rollback**

Un rollback se realiza cuando el sistema desplegado en producción presenta:

- fallas críticas que afectan la disponibilidad del servicio
- errores funcionales que impiden el flujo normal de los usuarios
- fallos de seguridad o brechas detectadas después del despliegue
- incompatibilidades no detectadas en `stage`
- regresiones importantes que deterioran la experiencia
- degradación en indicadores clave (tiempo de respuesta, consumo, errores)

La decisión de revertir no se toma por preferencia sino por impacto.

Si la falla afecta continuidad de negocio, usuarios o integraciones, el rollback es obligatorio.

## **Objetivo del rollback**

Restaurar la última versión estable del sistema con la mínima interrupción posible y sin pérdida de datos.

Un rollback correcto devuelve al sistema a un estado seguro, mientras el equipo analiza la causa raíz del fallo y prepara un nuevo fix mediante el flujo normal de cambios.

## **Preparación previa al rollback**

Todo cambio que llega a producción debe aprobarse solo si cuenta con:

- el tag anterior disponible y verificado
- los artefactos de la versión estable (build, contenedor, bundle)
- scripts de reversión aplicables a BD o configuraciones
- verificaciones de compatibilidad (por ejemplo, cambios en esquemas)
- un responsable asignado para ejecutar la reversión
- monitoreo activo para detectar si el rollback es exitoso

La preparación es tan importante como la reversión en sí: un rollback improvisado puede causar incluso más daño que el fallo original.

## **Proceso formal de rollback**

Aunque el entorno de despliegue puede variar, el proceso es estructuralmente el mismo.

Los pasos están en bullet points porque representan acciones secuenciales.

### **1. Identificación del fallo**

- El equipo monitorea logs, dashboards, métricas y errores.
- Se confirma que el problema es consecuencia directa del cambio recién desplegado.
- Se determina que el impacto es significativo y que se requiere revertir.

### **2. Activación del procedimiento de emergencia**

- El Release Manager o responsable de guardia autoriza el rollback.
- Se notifica a los equipos involucrados (desarrollo, QA, operación, negocio).
- Se registra el incidente en un ticket o Issue etiquetado como **critical** o **production-failure**.

### **3. Ejecución del rollback**

Dependiendo del sistema, pueden existir varios mecanismos:

**Rollback por versión (tag):**

- Se identifica el tag inmediatamente anterior, por ejemplo `v1.3.4`.
- El sistema despliega nuevamente ese artefacto en producción utilizando CI/CD.

**Rollback por contenedor:**

- Se restaura la imagen anterior registrada en el repositorio (por ejemplo, Docker registry).

**Rollback por infraestructura:**

- Se regresa a la configuración previa si el cambio modificó ambientes, rutas o infraestructura.

### **4. Validación del rollback**

Una vez que el entorno vuelve a la versión estable:

- se monitorean indicadores
- se verifica que el problema desapareció
- se revisa que no surgieron nuevos fallos
- se comprueba el funcionamiento general del sistema

### **5. Registro y documentación**

Después del rollback:

- se documenta la causa del fallo
- se registra qué se revirtió y cuándo
- se actualiza la RFC del cambio problemático
- se crea un ticket para análisis de causa raíz (Root Cause Analysis – RCA)
- se planifica una corrección mediante un nuevo flujo de cambio

El rollback nunca se considera un cierre definitivo; es un paso temporal para proteger el sistema mientras se prepara la solución real.