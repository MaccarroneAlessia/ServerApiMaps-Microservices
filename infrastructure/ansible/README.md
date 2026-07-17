# Ansible Configuration Management

**Ansible** is an IT configuration management and automation tool developed by RedHat. Being strictly *agent-less* (meaning it does not require the installation of daemons on client nodes), it works by connecting to remote servers via SSH and applying declarative configurations written in YAML format, called Playbooks.

The purpose of this folder comes immediately after infrastructural provisioning. Once Terraform has created the "empty boxes" (EC2 machines on AWS or local VMs), Ansible steps in to "bring them to life," automatically installing and configuring a **Kubernetes (K3s)** cluster without any manual human intervention via SSH.

## 📂 Structure

- **`inventory.tmpl`**: The base template used to dynamically generate the inventory. Since machine IPs are determined dynamically upon creation, Terraform handles injecting them into this file to instruct Ansible on which nodes are the Master and the Workers.
- **`playbook-k3s.yml`**: The main YAML file containing the choreography. It handles the download of K3s on the master, extracts its security token, and sends it to the workers to register them in the same cluster.

## 💡 Architecture & Design

1. **Idempotency**: Ansible ensures that the playbook is safe to rerun. If run on partially configured machines, it will only modify what is not in the desired state, without compromising pre-existing work.
2. **Automated K3s**: Instead of manually configuring Kubernetes or relying on bash scripts encapsulated in EC2 User Data, choosing Ansible allows for the automatic extraction of the dynamically generated Token on the Master Node and surgically sends it to the Worker Nodes, ensuring cluster joining.

## 🚀 Deployment Commands

Normally, Ansible execution is seamlessly embedded and triggered by the main PowerShell script. 
However, if it is necessary to run the Kubernetes installation on an IaaS cluster independently:

```bash
cd infrastructure/ansible

# Ensure the inventory/hosts.ini has been correctly generated
ansible-playbook -i inventory/hosts.ini playbook-k3s.yml --private-key /path/to/key.pem
```
