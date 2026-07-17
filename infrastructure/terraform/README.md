# Terraform (Local VMs via Multipass)

This isolated **Terraform** implementation is dedicated to the rapid creation of a local testing environment, using the **Multipass** provider developed by Canonical. Multipass operates as a lightweight Virtual Machine manager, ideal for establishing a natively Ubuntu-based environment without bogging down the system with classic monolithic hypervisors.

While the `aws-terraform` component handles Cloud Production, this directory builds a full-fledged Local Lab. It works by creating an isolated multi-node cluster right on your local computer to test or develop Kubernetes applications without incurring any AWS costs whatsoever.

## 📂 Structure

- **`main.tf`**: Instructs Multipass to download the base image (e.g., Ubuntu Jammy 22.04 LTS) and configures two separate VMs (one Master and one Worker). It also regulates allocated resources (CPU, RAM, storage) and generates dynamic IPs via Ansible templates using local Provisioners.
- **`outputs.tf`**: File dedicated to the output, to rapidly display the newly assigned IP addresses for the local nodes in the terminal.

## 💡 Architecture & Design

1. **Local Optimization via Multipass**: This is an architectural choice aimed at providing the best and fastest local experience for Ubuntu on Windows or Mac. Multipass simulates the AWS EC2 cloud experience surprisingly well for infrastructure testing.
2. **Dynamic Inventory Generation**: By leveraging the Terraform `local_file` resource coupled with the template directive, the executor autonomously injects the IPs generated during startup directly into the `ansible` directory. In this way, Terraform and Ansible communicate natively.

## 🚀 Deployment Commands

To create the virtual environment, you must have Terraform and Multipass installed.

```bash
cd infrastructure/terraform

# Initialize the local provider
terraform init

# Generate and start the local virtual machines
terraform apply -auto-approve

# After testing, destroy the VMs to free up disk space
terraform destroy -auto-approve
```

Following the `terraform apply`, you can navigate to the Ansible directory to execute K3s provisioning within them.
