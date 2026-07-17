# Infrastructure & DevOps Orchestration

This directory serves as the root for all DevOps, infrastructure, and automation operations. It consolidates PowerShell orchestration scripts and groups tools for Infrastructure as Code (Terraform), Configuration Management (Ansible), and Container Orchestration (Kubernetes).

The main goal is to maintain a strict separation between the application's source code and the code required to define where and how this application should run. Rather than relying on manual configurations (ClickOps) via the AWS Console or dozens of scattered commands, automation is centralized in an end-to-end process.

## 📂 Structure

- **`deploy_k3s_windows.ps1`**: The true heart of the automation. This PowerShell script acts as a universal coordinator:
  1. **AWS Provisioning (Terraform)**: Provisions the VPC network, EC2 nodes, and Database on AWS.
  2. **Data Extraction**: Captures the public IP of the new nodes and dynamically generates the Ansible inventory.
  3. **Configuration (Ansible)**: Installs the Kubernetes cluster (K3s) on the provisioned machines.
  4. **K8s Deployment**: Retrieves secrets from AWS SSM, sets up the `kubeconfig`, and launches the K8s manifests to deploy the Spring Boot app.
- The subfolders (`aws-terraform`, `terraform`, `ansible`, `k8s`) contain the specific files for each technology.

## 💡 Architecture & Design

The chosen approach aims for total "One-Click" automation. Through a single command, human error is eliminated, reducing the deployment time of a cloud infrastructure from hours to approximately 5-10 minutes. The PowerShell script guarantees support on Windows development machines, but the underlying concepts are universal and portable to Bash.

## 🚀 Deployment Commands

To bring the entire project to production starting from scratch, simply run the orchestration script:

```powershell
cd infrastructure
.\deploy_k3s_windows.ps1
```

The execution will output each step to the terminal, returning the public URL of the Load Balancer (ALB) from which the site can be accessed upon completion. To run the tools individually, refer to the READMEs in their respective subfolders.
