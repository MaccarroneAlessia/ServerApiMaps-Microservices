Write-Host "=========================================="
Write-Host "STOP DEPLOY LOCALE (FASE 1)"
Write-Host "=========================================="

Write-Host "`n[1] Smantellamento delle risorse su Kubernetes locale..."
kubectl delete -f infrastructure/k8s/

if ($LASTEXITCODE -ne 0) {
    Write-Host "[Errore] Impossibile eliminare le risorse. Kubernetes e' acceso?" -ForegroundColor Red
    exit
}

Write-Host "`n*** SITO LOCALE FERMATO CON SUCCESSO! ***"
Write-Host "Tutti i pod, i servizi e il database sono stati rimossi."
Write-Host "=========================================="
