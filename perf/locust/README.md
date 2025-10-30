## Locust performance tests

### Prerequisitos
- Python 3.9+
- Pip

Instalar dependencias:

```bash
pip install -r perf/locust/requirements.txt
```

### Variables de entorno
- LOCUST_HOST: URL base (p.ej. http://localhost:8080)
- LOCUST_BASE_PATH: prefijo de rutas (p.ej. "" o "/app")
- LOCUST_USERNAME y LOCUST_PASSWORD: credenciales (por defecto test/password)
- LOCUST_WAIT_MIN y LOCUST_WAIT_MAX: espera entre tareas

### Ejecutar Locust (modo web)

```bash
locust -f perf/locust/locustfile.py --host=$LOCUST_HOST
```

Abrir http://localhost:8089 y configurar usuarios/tasa.

### Ejecutar en modo headless (ejemplo)

```bash
locust -f perf/locust/locustfile.py \
  --host http://localhost:8080 \
  --headless -u 200 -r 20 -t 5m \
  --csv perf_results
```

### Flujos cubiertos
- Autenticación (POST /api/authenticate)
- Listado de productos (GET /api/products)
- Crear pedido (POST /api/orders)
- Crear pedido y pagar (POST /api/orders + POST /api/payments)
- Favoritos: crear y listar (POST /api/favourites, GET /api/favourites)
- Registro y login de usuario


