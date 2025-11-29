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
    $cbDetails = $response.components.circuitBreakers.details.favouriteService.details
    $state = $cbDetails.state
    $failureRate = $cbDetails.failureRate
    Write-Host "Estado CB: $state (Failure Rate: $failureRate)" -ForegroundColor Green
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
    $cbDetails = $response.components.circuitBreakers.details.favouriteService.details
    $state = $cbDetails.state
    $failureRate = $cbDetails.failureRate
    Write-Host "Estado CB: $state (Failure Rate: $failureRate)" -ForegroundColor Green
} catch {
    Write-Host "Error checking health: $_" -ForegroundColor Red
}
Pause-Script

Write-Host "--- 4. RECUPERACION ---" -ForegroundColor Cyan
docker start $productContainer
Write-Host "Iniciando Product Service..." -ForegroundColor Green
Start-Sleep -Seconds 60
Pause-Script

Write-Host "Llamando al Servicio para cerrar el Circuit Breaker..." -ForegroundColor Yellow

$maxRetries = 10
$retryCount = 0
$state = ""

do {
    $retryCount++
    Write-Host "Intento $retryCount..." -NoNewline
    try {
         $null = Invoke-RestMethod -Uri "$baseUrl/api/favourites" -Method Get
         Write-Host " OK" -ForegroundColor Green
    } catch {
         Write-Host " Fallo" -ForegroundColor Red
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
        $cbDetails = $response.components.circuitBreakers.details.favouriteService.details
        $state = $cbDetails.state
        $failureRate = $cbDetails.failureRate
        Write-Host "  Estado CB: $state (Failure Rate: $failureRate)" -ForegroundColor Yellow
    } catch {
        Write-Host "  Error verificando salud" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 1
} until ($state -eq "CLOSED" -or $retryCount -ge $maxRetries)

if ($state -eq "CLOSED") {
    Write-Host "EXITO: Circuit Breaker recuperado y CERRADO." -ForegroundColor Green
} else {
    Write-Host "FALLO: El Circuit Breaker no se cerró después de $maxRetries intentos." -ForegroundColor Red
}

Write-Host "PRUEBA COMPLETADA" -ForegroundColor Cyan
