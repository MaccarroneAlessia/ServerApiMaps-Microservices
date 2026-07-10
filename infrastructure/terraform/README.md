# Terraform (Local VMs via Multipass)

Questa implementazione isolata di **Terraform** è preposta alla creazione rapida di un ambiente locale di test, tramite il provider **Multipass** sviluppato da Canonical. Multipass opera come un gestore di Macchine Virtuali leggero, ideale per instaurare un ambiente basato nativamente su Ubuntu senza appesantire il sistema con classici hypervisor monolitici.

Mentre la componente `aws-terraform` si occupa della Produzione in Cloud, questa directory crea un vero e proprio Lab Locale. Funziona creando un cluster multi-nodo isolato all'interno del proprio computer locale su cui testare o sviluppare gli applicativi Kubernetes senza intaccare in alcun modo i costi di AWS.

## 📂 Struttura

- **`main.tf`**: Istruisce Multipass al download dell'immagine base (es. Ubuntu Jammy 22.04 LTS) e alla configurazione di due VM separate (un Master e un Worker). Regola anche le risorse assegnate (CPU, RAM, storage) e genera gli IP dinamici tramite i template di Ansible servendosi di Provisioner locali.
- **`outputs.tf`**: File dedicato all'output per mostrare rapidamente a terminale gli indirizzi IP appena assegnati ai nodi locali.

## 💡 Architettura e Design

1. **Ottimizzazione Locale tramite Multipass**: Si tratta di una scelta architetturale mirata ad avere la migliore e più veloce esperienza locale per Ubuntu su Windows o Mac. Multipass simula in maniera sorprendentemente vicina l'esperienza cloud AWS EC2 per i test di infrastruttura.
2. **Generazione Dinamica dell'Inventory**: Sfruttando la risorsa `local_file` di Terraform accoppiata alla direttiva template, l'esecutore inietta autonomamente gli IP generati durante l'accensione all'interno della directory `ansible`. In questo modo Terraform ed Ansible dialogano nativamente.

## 🚀 Comandi per il Deploy

Per poter creare l'ambiente virtuale, occorre aver installato Terraform e Multipass.

```bash
cd infrastructure/terraform

# Inizializza il provider locale
terraform init

# Genera e accende le macchine virtuali locali
terraform apply -auto-approve

# Dopo il test, distrugge le VM liberando spazio su disco
terraform destroy -auto-approve
```

Successivamente al `terraform apply`, è possibile recarsi nella directory Ansible per eseguire il provisioning di K3s al loro interno.
