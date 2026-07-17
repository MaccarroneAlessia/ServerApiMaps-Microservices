Write-Host "=========================================="
Write-Host "INIZIO DEPLOY LOCALE (FASE 1)"
Write-Host "=========================================="

Write-Host "`n[1] Compilazione dell'immagine Docker in corso..."
# cd server-springboot-maps
Set-Location server-springboot-maps
docker build -t maps-app:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "[Errore] durante la build di Docker. Assicurati che Docker Desktop sia in esecuzione!" -ForegroundColor Red
    exit
}
Set-Location ..

Write-Host "`n[2] Applicazione dei manifesti su Kubernetes locale..."
kubectl apply -f infrastructure/k8s/
if ($LASTEXITCODE -ne 0) {
    Write-Host "[Errore] durante kubectl apply. Assicurati che Kubernetes sia abilitato su Docker Desktop!" -ForegroundColor Red
    exit
}

Write-Host "`n[3] Attesa avvio dei Pod (Aspettiamo che l'app sia pronta)..."
# Aspetta finché il deployment maps-app non ha la condition "Available"
kubectl wait --for=condition=available --timeout=300s deployment/maps-app

Write-Host "`n[4] Verifica esposizione servizio su http://localhost:30080..."
Start-Sleep -Seconds 5 # Attendi qualche secondo che il servizio sia pienamente up
$url = "http://localhost:30080/actuator/health"
try {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "Il servizio risponde correttamente tramite NodePort su localhost!" -ForegroundColor Green
    Write-Host "`n*** DEPLOY LOCALE COMPLETATO CON SUCCESSO! ***"
    Write-Host "Puoi testare l'applicazione aprendo questo link nel tuo browser:"
    Write-Host "-> http://localhost:30080"
} catch {
    Write-Host "NodePort non mappato su localhost. Avvio port-forwarding forzato..." -ForegroundColor Yellow
    # Avvia port-forward in background (nascosto) usando Start-Process
    Start-Process -FilePath "kubectl" -ArgumentList "port-forward svc/maps-app-service 30080:8080" -WindowStyle Hidden
    Start-Sleep -Seconds 3
    Write-Host "`n*** DEPLOY LOCALE COMPLETATO CON SUCCESSO (tramite Port-Forward)! ***"
    Write-Host "Puoi testare l'applicazione aprendo questo link nel tuo browser:"
    Write-Host "-> http://localhost:30080"
}
Write-Host "=========================================="
