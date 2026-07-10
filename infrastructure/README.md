# Infrastructure & DevOps Orchestration

Questa directory funge da radice per tutte le operazioni inerenti il DevOps, l'infrastruttura e l'automazione. Essa riunisce gli script di orchestrazione PowerShell e raggruppa gli strumenti per fare Infrastructure as Code (Terraform), Configuration Management (Ansible) e orchestrazione dei container (Kubernetes).

Lo scopo principale è mantenere una netta separazione tra il codice sorgente dell'applicazione e il codice necessario per definire dove e come questa applicazione deve essere eseguita. Piuttosto che far affidamento su configurazioni manuali (ClickOps) tramite Console AWS o decine di comandi sparsi, l'automazione è centralizzata in un processo end-to-end.

## 📂 Struttura

- **`deploy_k3s_windows.ps1`**: Il vero cuore dell'automazione. Questo script PowerShell agisce come un coordinatore universale:
  1. **Provisioning AWS (Terraform)**: Istanzia la rete VPC, i nodi EC2 e il Database su AWS.
  2. **Data Extraction**: Cattura l'IP pubblico dei nuovi nodi e genera dinamicamente l'inventario per Ansible.
  3. **Configuration (Ansible)**: Installa il cluster Kubernetes (K3s) sulle macchine create.
  4. **Deploy K8s**: Recupera i secret da AWS SSM, imposta il `kubeconfig` e lancia i manifesti K8s per dispiegare l'app Spring Boot.
- Le sottocartelle (`aws-terraform`, `terraform`, `ansible`, `k8s`) racchiudono i file specifici per ogni tecnologia.

## 💡 Architettura e Design

L'approccio scelto mira all'automazione totale "One-Click". Tramite un singolo comando, l'errore umano viene eliminato, abbattendo il tempo di deploy di un'infrastruttura cloud da ore a circa 5-10 minuti. Lo script PowerShell garantisce supporto su macchine di sviluppo Windows, ma i concetti impiegati sono universali e portabili su Bash.

## 🚀 Comandi per il Deploy

Per portare in produzione l'intero progetto partendo da zero, è sufficiente lanciare lo script di orchestrazione:

```powershell
cd infrastructure
.\deploy_k3s_windows.ps1
```

L'esecuzione mostrerà ogni passaggio a terminale, restituendo al termine l'URL pubblico del Load Balancer (ALB) da cui raggiungere il sito. Per eseguire i tool singolarmente, fare riferimento ai README delle rispettive sottocartelle.
