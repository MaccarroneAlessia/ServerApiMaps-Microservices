# AWS Terraform (Infrastructure as Code)

**Terraform** is an Infrastructure as Code (IaC) tool used to declare and orchestrate hardware architecture (Networks, EC2, Databases, Load Balancers) via configuration files in HCL syntax. This folder hosts code specifically dedicated to the **Amazon Web Services (AWS)** provider.

The real infrastructure, network topology, and physical servers running the application reside here as code. By replacing traditional "ClickOps" from the AWS Web Console with Terraform, the entire corporate data center becomes versionable, 100% reproducible, and documented by design.

## 📂 Structure

- **`main.tf`**: Main file for declaring base configuration and providers.
- **`network.tf`**: Declares the private VPC network, defining public and private Subnets, Internet Gateways, and Route Tables.
- **`ec2.tf`**: Provisions the EC2 compute nodes that will host Kubernetes (Master/Worker).
- **`rds.tf`**: Declares the managed Amazon RDS database for MySQL.
- **`alb.tf`**: Defines the Application Load Balancer necessary to balance traffic across active Worker nodes.
- **`security.tf`**: Configures Security Groups to ensure secure communications via precise firewall rules among components.
- **`ssm.tf`**: Allows controlled access to AWS Parameter Store to securely fetch credentials without hardcoding them.
- **`variables.tf`** and **`outputs.tf`**: Define injectable input parameters and outputs returned upon completion, respectively.

## 💡 Architecture & Design

1. **Pure IaaS (Kubernetes Cluster on EC2)**: We opted to maintain a pure cluster by launching "bare" EC2 machines to independently install K3s, rather than embracing expensive managed services like AWS EKS. This choice yields maximum control over the underlying servers, heavily optimizing costs for testing and small/medium-sized environments.
2. **Database Isolation**: The MySQL database in RDS is confined to a Private Subnet. It is completely unreachable from the Internet, being exclusively accessible by the Kubernetes Worker nodes through strict Security Group rules.
3. **SSM Parameter Store for Secrets**: Sensitive strings (DB passwords, API keys) are dynamically read at runtime via AWS SSM, preventing any risk of leaks on Git.

## 🚀 Deployment Commands

Orchestration is primarily delegated to the root PowerShell script. 
For manual provisioning management on AWS:

```bash
cd infrastructure/aws-terraform

# Download the required provider plugins (AWS)
terraform init

# Generate an execution plan previewing the resources to be created
terraform plan -var="db_password=YOUR_PASSWORD" -var="google_api_key=YOUR_APIKEY"

# Execute real cloud provisioning
terraform apply -var="db_password=YOUR_PASSWORD" -var="google_api_key=YOUR_APIKEY"
```

To tear down the infrastructure and avoid costs when finished:
```bash
terraform destroy -var="db_password=YOUR_PASSWORD" -var="google_api_key=YOUR_APIKEY"
```
