# Ansible Configuration Management

**Ansible** è uno strumento di Configuration Management e automazione IT sviluppato da RedHat. Essendo strettamente *agent-less* (ovvero non richiedendo l'installazione di demoni sui nodi client), funziona connettendosi ai server remoti tramite SSH e applicando configurazioni dichiarative scritte in formato YAML, chiamate Playbooks.

Il compito di questa cartella si colloca subito dopo il provisioning infrastrutturale. Una volta che Terraform ha creato le "scatole vuote" (macchine EC2 su AWS o VM locali), Ansible interviene per "dar loro vita", installando e configurando automaticamente un cluster **Kubernetes (K3s)** senza alcun intervento manuale umano via SSH.

## 📂 Struttura

- **`inventory.tmpl`**: Il template base impiegato per generare dinamicamente l'inventario. Poiché gli IP delle macchine vengono decisi in modo dinamico alla loro creazione, Terraform si preoccupa di iniettarli in questo file per istruire Ansible su quali nodi siano il Master e i Worker.
- **`playbook-k3s.yml`**: Il file YAML principale che contiene la coreografia. Esegue lo scaricamento di K3s sul master, ne estrae il token di sicurezza e lo invia ai worker per registrarli nel medesimo cluster.

## 💡 Architettura e Design

1. **Idempotenza**: Ansible assicura che il playbook sia sicuro da rieseguire. Se eseguito su macchine già parzialmente configurate, modificherà soltanto ciò che non è nello stato desiderato, senza compromettere il lavoro preesistente.
2. **K3s Automato**: Al posto di configurare manualmente Kubernetes o affidarsi a script bash incapsulati in EC2 User Data, la scelta di Ansible consente di estrarre in automatico il Token generato dinamicamente sul Master Node e inviarlo con precisione chirurgica ai Worker Node, assicurando l'unione al cluster.

## 🚀 Comandi per il Deploy

Normalmente, l'esecuzione di Ansible è inglobata e triggerata in modo trasparente dallo script PowerShell principale. 
Se tuttavia è necessario eseguire l'installazione di Kubernetes su un cluster IaaS in modo indipendente:

```bash
cd infrastructure/ansible

# Assicurarsi che l'inventory/hosts.ini sia stato generato correttamente
ansible-playbook -i inventory/hosts.ini playbook-k3s.yml --private-key /percorso/alla/chiave.pem
```
