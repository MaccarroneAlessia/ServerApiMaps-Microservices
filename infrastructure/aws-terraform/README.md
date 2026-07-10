# AWS Terraform (Infrastructure as Code)

**Terraform** è uno strumento di Infrastructure as Code (IaC) impiegato per dichiarare e orchestrare l'architettura hardware (Reti, EC2, Database, Load Balancers) attraverso file di configurazione in sintassi HCL. Questa cartella ospita il codice dedicato in modo specifico al provider di **Amazon Web Services (AWS)**.

L'infrastruttura reale, la topologia di rete e i server fisici su cui ruota l'applicazione risiedono qui in forma di codice. Sostituendo la tradizionale "ClickOps" dalla AWS Web Console con Terraform, l'intero data center aziendale diviene versionabile, riproducibile al 100% e documentato per design.

## 📂 Struttura

- **`main.tf`**: File principale per la dichiarazione della configurazione base e dei provider.
- **`network.tf`**: Dichiara la rete VPC privata, definendo Subnet pubbliche e private, Internet Gateway e Route Tables.
- **`ec2.tf`**: Esegue il provisioning dei nodi computazionali EC2 che ospiteranno Kubernetes (Master/Worker).
- **`rds.tf`**: Dichiara il database gestito Amazon RDS per MySQL.
- **`alb.tf`**: Definisce l'Application Load Balancer necessario per bilanciare il traffico tra i nodi Worker attivi.
- **`security.tf`**: Configura i Security Group per garantire comunicazioni sicure tramite regole firewall ben precise tra i componenti.
- **`ssm.tf`**: Permette l'accesso controllato ai Parameter Store AWS per recuperare credenziali in modo sicuro senza hardcodarle.
- **`variables.tf`** e **`outputs.tf`**: Definiscono rispettivamente i parametri di input iniettabili e gli output restituiti al termine dell'installazione.

## 💡 Architettura e Design

1. **IaaS Puro (Cluster Kubernetes su EC2)**: Si è optato per il mantenimento di un cluster puro lanciando macchine EC2 "nude" su cui installare autonomamente K3s, invece di abbracciare servizi managed costosi come AWS EKS. Questa scelta restituisce il massimo controllo sui server sottostanti ottimizzando pesantemente i costi per test e ambienti di piccola/media taglia.
2. **Isolamento del Database**: Il database MySQL in RDS risiede confinato in una Subnet Privata. È totalmente irraggiungibile da Internet, risultando accessibile esclusivamente dai nodi Kubernetes Worker tramite precise regole di Security Group.
3. **SSM Parameter Store per i Secret**: Le stringhe sensibili (password del DB, chiavi API) vengono lette a runtime dinamicamente tramite AWS SSM, impedendo ogni rischio di leak su Git.

## 🚀 Comandi per il Deploy

L'orchestrazione è primariamente demandata allo script PowerShell radice. 
Per la gestione manuale del provisioning su AWS:

```bash
cd infrastructure/aws-terraform

# Scarica i plugin provider necessari (AWS)
terraform init

# Genera un'anteprima delle risorse che verrebbero create
terraform plan -var="db_password=LA_TUA_PASSWORD" -var="google_api_key=LA_TUA_APIKEY"

# Lancia il provisioning reale in Cloud
terraform apply -var="db_password=LA_TUA_PASSWORD" -var="google_api_key=LA_TUA_APIKEY"
```

Per abbattere l'infrastruttura ed evitare costi al termine dell'utilizzo:
```bash
terraform destroy -var="db_password=LA_TUA_PASSWORD" -var="google_api_key=LA_TUA_APIKEY"
```
