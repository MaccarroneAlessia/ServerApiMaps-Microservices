#!/bin/bash
set -e

# Passaggio dei parametri opzionali (es. ./deploy_k3s_linux.sh "mypass" "myapikey")
DB_PASSWORD=${1:-"PASS"}
GOOGLE_API_KEY=${2:-"API_KEY"}

echo "=========================================="
echo "🚀 INIZIO DEPLOY CLOUD AWS (LINUX/MAC) 🚀"
echo "=========================================="

echo -e "\n1️⃣ Avvio Terraform (Creazione infrastruttura, RDS, EC2, ALB)..."
# [1 - TERRAFORM APPLY] Lancia l'infrastruttura as Code su AWS.
cd aws-terraform
terraform init
terraform apply -var="db_password=$DB_PASSWORD" -var="google_api_key=$GOOGLE_API_KEY" -auto-approve

echo -e "\n2️⃣ Recupero gli IP pubblici generati da Terraform..."
MASTER_IP=$(terraform output -raw k3s_master_ip)
WORKER_IP=$(terraform output -raw k3s_worker_ip)
ALB_DNS=$(terraform output -raw alb_dns_name)
cd ..

echo "IP Master trovato: $MASTER_IP"
echo "IP Worker trovato: $WORKER_IP"
echo "ALB URL: $ALB_DNS"

echo -e "\n3️⃣ Attendo 30 secondi per l'avvio delle macchine EC2..."
sleep 30

echo -e "\n4️⃣ Installazione K3s tramite Ansible..."
# [2 - CONFIGURATION MANAGEMENT] A differenza di Windows, su Mac/Linux usiamo nativamente Ansible
# Ansible leggera l'inventario generato da Terraform e installerà il master e unirà il worker automaticamente.

export ANSIBLE_HOST_KEY_CHECKING=False # Evita prompt di conferma per host sconosciuti
chmod 600 ansible/k3s-key.pem # Mac/Linux richiedono permessi stretti per la chiave privata

ansible-playbook -i ansible/inventory.ini ansible/playbook-k3s.yml --private-key ansible/k3s-key.pem -u ubuntu

echo -e "\n5️⃣ Deploy dei Manifest Kubernetes (Database e App)..."
# [3 - KUBERNETES SECRETS & DEPLOY] Genera un token ECR temporaneo da AWS
# e lo inietta in K8s come 'Secret' (ecr-registry-secret) per il pull dell'immagine.
echo "Richiedo la password Docker da AWS ECR..."
ECR_PASSWORD=$(aws ecr get-login-password --region eu-west-1)

echo "Applico i secret su K3s..."
ssh -o StrictHostKeyChecking=no -i ansible/k3s-key.pem ubuntu@$MASTER_IP "sudo k3s kubectl create secret docker-registry ecr-registry-secret --docker-server=493789587491.dkr.ecr.eu-west-1.amazonaws.com --docker-username=AWS --docker-password=$ECR_PASSWORD --dry-run=client -o yaml | sudo k3s kubectl apply -f -"

# Crea una copia locale per iniettare il tag ECR e i secrets
cp -r k8s temp_k8s

# Compatibilità sed per Mac (BSD) e Linux (GNU)
if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' 's/imagePullPolicy: IfNotPresent/imagePullPolicy: Always\n        imagePullSecrets:\n        - name: ecr-registry-secret/g' temp_k8s/app-deployment.yaml
  sed -i '' 's|image: maps-app:latest|image: 493789587491.dkr.ecr.eu-west-1.amazonaws.com/maps-app-repo:latest|g' temp_k8s/app-deployment.yaml
else
  sed -i 's/imagePullPolicy: IfNotPresent/imagePullPolicy: Always\n        imagePullSecrets:\n        - name: ecr-registry-secret/g' temp_k8s/app-deployment.yaml
  sed -i 's|image: maps-app:latest|image: 493789587491.dkr.ecr.eu-west-1.amazonaws.com/maps-app-repo:latest|g' temp_k8s/app-deployment.yaml
fi

echo "Copio i file YAML sul Master..."
scp -o StrictHostKeyChecking=no -i ansible/k3s-key.pem -r temp_k8s ubuntu@$MASTER_IP:~/

echo "Applico Kubernetes..."
ssh -o StrictHostKeyChecking=no -i ansible/k3s-key.pem ubuntu@$MASTER_IP "sudo k3s kubectl apply -f ~/temp_k8s/"

# Pulizia file temporanei
rm -rf temp_k8s

echo -e "\n🎉 DEPLOY CLOUD COMPLETATO! 🎉"
echo "L'applicazione sarà raggiungibile tra pochi minuti all'indirizzo:"
echo "👉 http://$ALB_DNS/graphql"
echo "note: Ricorda di fare 'cd aws-terraform && terraform destroy' alla fine!"

# cd infrastructure
# chmod +x deploy_k3s_linux.sh
# ./deploy_k3s_linux.sh "TuaPasswordDB" "TuaApiKeyGoogle"


