# 🗺️ ServerApiMaps-Microservices

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-K3s-blue.svg)](https://k3s.io/)
[![Terraform](https://img.shields.io/badge/Terraform-IaC-purple.svg)](https://www.terraform.io/)
[![AWS](https://img.shields.io/badge/AWS-Cloud-yellow.svg)](https://aws.amazon.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF.svg)](https://github.com/features/actions)

Progetto universitario per l'esame di **Sistemi Cloud** e **Ingegneria dei Sistemi Distribuiti**.  
Questa repository contiene l'intero sviluppo, la containerizzazione e l'automazione infrastrutturale (Infrastructure as Code) di un'applicazione a microservizi basata su **Spring Boot** per l'analisi del traffico, dotata di geocoding integrato tramite le API di Google Maps.

Il focus principale del progetto è sulle metodologie **DevOps moderne**, sull'infrastruttura **Cloud Ibrida** e sulla **Fault-Tolerance**.

---

## 🏗️ Architettura del Progetto

L'applicazione e l'infrastruttura sono strutturate seguendo pattern avanzati per i sistemi distribuiti:

### 1. Livello Applicativo (Backend)
- **Framework**: Spring Boot 3 con Java 17.
- **Resilienza**: L'integrazione con Google Maps è protetta tramite **Resilience4j** (implementando `Circuit Breaker`, `Retry` e `TimeLimiter`) per prevenire fault a cascata.
- **Asincronia**: I processi di raccolta dati usano `CompletableFuture` e Message Brokering (RabbitMQ).
- **Sicurezza**: L'immagine Docker usa una JRE *distroless* e l'app gira con utente ristretto (`springuser`).

### 2. Livello Infrastruttura Cloud (AWS)
Invece di adottare servizi managed "black-box" (come EKS), l'infrastruttura Cloud è stata progettata per garantire il massimo controllo sui nodi tramite **Terraform (IaC)**:
- **VPC Custom**: Rete isolata con subnet pubbliche e private.
- **Amazon RDS (MySQL)**: Database relazionale isolato e protetto nelle Subnet Private.
- **Application Load Balancer (ALB)**: Proxy inverso con Health Checks verso i pod.
- **AWS Parameter Store**: Gestione Zero-Trust dei segreti (API keys, password DB).

### 3. Livello Orchestrazione (Kubernetes)
Il deploy avviene tramite **K3s** (distribuzione Kubernetes leggera) configurato automaticamente sui nodi EC2.
Le best-practice K8s implementate includono:
- **Resource Limits & Requests**: Prevenzione contro Memory Leak ed esaurimento risorse (OOMKilled).
- **Liveness & Readiness Probes**: Monitoraggio attivo dei container via Actuator.
- **ImagePullSecrets**: Autenticazione K8s verso AWS ECR (Elastic Container Registry) per le immagini private.

---

## 📂 Struttura della Repository

- `/server-springboot-maps`: Codice sorgente Java dell'applicazione e unit tests.
- `/infrastructure/aws-terraform`: Codice IaC per la creazione delle risorse fisiche su AWS.
- `/infrastructure/k8s`: Manifesti Kubernetes (`Deployment`, `Service`, `Secret`).
- `/infrastructure/ansible`: Playbook Ansible per il configuration management dei nodi K3s.
- `/.github/workflows`: Pipeline CI/CD automatizzata in GitHub Actions.
- `/materiale`: Risorse di studio, resoconti di ispezione e Q&A preparatorie all'esame.

---

## 🚀 Getting Started

Il progetto può essere eseguito localmente o sul Cloud AWS. Prima di iniziare, è fondamentale configurare i prerequisiti comuni.

### 🔑 Prerequisito: Google Maps API Key
Per il corretto funzionamento dell'applicativo (geocoding e analisi del traffico), è richiesta una chiave API di Google Maps valida:
1. Vai sulla [Google Cloud Console - API e Servizi](https://console.cloud.google.com/apis/credentials).
2. Crea un nuovo progetto (se non lo hai già) e assicurati che la fatturazione sia abilitata.
3. Clicca in alto su **Crea credenziali** -> **Chiave API**.
4. Copia la chiave generata. *(chiave senza restrizioni di indirizzo IP, altrimenti il Load Balancer cloud verrebbe bloccato).*

---

### Fase 1: Esecuzione Locale (Docker & Kubernetes locale)

**Prerequisiti Locali:** 
- Avere **Docker Desktop** installato e *avviato* sul proprio computer.
- Dalle impostazioni di Docker Desktop (Settings), andare su **Kubernetes** e spuntare **"Enable Kubernetes"**.

1. **Configurazione dei Segreti (`mysql-secret.yaml`)**:
   Prima di fare il deploy locale, devi inserire la tua API Key nel cluster. Kubernetes richiede che i segreti siano in formato **Base64**.
   - Converti la tua API Key in Base64 (puoi usare un tool online o da terminale: `echo -n "TUA_CHIAVE" | base64`).
   - Apri il file `infrastructure/k8s/mysql-secret.yaml`.
   - Modifica il valore `GOOGLE_MAPS_API_KEY: ` inserendo la tua stringa in Base64 appena generata. (Fai lo stesso per le password del DB se le hai modificate).

2. **Build dell'immagine locale**:
   ```bash
   cd server-springboot-maps
   docker build -t maps-app:latest .
   ```

3. **Deploy sul cluster locale**:
   ```bash
   cd ..
   kubectl apply -f infrastructure/k8s/
   ```

4. L'applicazione sarà disponibile su: `http://localhost:30080`

---

### Fase 2: Deploy in Cloud (AWS & Terraform)

Il deploy in cloud è **totalmente automatizzato** tramite script multi-piattaforma. Crea da zero la VPC, le macchine EC2, installa Kubernetes e lancia l'applicativo, iniettando automaticamente i segreti in modo sicuro.

**Prerequisiti:** Account AWS configurato (chiavi in `~/.aws/credentials`), Terraform installato.

#### Su macOS / Linux / WSL (via Ansible):
```bash
cd infrastructure
chmod +x deploy_k3s_linux.sh
./deploy_k3s_linux.sh "PasswordDB" "ApiKeyGoogle"
```

#### Su Windows (via PowerShell e SSH agent-less):
```powershell
cd infrastructure
.\deploy_k3s_windows.ps1 -DbPassword "PasswordDB" -GoogleApiKey "ApiKeyGoogle"
```

> **Nota:** Lo script PowerShell si occuperà automaticamente di codificare in Base64 la API Key e la password del DB, per poi iniettarle nei Secret di K3s in modo sicuro, senza esporle nel file YAML hardcodato. Al termine del processo, lo script restituirà l'URL dell'Application Load Balancer pubblico.

---

## 🔄 CI/CD Pipeline

Ad ogni commit sul branch `main`, la pipeline di GitHub Actions si occupa di:
1. Eseguire la suite di test (**Test Gate** con Mockito).
2. Se i test passano, eseguire la build dell'immagine Docker.
3. Inviare l'immagine su **AWS ECR** (Elastic Container Registry).
4. Avviare un **Rolling Update** su Kubernetes (K3s Master Node) senza downtime tramite SSH remoto.

### 🔐 Configurazione dei Segreti di GitHub Actions
Per far funzionare il deploy automatico su cloud a ogni `git push`, devi configurare i seguenti **Repository Secrets** su GitHub (vai su **Settings > Secrets and variables > Actions > New repository secret**):

- `AWS_ACCESS_KEY_ID`: La tua chiave di accesso pubblica dell'account AWS.
- `AWS_SECRET_ACCESS_KEY`: La tua chiave segreta dell'account AWS.
- `K3S_MASTER_IP`: L'indirizzo IP pubblico del nodo Master (lo vedi stampato alla fine dell'esecuzione dello script PowerShell o Terraform).
- `K3S_SSH_KEY`: Il contenuto testuale completo della chiave privata SSH generata da Terraform per accedere alle macchine (apri con il blocco note il file locale `infrastructure/ansible/k3s-key.pem` e copia tutto il contenuto, compresi i tag BEGIN/END).
- `SPRING_DATASOURCE_URL`: L'URL di connessione al database RDS, nel formato esatto (l'endpoint RDS viene restituito dall'output finale dello script o di Terraform).

---

## 🧹 Cost Saving (Distruzione Risorse)

Per smantellare l'intera infrastruttura Cloud ed evitare addebiti indesiderati a fine test/esame:

```bash
cd infrastructure/aws-terraform
terraform destroy -auto-approve -var="db_password=PASS" -var="google_api_key=API_KEY"
```