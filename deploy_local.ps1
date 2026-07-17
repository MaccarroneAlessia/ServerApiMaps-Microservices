Write-Host "=========================================="
Write-Host "LOCAL DEPLOYMENT START (PHASE 1)"
Write-Host "=========================================="

Write-Host "`n[1] Compiling Docker image..."
# cd server-springboot-maps
Set-Location server-springboot-maps
docker build -t maps-app:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "[Error] during Docker build. Ensure Docker Desktop is running!" -ForegroundColor Red
    exit
}
Set-Location ..

Write-Host "`n[2] Applying manifests to local Kubernetes..."
kubectl apply -f infrastructure/k8s/
if ($LASTEXITCODE -ne 0) {
    Write-Host "[Error] during kubectl apply. Ensure Kubernetes is enabled in Docker Desktop!" -ForegroundColor Red
    exit
}

Write-Host "`n[3] Waiting for Pods to start (Waiting for app to be ready)..."
# Waits until the maps-app deployment has the "Available" condition
kubectl wait --for=condition=available --timeout=300s deployment/maps-app

Write-Host "`n[4] Verifying service exposure on http://localhost:30080..."
Start-Sleep -Seconds 5 # Wait a few seconds for the service to be fully up
$url = "http://localhost:30080/actuator/health"
try {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "The service responds correctly via NodePort on localhost!" -ForegroundColor Green
    Write-Host "`n*** LOCAL DEPLOYMENT COMPLETED SUCCESSFULLY! ***"
    Write-Host "You can test the application by opening this link in your browser:"
    Write-Host "-> http://localhost:30080"
} catch {
    Write-Host "NodePort not mapped on localhost. Forcing port-forwarding..." -ForegroundColor Yellow
    # Starts background port-forward (hidden) using Start-Process
    Start-Process -FilePath "kubectl" -ArgumentList "port-forward svc/maps-app-service 30080:8080" -WindowStyle Hidden
    Start-Sleep -Seconds 3
    Write-Host "`n*** LOCAL DEPLOYMENT COMPLETED SUCCESSFULLY (via Port-Forward)! ***"
    Write-Host "You can test the application by opening this link in your browser:"
    Write-Host "-> http://localhost:30080"
}
Write-Host "=========================================="
