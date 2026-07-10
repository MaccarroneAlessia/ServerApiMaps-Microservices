# Kubernetes (K8s / K3s) Manifests

**Kubernetes (K8s)** è lo standard de facto open-source per l'automazione, il deployment e la gestione operativa di applicazioni containerizzate. In questo progetto è utilizzata una derivazione estremamente leggera e ottimizzata denominata **K3s** (Rancher). Questa cartella è il repository dei cosiddetti "Manifesti", ovvero file YAML che descrivono in modo dichiarativo come deve essere modellato l'ecosistema a runtime.

L'infrastruttura software a runtime è qui definita. Invece di lanciare isolatamente singoli container (come si farebbe tramite Docker CLI), le configurazioni istruiscono il control plane di Kubernetes su quanti pod desideriamo, su che porte esporli, come si parlano tra di loro e in che modo iniettare la configurazione. Sarà poi Kubernetes ad assicurarsi di mantenere lo "stato desiderato", rimpiazzando in autonomia eventuali pod non funzionanti tramite self-healing.

## 📂 Struttura

La directory racchiude i Manifesti YAML da applicare:
- **`app-deployment.yaml`**: Descrive l'istanza dell'Applicazione Spring Boot. Stabilisce da quale repository scaricare l'immagine Docker, quanti limiti di risorse (CPU/RAM) imporre e il numero di repliche. Include contestualmente il Service associato (`NodePort`) per l'esposizione.
- **`mysql-deployment.yaml`**: Definisce l'infrastruttura del database locale MySQL nel caso in cui non si stia usando l'AWS RDS managed. Include Deployment, Service e dichiarazione del Persistent Volume.
- **`app-configmap.yaml`**: Contiene la mappa delle variabili d'ambiente non sensibili (ad es. l'URL di connessione).
- **`*-secret.yaml`**: (Attenzione: volutamente ignorati via `.gitignore`). File che incapsulano dati sensibili come API keys o le credenziali generate dallo script.
- **`cloud/`**: Cartella adibita a ospitare i manifesti Kubernetes dedicati specificamente all'ambiente cloud (AWS).

## 💡 Architettura e Design

1. **Esportazione tramite NodePort**: I pod dell'applicazione sono esposti usando una NodePort statica elevata (es. `30080`) a livello di istanza EC2. Dopodiché interviene l'Application Load Balancer (ALB) predisposto da Terraform, che aggancia tale porta intercettando e filtrando il traffico in ingresso verso i cluster node.
2. **Isolamento della Configurazione**: Le chiavi crittografiche (Google Maps) e le password del database MySQL sono isolate nel layer Secret, separate nettamente dall'immagine Docker. Il file `mysql-secret.yaml` resta escluso dal controllo di versione.
3. **Decoupling Ambientale**: I manifesti presenti sono agnostici rispetto all'ambiente. È possibile applicare l'applicazione indifferentemente sul Docker Desktop locale o sull'ecosistema remoto AWS senza dover stravolgere la base del deployment.

## 🚀 Comandi per il Deploy

Lo script PowerShell radice si occupa di iniettare autonomamente questi manifesti verso AWS.
Qualora si preferisse lanciare il cluster in un ambiente locale (ad es. su Docker Desktop):

```bash
cd infrastructure/k8s

# Applica massivamente tutti i manifesti
kubectl apply -f .

# Verifica lo stato dei Pod 
kubectl get pods

# Elimina tutte le risorse create
kubectl delete -f .
```
