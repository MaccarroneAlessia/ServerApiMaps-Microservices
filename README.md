# ☁️🗺️ Progetto Sistemi Cloud: Microservizi, Kubernetes & AWS Infrastructure as Code

Progetto universitario per l'esame di **Sistemi Cloud** e **Ingegneria dei Sistemi Distribuiti**.
Questo repository contiene lo sviluppo, la containerizzazione e l'automazione infrastrutturale (IaC) di un'applicazione a microservizi basata su **Spring Boot** (Architettura 3-tier) per la gestione e analisi del traffico, con geocoding integrato tramite le API di Google Maps.

Il focus principale del progetto è sulle metodologie DevOps moderne, l'infrastruttura ibrida e la fault-tolerance.

Il progetto è strutturato in due fasi implementative:
- **Fase 1**: Containerizzazione (Docker) e orchestrazione locale tramite Kubernetes (K3s/Docker Desktop).
- **Fase 2**: Infrastruttura Cloud Ibrida su **Amazon Web Services (AWS)** completamente automatizzata (Terraform + PowerShell) usando macchine IaaS (EC2) per il cluster Kubernetes.

---

## 🏗️ Architettura dell'Applicazione (Spring Boot)
L'applicazione è sviluppata seguendo pattern avanzati per i sistemi distribuiti:
1. **Tier 1 (Presentation)**: Controller REST per l'esposizione degli endpoint.
2. **Tier 2 (Business Logic)**: Servizi core con integrazione API esterne. La comunicazione con Google Maps è protetta tramite **Resilience4j** (implementando il pattern **Circuit Breaker** e Retry) per garantire fault-tolerance e prevenire l'esaurimento dei thread in caso di network failures o rate limiting dell'API.
3. **Tier 3 (Data Layer)**: Connessione a database relazionale (MySQL 8.0 / Amazon RDS) tramite Spring Data JPA / Hibernate.

Il codice è impacchettato tramite un **Dockerfile Multi-Stage** che ottimizza la build e utilizza una JRE *distroless* per il runtime. L'app gira con un utente di sistema ristretto (`springuser`) per ragioni di sicurezza. I log sono inviati in `STDOUT` rispettando i dogmi delle "12-Factor App".

---

## 💻 Fase 1: Esecuzione Locale (Kubernetes)

Tutti i manifesti necessari si trovano nella cartella `infrastructure/k8s/`.

**Prerequisiti:** Docker Desktop avviato con Kubernetes abilitato.

1. Costruire l'immagine Docker locale:
   ```bash
   cd server-springboot-maps
   docker build -t maps-app:latest .
   ```
2. Effettuare il deploy sul cluster Kubernetes:
   ```bash
   cd ..
   kubectl apply -f infrastructure/k8s/
   ```
3. Verificare lo stato dei pod (attendere che siano in `Running`):
   ```bash
   kubectl get pods
   ```
4. Accedere all'applicazione su: `http://localhost:30080`

> 🛡️ **Nota Sicurezza:** I secret non sono mai pushati su repository. I file come `mysql-secret.yaml` e gli state locali (`.tfstate`) sono rigorosamente ignorati tramite `.gitignore`.

---

## ☁️ Fase 2: Deploy in Cloud AWS (Infrastructure as Code)

Invece di adottare servizi managed costosi e "Black-Box" come EKS o ECS Fargate, l'infrastruttura Cloud è stata progettata per garantire il **massimo controllo sui nodi (IaaS)** creando un vero cluster in Cloud.

### Architettura AWS (K3s su EC2)
- **VPC Custom**: Rete isolata con subnet separate (pubbliche e private) per proteggere il database.
- **Cluster Kubernetes (K3s) su EC2**: Terraform provisiona istanze EC2 (nodi Master e Worker) installando e configurando K3s (una distribuzione Kubernetes leggera) in modo automatizzato via SSH.
- **Application Load Balancer (ALB)**: Funge da proxy inverso. Instrada il traffico ai nodi EC2 ed effettua continui **Health Checks** per isolare i nodi in caso di fault (ad es. per caduta del nodo master o per pod unhealthy).
- **Amazon RDS (MySQL)**: Database managed protetto nelle Subnet Private.
- **AWS SSM Parameter Store**: Gestione enterprise dei segreti. Le password (RDS e API Key) sono archiviate in AWS SSM e lette dinamicamente da Terraform in modo cifrato per generare i secret K8s senza hardcodarle nei manifesti.

### 🚀 Deploy "One-Click" (PowerShell Magic)
Per la fase di rilascio è stato creato un potente script PowerShell che orchestra **Terraform**, attende la creazione delle risorse AWS, esegue il setup di rete, recupera gli IP delle macchine ed esegue il deploy dei manifesti Kubernetes direttamente nel cluster in cloud:

```powershell
cd infrastructure
.\deploy_k3s_windows.ps1
```
*(Lo script automatizza l'intero processo per 5-10 minuti e restituirà alla fine l'URL del Load Balancer su cui testare l'app in produzione!).*

### Pipeline Automatica (CI/CD)
Il progetto include una pipeline GitHub Actions (`.github/workflows/deploy.yml`). Ad ogni push, la CI compila il codice e spinge la nuova immagine Docker su **Amazon ECR (Elastic Container Registry)** in AWS, pronta per essere scaricata dai nodi worker.

### Esecuzione di Terraform (Manuale)
```bash
cd infrastructure/aws-terraform
terraform init
terraform apply -var="db_password=PASSWORD" -var="google_api_key=API_KEY"
```

### Esecuzione di Terraform (Automattico)
```bash
cd infrastructure
.\deploy_k3s_windows.ps1
```

Restituirà l'URL del Load Balancer su cui testare l'app.

### Distruzione Risorse (Cost Saving)
Per smantellare l'intera infrastruttura Cloud e azzerare i costi (spegnendo RDS, ALB, EC2 e VPC):
```bash
cd infrastructure/aws-terraform
terraform destroy -auto-approve -var="db_password=PASSWORD" -var="google_api_key=API_KEY"
```