# 🛠️ Stack Tecnologico e Strumenti (Tools)

In questo documento è riassunto l'intero stack architetturale utilizzato per costruire, automatizzare e rilasciare l'applicazione, con il dettaglio dei file principali che ne governano la logica.

---

## 1. Sviluppo Backend & Logica Applicativa

### 🍃 Spring Boot (Core Applicativo)
- È il framework Java su cui si basa l'intero core dell'applicazione. Gestisce la logica di business, l'esposizione delle API REST e l'interazione con il database.
- Il codice sorgente si trova in [../server-springboot-maps/src/main/java/](../server-springboot-maps/src/main/java/).
  - Contiene i **Controller** (per gestire le richieste HTTP), i **Service** (per la logica di business) e le classi **Entity/Repository** per mappare i dati su MySQL.

### 🐇 RabbitMQ & Amazon SQS (Messaggistica Asincrona)
- Implementa il pattern *Publisher-Subscriber* per disaccoppiare la ricezione del traffico dal salvataggio su DB, proteggendo il sistema da picchi di carico.
- Si trovano nel package [messaging/](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/).
  - [RabbitMqConfig.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/RabbitMqConfig.java): Inizializza l'infrastruttura di coda.
  - [RabbitMqPublisher.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/RabbitMqPublisher.java) / [SqsPublisher.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/SqsPublisher.java): Ricevono i dati dal Controller e li inviano istantaneamente alla coda asincrona (RabbitMQ in locale, SQS sul Cloud tramite il profilo `@Profile("cloud")`).
  - [RabbitMqTrafficMessageListener.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/RabbitMqTrafficMessageListener.java) / [SqsTrafficMessageListener.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/SqsTrafficMessageListener.java): Ascoltano la coda in background e scrivono i dati fisicamente nel database con il loro ritmo.

### 🛡️ Resilience4j (Fault Tolerance)
- Protegge l'applicazione da guasti esterni implementando il pattern *Circuit Breaker*.
- La libreria è integrata tramite annotazioni direttamente nei Service Java (es. [GeocodingService.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/service/GeocodingService.java)), intercettando e gestendo eventuali indisponibilità delle API di Google Maps senza far crollare il backend.

---

## 2. Infrastructure as Code (IaC) e Automazione

### 🏗️ Terraform
- Esegue il *Provisioning* dell'hardware sul Cloud (AWS). Costruisce l'infrastruttura immutabile.
- Moduli in [../infrastructure/aws-terraform/](../infrastructure/aws-terraform/).
  - [network.tf](../infrastructure/aws-terraform/network.tf): Crea la Virtual Private Cloud (VPC) e le Subnet pubbliche.
  - [ec2.tf](../infrastructure/aws-terraform/ec2.tf): Provisiona i server virtuali (Master e Worker).
  - [rds.tf](../infrastructure/aws-terraform/rds.tf): Crea il database relazionale MySQL gestito.
  - [alb.tf](../infrastructure/aws-terraform/alb.tf): Configura l'Application Load Balancer per smistare il traffico da internet verso i nodi EC2.

### ⚙️ Ansible
- Esegue il *Configuration Management* agent-less. Entra via SSH nei server crudi creati da Terraform e li trasforma in un cluster Kubernetes.
- Playbook in [../infrastructure/ansible/](../infrastructure/ansible/).
  - [inventory.tmpl](../infrastructure/ansible/inventory.tmpl): Il template della mappa dei server, popolato dinamicamente con gli IP di Terraform.
  - [playbook-k3s.yml](../infrastructure/ansible/playbook-k3s.yml): Il copione che si collega al Master, scarica K3s, estrae il token di sicurezza, e poi si collega al Worker iniettando il token per farlo unire al cluster.

### 📜 PowerShell / Bash Scripts
- È il "collante" imperativo. Unisce Terraform e la configurazione dei server in un unico flusso automatizzato "one-click".
- Script [deploy_k3s_windows.ps1](../infrastructure/deploy_k3s_windows.ps1) (e variante Linux [deploy_k3s_linux.sh](../infrastructure/deploy_k3s_linux.sh)).
  - Lancia Terraform, ne legge l'output JSON per estrarre gli IP, si collega via SSH ai server per configurare K3s (fungendo da workaround Windows per Ansible), inietta i Secret crittografati nel cluster e lancia i manifesti Kubernetes. Abbatte i tempi di rilascio azzerando l'errore umano.

---

## 3. Container & Orchestrazione

### 🐳 Docker
- Containerizza l'applicazione Spring Boot rendendola portabile e isolata.
- [Dockerfile](../server-springboot-maps/Dockerfile): Utilizza una *Multi-stage build* (Maven per compilare, JRE leggero per eseguire), garantendo un'immagine finale sicura e ottimizzata.

### ☸️ Kubernetes (K3s)
- Orchestra i container garantendo *High Availability* e *Self-Healing*. Astrae l'hardware sottostante.
- Manifesti YAML in [../infrastructure/k8s/](../infrastructure/k8s/).
  - [app-deployment.yaml](../infrastructure/k8s/app-deployment.yaml): Definisce le repliche del container Spring Boot (Deployment) e come smistare il traffico in ingresso dall'ALB verso i Pod (Service di tipo NodePort).
  - [app-configmap.yaml](../infrastructure/k8s/app-configmap.yaml) / [mysql-secret.yaml](../infrastructure/k8s/mysql-secret.yaml): Separano la configurazione dal codice (Zero-Trust), iniettando variabili d'ambiente e password crittografate senza toccare l'immagine Docker.

---

## 4. Continuous Integration e Deployment (CI/CD)

### 🐙 GitHub Actions
- Pipeline di automazione cloud-native. Ad ogni push, testa, compila e rilascia la nuova versione in produzione senza disservizi.
- Workflow [.github/workflows/deploy.yml](../.github/workflows/deploy.yml).
  - Configura l'ambiente (Java), esegue i test Maven per verificare la solidità, compila l'immagine Docker, fa il login su AWS, pusha l'immagine sul registry ECR, e infine aggiorna il cluster K3s tramite SSH (eseguendo un *Rolling Update* dei pod).

---

## 🚀 Avvio del progetto

### 🔴 Fase 1: Deploy in Locale (Test e Sviluppo)
Questa fase permette di testare l'applicativo sul proprio computer prima di procedere con l'automazione cloud.
1. Aprire il progetto backend (`server-springboot-maps`) in un IDE.
2. Assicurarsi di avere un database MySQL attivo in locale (es. XAMPP o Docker) e verificare le credenziali nel file `application-local.properties`.
3. Avviare l'applicazione Spring Boot eseguendo la classe principale `ServerSpringbootMapsApplication.java`.
4. Testare le API tramite Postman o browser all'indirizzo `http://localhost:8080`.

### 🔵 Fase 2: Deploy Cloud (Produzione Automatica)
Questa fase automatizza la creazione dell'infrastruttura AWS e il rilascio del software tramite CI/CD.

**Step A: Creazione Infrastruttura (Terraform + Ansible)**
1. Inserire le credenziali AWS valide nel file locale `~/.aws/credentials`.
2. Aprire il terminale nella cartella `infrastructure/` e lanciare lo script di automazione (inserendo password e API key):
   - **Su Windows:** `powershell -ExecutionPolicy Bypass -File .\deploy_k3s_windows.ps1 -DbPassword "PASS" -GoogleApiKey "API_KEY"`
   - **Su Linux/Mac:** `./deploy_k3s_linux.sh -p "PASS" -g "API_KEY"`
3. Al termine dell'esecuzione (circa 5 minuti), lo script stamperà in console l'**IP del Master K3s** e l'**URL del Database RDS**.

**Step B: Rilascio Software (GitHub Actions)**
1. Andare sulla repository GitHub in **Settings > Secrets and variables > Actions**.
2. Aggiornare i segreti con i valori appena restituiti dallo script (`K3S_MASTER_IP`, `SPRING_DATASOURCE_URL`, `K3S_SSH_KEY`) e le credenziali AWS.
3. Andare nel tab **Actions** di GitHub, selezionare il workflow di deploy e cliccare su **Run workflow**. 
4. Al termine della pipeline (semaforo verde), l'applicazione sarà online e raggiungibile tramite l'indirizzo DNS dell'Application Load Balancer (stampato dallo script al punto 3).

*(A fine utilizzo, per distruggere l'intera infrastruttura e azzerare i costi, eseguire `terraform destroy -auto-approve` all'interno della cartella `infrastructure/aws-terraform/`).*
