Write-Host "=========================================="
Write-Host "STOP LOCAL DEPLOYMENT (PHASE 1)"
Write-Host "=========================================="

Write-Host "`n[1] Tearing down resources on local Kubernetes..."
kubectl delete -f infrastructure/k8s/

if ($LASTEXITCODE -ne 0) {
    Write-Host "[Error] Unable to delete resources. Is Kubernetes running?" -ForegroundColor Red
    exit
}

Write-Host "`n*** LOCAL SITE STOPPED SUCCESSFULLY! ***"
Write-Host "All pods, services, and the database have been removed."
Write-Host "=========================================="
