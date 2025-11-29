$baseUrl = "http://localhost:8800/favourite-service"
$productContainer = "ecommerce-microservice-backend-app-1-product-service-container-1"

function Pause-Script {
    Read-Host "Presiona ENTER para continuar..."
}

Write-Host "--- 1. ESTADO INICIAL ---" -ForegroundColor Cyan
Write-Host "Asegurando que el servicio dependiente este arriba..."
docker start $productContainer
Start-Sleep -Seconds 5
Pause-Script

Write-Host "Verificando Salud..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    $status = $response.components.circuitBreakers.details.favouriteService.status
    Write-Host "Estado: $status" -ForegroundColor Green
} catch {
    Write-Host "Error checking health: $_" -ForegroundColor Red
}

Write-Host "Llamando al Servicio..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/favourites" -Method Get
    Write-Host "Success (200 OK)" -ForegroundColor Green
    Write-Host "Respuesta:" -ForegroundColor Gray
    $json = $response | ConvertTo-Json -Depth 2
    Write-Host $json -ForegroundColor Gray
} catch {
    Write-Host "Error calling service: $_" -ForegroundColor Red
}
Pause-Script

Write-Host "--- 2. SIMULANDO FALLO ---" -ForegroundColor Cyan
docker stop $productContainer
Write-Host "Product Service Detenido" -ForegroundColor Red
Start-Sleep -Seconds 2
Pause-Script

Write-Host "--- 3. PROBANDO FALLBACK ---" -ForegroundColor Cyan
Write-Host "Llamando al Servicio (Esperando Fallback)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/favourites" -Method Get
    Write-Host "Success (200 OK) - Fallback Triggered" -ForegroundColor Green
    Write-Host "Respuesta:" -ForegroundColor Gray
    $json = $response | ConvertTo-Json -Depth 2
    Write-Host $json -ForegroundColor Gray
} catch {
    Write-Host "Error calling service: $_" -ForegroundColor Red
}

Write-Host "Verificando Salud..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    $status = $response.components.circuitBreakers.details.favouriteService.status
    Write-Host "Estado: $status" -ForegroundColor Green
} catch {
    Write-Host "Error checking health: $_" -ForegroundColor Red
}
Pause-Script

Write-Host "--- 4. RECUPERACION ---" -ForegroundColor Cyan
docker start $productContainer
Write-Host "Iniciando Product Service..." -ForegroundColor Green
Start-Sleep -Seconds 15
Pause-Script

Write-Host "Llamando al Servicio (Esperando Exito)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/favourites" -Method Get
    Write-Host "Success (200 OK)" -ForegroundColor Green
    Write-Host "Respuesta:" -ForegroundColor Gray
    $json = $response | ConvertTo-Json -Depth 2
    Write-Host $json -ForegroundColor Gray
} catch {
    Write-Host "Error calling service: $_" -ForegroundColor Red
}

Write-Host "Verificando Salud..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    $status = $response.components.circuitBreakers.details.favouriteService.status
    Write-Host "Estado: $status" -ForegroundColor Green
} catch {
    Write-Host "Error checking health: $_" -ForegroundColor Red
}

Write-Host "PRUEBA COMPLETADA" -ForegroundColor Cyan
