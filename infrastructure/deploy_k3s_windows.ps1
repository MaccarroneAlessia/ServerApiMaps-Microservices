param (
    [string]$DbPassword = "PASS",
    [string]$GoogleApiKey = "API_KEY"
)

Write-Host "=========================================="
Write-Host "🚀 INIZIO DEPLOY CLOUD AWS 🚀"
Write-Host "=========================================="

Write-Host "`n1️⃣ Avvio Terraform (Creazione infrastruttura, RDS, EC2, ALB)..."
cd aws-terraform
terraform init
terraform apply -var="db_password=$DbPassword" -var="google_api_key=$GoogleApiKey" -auto-approve

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Errore durante Terraform apply!" -ForegroundColor Red
    exit
}

Write-Host "`n2️⃣ Recupero gli IP pubblici generati da Terraform..."
$MasterIP = (terraform output -raw k3s_master_ip)
$WorkerIP = (terraform output -raw k3s_worker_ip)
$AlbDns = (terraform output -raw alb_dns_name)
cd ..

Write-Host "IP Master trovato: $MasterIP"
Write-Host "IP Worker trovato: $WorkerIP"
Write-Host "ALB URL: $AlbDns"

Write-Host "`n3️⃣ Attendo 30 secondi per assicurarmi che le macchine siano pronte per SSH..."
Start-Sleep -Seconds 30

Write-Host "`n4️⃣ Installazione K3s sul MASTER NODE..."
$SshKey = "ansible\k3s-key.pem"

# Comando SSH Master
$InstallMasterCmd = "curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC='--tls-san $MasterIP --node-external-ip $MasterIP' sh -"
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"$InstallMasterCmd`""

Write-Host "`n5️⃣ Recupero il Token dal Master per far unire il Worker..."
$K3sToken = cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"sudo cat /var/lib/rancher/k3s/server/node-token`""

Write-Host "Token recuperato con successo."

Write-Host "`n6️⃣ Installazione K3s sul WORKER NODE..."
$InstallWorkerCmd = "curl -sfL https://get.k3s.io | K3S_URL=https://${MasterIP}:6443 K3S_TOKEN=$K3sToken sh -"
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$WorkerIP `"$InstallWorkerCmd`""

Write-Host "`n7️⃣ Deploy dei Manifest Kubernetes (Database e App)..."
# Genero il token ECR
Write-Host "Richiedo la password Docker da AWS ECR..."
$EcrPassword = (aws ecr get-login-password --region eu-west-1)

# Creo il file temporaneo yaml
Write-Host "Applico i deployment..."
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"sudo k3s kubectl create secret docker-registry ecr-registry-secret --docker-server=493789587491.dkr.ecr.eu-west-1.amazonaws.com --docker-username=AWS --docker-password=$EcrPassword --dry-run=client -o yaml | sudo k3s kubectl apply -f -`""

# Applico i file della cartella k8s inviandoli via cat (sostituiti con copia locale)
Copy-Item "k8s" -Destination "temp_k8s" -Recurse -Force
# Patch per imagePullSecrets sul deployment
$DeployPath = "temp_k8s\app-deployment.yaml"
$Content = Get-Content $DeployPath
$Content = $Content -replace 'imagePullPolicy: IfNotPresent', "imagePullPolicy: Always`n        imagePullSecrets:`n        - name: ecr-registry-secret"
$Content = $Content -replace 'image: maps-app:latest', "image: 493789587491.dkr.ecr.eu-west-1.amazonaws.com/maps-app-repo:latest"
Set-Content -Path $DeployPath -Value $Content

Write-Host "Copio i file YAML sul Master..."
cmd.exe /c "scp -o StrictHostKeyChecking=no -i $SshKey -r temp_k8s ubuntu@$MasterIP:~/"

Write-Host "Applico Kubernetes..."
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"sudo k3s kubectl apply -f ~/temp_k8s/`""

Remove-Item "temp_k8s" -Recurse -Force

Write-Host "`n🎉 DEPLOY CLOUD COMPLETATO! 🎉"
Write-Host "L'applicazione sarà raggiungibile tra pochi minuti all'indirizzo:"
Write-Host "👉 http://${AlbDns}/graphql"
Write-Host "ATTENZIONE: Ricordati di fare `terraform destroy` a fine esame!"
