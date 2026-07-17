param (
    [string]$DbPassword = "PASS",
    [string]$GoogleApiKey = "API_KEY"
)

Write-Host "=========================================="
Write-Host "[INIZIO DEPLOY CLOUD AWS]"
Write-Host "=========================================="

Write-Host "`n[1] Avvio Terraform (Creazione infrastruttura, RDS, EC2, ALB)..."
# [1 - TERRAFORM APPLY] Lancia l'infrastruttura as Code. Terraform crea fisicamente l'hardware su AWS.
# I parametri 'DbPassword' e 'GoogleApiKey' passati in input sono inviati in modo sicuro come variabili.
Set-Location aws-terraform
terraform init
terraform apply -var="db_password=$DbPassword" -var="google_api_key=$GoogleApiKey" -auto-approve

if ($LASTEXITCODE -ne 0) {
    Write-Host "[X] Errore durante Terraform apply!" -ForegroundColor Red
    exit
}

Write-Host "`n[2] Recupero gli IP pubblici generati da Terraform..."
$MasterIP = (terraform output -raw k3s_master_ip)
$WorkerIP = (terraform output -raw k3s_worker_ip)
$AlbDns = (terraform output -raw alb_dns_name)
Set-Location ..

Write-Host "IP Master trovato: $MasterIP"
Write-Host "IP Worker trovato: $WorkerIP"
Write-Host "ALB URL: $AlbDns"

Write-Host "`n[3] Attendo 30 secondi per assicurarmi che le macchine siano pronte per SSH..."
Start-Sleep -Seconds 30

Write-Host "`n[4] Installazione K3s sul MASTER NODE..."
# [2 - CONFIGURATION MANAGEMENT] Sostituisce Ansible. Esegue il download e l'installazione
# di K3s (Kubernetes) sul nodo principale, configurando la macchina Ubuntu nuda via SSH.
$SshKey = "ansible\k3s-key.pem"

# Comando SSH Master
$InstallMasterCmd = "LOCAL_IP=`$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4); export INSTALL_K3S_EXEC=\`"--tls-san $MasterIP --node-external-ip $MasterIP --node-ip `$LOCAL_IP --advertise-address `$LOCAL_IP\`"; curl -sfL https://get.k3s.io | sh -"
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"$InstallMasterCmd`""

Write-Host "`n[5] Recupero il Token dal Master per far unire il Worker..."
# Estrae il file di token crittografico generato da K3s Master necessario per autorizzare i worker.
$K3sToken = cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"sudo cat /var/lib/rancher/k3s/server/node-token`""

Write-Host "Token recuperato con successo."

Write-Host "`n[6] Installazione K3s sul WORKER NODE..."
# Usa il token appena prelevato per dire al Worker di unirsi al cluster K3s principale.
$InstallWorkerCmd = "curl -sfL https://get.k3s.io | K3S_URL=https://${MasterIP}:6443 K3S_TOKEN=$K3sToken sh -"
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$WorkerIP `"$InstallWorkerCmd`""

Write-Host "`n[!] Attendo 20 secondi che le API di Kubernetes si avviino completamente..."
Start-Sleep -Seconds 20

Write-Host "`n[7] Deploy dei Manifest Kubernetes (Database e App)..."
# [3 - KUBERNETES SECRETS & DEPLOY] Autentica K8s verso il registro Docker privato.
# Preleva un token ECR temporaneo da AWS e lo inietta in K8s come 'Secret' (ecr-registry-secret).
# Questo è indispensabile altrimenti K8s non avrà i permessi per scaricare la tua immagine Docker.
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
$Content = $Content -replace 'imagePullPolicy: IfNotPresent', 'imagePullPolicy: Always'
$Content = $Content -replace '      containers:', "      imagePullSecrets:`n        - name: ecr-registry-secret`n      containers:"
$Content = $Content -replace 'image: maps-app:latest', "image: 493789587491.dkr.ecr.eu-west-1.amazonaws.com/maps-app-repo:latest"
Set-Content -Path $DeployPath -Value $Content

# Patch per mysql-secret.yaml (iniezione delle credenziali passate allo script)
$SecretPath = "temp_k8s\mysql-secret.yaml"
$SecretContent = Get-Content $SecretPath
$DbBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($DbPassword))
$ApiBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($GoogleApiKey))
$SecretContent = $SecretContent -replace 'MYSQL_PASSWORD: .*', "MYSQL_PASSWORD: $DbBase64"
$SecretContent = $SecretContent -replace 'MYSQL_ROOT_PASSWORD: .*', "MYSQL_ROOT_PASSWORD: $DbBase64"
$SecretContent = $SecretContent -replace 'GOOGLE_MAPS_API_KEY: .*', "GOOGLE_MAPS_API_KEY: $ApiBase64"
Set-Content -Path $SecretPath -Value $SecretContent

Write-Host "Copio i file YAML sul Master..."
cmd.exe /c "scp -o StrictHostKeyChecking=no -i $SshKey -r temp_k8s ubuntu@${MasterIP}:~/"

Write-Host "Applico Kubernetes..."
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"sudo k3s kubectl apply -f ~/temp_k8s/`""

Remove-Item "temp_k8s" -Recurse -Force

Write-Host "Forzo il riavvio dell'applicazione per ricaricare la nuova API Key..."
cmd.exe /c "ssh -o StrictHostKeyChecking=no -i $SshKey ubuntu@$MasterIP `"sudo k3s kubectl rollout restart deployment maps-app`""

Write-Host "`n*** DEPLOY CLOUD COMPLETATO! ***"
Write-Host "L'applicazione sarà raggiungibile tra pochi minuti all'indirizzo:"
Write-Host "-> http://${AlbDns}/graphql"
Write-Host "NOTE: Ricorda di fare `terraform destroy` alla fine!"
