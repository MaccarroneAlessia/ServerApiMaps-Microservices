# Script per l'installazione delle dipendenze del Progetto Sistemi Cloud
# Eseguire questo script in PowerShell come Amministratore (Run as Administrator)

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Installazione Dipendenze Sistemi Cloud      " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Questo script installerà:"
Write-Host "- Docker Desktop (Containerizzazione)"
Write-Host "- HashiCorp Terraform (Infrastructure as Code)"
Write-Host "- Amazon AWS CLI (Accesso Cloud)"
Write-Host "=============================================" -ForegroundColor Cyan

# Controllo privilegi di amministratore
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-Not $isAdmin) {
    Write-Host "ATTENZIONE: Stai eseguendo lo script senza privilegi di Amministratore." -ForegroundColor Yellow
    Write-Host "Alcune installazioni (come Docker Desktop) potrebbero fallire o richiedere conferme manuali continue." -ForegroundColor Yellow
    Write-Host "Se l'installazione si blocca, riavvia PowerShell come Amministratore e riprova." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}

Write-Host "`n[1/3] Installazione Docker Desktop..." -ForegroundColor Green
winget install -e --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements

Write-Host "`n[2/3] Installazione Terraform..." -ForegroundColor Green
winget install -e --id Hashicorp.Terraform --accept-package-agreements --accept-source-agreements

Write-Host "`n[3/3] Installazione AWS CLI..." -ForegroundColor Green
winget install -e --id Amazon.AWSCLI --accept-package-agreements --accept-source-agreements

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host " Installazione completata!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "IMPORTANTE: Devi CHIUDERE questa finestra e aprire un nuovo terminale" -ForegroundColor Yellow
Write-Host "per fare in modo che Windows riconosca i nuovi comandi (docker, terraform, aws)." -ForegroundColor Yellow
Write-Host "Ricordati inoltre di avviare l'applicazione Docker Desktop per far partire il demone!" -ForegroundColor Yellow
