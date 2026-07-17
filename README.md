# 🗺️ ServerApiMaps-Microservices

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-K3s-blue.svg)](https://k3s.io/)
[![Terraform](https://img.shields.io/badge/Terraform-IaC-purple.svg)](https://www.terraform.io/)
[![AWS](https://img.shields.io/badge/AWS-Cloud-yellow.svg)](https://aws.amazon.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF.svg)](https://github.com/features/actions)

University project for the **Cloud Systems** and **Distributed Systems Engineering** exams.  
This repository contains the full development, containerization, and infrastructure automation (Infrastructure as Code) of a microservices application based on **Spring Boot** for traffic analysis, featuring integrated geocoding via Google Maps APIs.

The main focus of the project is on modern **DevOps methodologies**, **Hybrid Cloud** infrastructure, and **Fault-Tolerance**.

---

## 🏗️ Project Architecture

The application and infrastructure are structured following advanced patterns for distributed systems:

### 1. Application Layer (Backend)
- **Framework**: Spring Boot 3 with Java 17.
- **Resiliency**: Integration with Google Maps is protected via **Resilience4j** (implementing `Circuit Breaker`, `Retry`, and `TimeLimiter`) to prevent cascading failures.
- **Asynchronous Processing**: Data collection processes use `CompletableFuture` and Message Brokering (RabbitMQ).
- **Security**: The Docker image uses a *distroless* JRE, and the app runs with a restricted user (`springuser`).

### 2. Cloud Infrastructure Layer (AWS)
Instead of adopting "black-box" managed services (like EKS), the Cloud infrastructure was designed to ensure maximum control over the nodes using **Terraform (IaC)**:
- **Custom VPC**: Isolated network with public and private subnets.
- **Amazon RDS (MySQL)**: Relational database isolated and protected in Private Subnets.
- **Application Load Balancer (ALB)**: Reverse proxy with Health Checks targeting the pods.
- **AWS Parameter Store**: Zero-Trust secret management (API keys, DB passwords).

### 3. Orchestration Layer (Kubernetes)
Deployment is handled via **K3s** (a lightweight Kubernetes distribution) automatically configured on EC2 nodes.
Implemented K8s best practices include:
- **Resource Limits & Requests**: Prevention against Memory Leaks and resource exhaustion (OOMKilled).
- **Liveness & Readiness Probes**: Active container monitoring via Actuator.
- **ImagePullSecrets**: K8s authentication against AWS ECR (Elastic Container Registry) for private images.

---

## 📂 Repository Structure

- `/server-springboot-maps`: Java source code of the application and unit tests.
- `/infrastructure/aws-terraform`: IaC code for provisioning physical resources on AWS.
- `/infrastructure/k8s`: Kubernetes manifests (`Deployment`, `Service`, `Secret`).
- `/infrastructure/ansible`: Ansible playbooks for configuration management of K3s nodes.
- `/.github/workflows`: Automated CI/CD pipeline in GitHub Actions.
- `/materiale`: Study resources, inspection reports, and Q&A prep for the exam.

---

## 🚀 Getting Started

The project can be run locally or on the AWS Cloud. Before starting, it's essential to configure the common prerequisites.

### 🔑 Prerequisite: Google Maps API Key
For the application to function correctly (geocoding and traffic analysis), a valid Google Maps API Key is required:
1. Go to the [Google Cloud Console - APIs & Services](https://console.cloud.google.com/apis/credentials).
2. Create a new project (if you haven't already) and ensure billing is enabled.
3. Click on **Create credentials** -> **API key** at the top.
4. Copy the generated key. *(Use a key without IP address restrictions, otherwise the cloud Load Balancer will be blocked).*

---

### Phase 1: Local Execution (Docker & Local Kubernetes)

**Local Prerequisites:** 
- Have **Docker Desktop** installed and *running* on your computer.
- From Docker Desktop Settings, go to **Kubernetes** and check **"Enable Kubernetes"**.

1. **Secret Configuration (`mysql-secret.yaml`)**:
   Before deploying locally, you must insert your API Key into the cluster. Kubernetes requires secrets to be in **Base64** format.
   - Convert your API Key to Base64 (you can use an online tool or the terminal: `echo -n "YOUR_KEY" | base64`).
   - Open the `infrastructure/k8s/mysql-secret.yaml` file.
   - Modify the `GOOGLE_MAPS_API_KEY: ` value by inserting your newly generated Base64 string. (Do the same for DB passwords if you modified them).

2. **Build the local image**:
   ```bash
   cd server-springboot-maps
   docker build -t maps-app:latest .
   ```

3. **Deploy on the local cluster**:
   ```bash
   cd ..
   kubectl apply -f infrastructure/k8s/
   ```

4. The application will be available at: `http://localhost:30080`

---

### Phase 2: Cloud Deployment (AWS & Terraform)

Cloud deployment is **fully automated** via cross-platform scripts. It creates the VPC, EC2 machines from scratch, installs Kubernetes, and launches the application, securely injecting secrets.

**Prerequisites:** Configured AWS Account (keys in `~/.aws/credentials`), Terraform installed.

#### On macOS / Linux / WSL (via Ansible):
```bash
cd infrastructure
chmod +x deploy_k3s_linux.sh
./deploy_k3s_linux.sh "DbPassword" "GoogleApiKey"
```

#### On Windows (via PowerShell and agent-less SSH):
```powershell
cd infrastructure
.\deploy_k3s_windows.ps1 -DbPassword "DbPassword" -GoogleApiKey "GoogleApiKey"
```

> **Note:** The PowerShell script will automatically encode the API Key and DB password in Base64, injecting them into the K3s Secrets securely, without exposing them in hardcoded YAML files. Upon completion, the script will output the public URL of the Application Load Balancer.

---

## 🔄 CI/CD Pipeline

On every commit to the `main` branch, the GitHub Actions pipeline handles:
1. Running the test suite (**Test Gate** with Mockito).
2. If tests pass, building the Docker image.
3. Pushing the image to **AWS ECR** (Elastic Container Registry).
4. Initiating a **Rolling Update** on Kubernetes (K3s Master Node) with zero downtime via remote SSH.

### 🔐 GitHub Actions Secrets Configuration
To enable automated cloud deployment on every `git push`, configure the following **Repository Secrets** on GitHub (go to **Settings > Secrets and variables > Actions > New repository secret**):

- `AWS_ACCESS_KEY_ID`: Your AWS account public access key.
- `AWS_SECRET_ACCESS_KEY`: Your AWS account secret key.
- `K3S_MASTER_IP`: The public IP address of the Master node (printed at the end of the PowerShell or Terraform script execution).
- `K3S_SSH_KEY`: The full text content of the SSH private key generated by Terraform to access the machines (open the local `infrastructure/ansible/k3s-key.pem` file with notepad and copy everything, including BEGIN/END tags).
- `SPRING_DATASOURCE_URL`: The RDS database connection URL in the exact format (the RDS endpoint is returned by the final script or Terraform output).

---

## 🩺 Diagnostics and Logging

If something goes wrong during cloud execution (e.g., `502` or `504` error from the browser), you can view container logs via the local terminal to identify the issue.

**1. Check container status (Pods):**
From the project root, run:
```bash
ssh -o StrictHostKeyChecking=no -i infrastructure/ansible/k3s-key.pem ubuntu@<K3S_MASTER_IP> "sudo k3s kubectl get pods -A"
```
*(Replace `<K3S_MASTER_IP>` with the public IP of the master instance).*

**2. Read Real-time Application Logs:**
To stream Spring Boot logs live, run:
```bash
ssh -o StrictHostKeyChecking=no -i infrastructure/ansible/k3s-key.pem ubuntu@<K3S_MASTER_IP> "sudo k3s kubectl logs -l app=maps-app --tail=100 -f"
```
*(Press `Ctrl+C` to exit).*

**🛠️ Common Troubleshooting:**
- **Error 502 Bad Gateway:** The Load Balancer works, but the Spring Boot app isn't ready yet (it's starting) or has crashed (possibly due to memory). Check the logs.
- **Error 504 Gateway Time-out:** The Load Balancer cannot communicate with EC2 servers. Machines are likely restarting or frozen.
- **Sluggishness / Sudden Freezes:** Using AWS micro-instances (`t3.micro`/`t3.small`) involves "CPU Credits". When depleted, performance plummets and Kubernetes might appear frozen. Restarting the instance temporarily resets the problem.

---

## ⏸️ Cost Management: Temporary Shutdown (Stop & Start)

To "turn off" the infrastructure and avoid paying when not in use (without destroying it):
1. Go to the AWS Console -> EC2 -> Select the instances -> **Stop instance**.
2. When you want to reuse it, select the instances -> **Start instance**.

⏱️ **Cold Boot Times:**
After restarting the machines, it takes an average of **3 to 4 minutes** for the website to come back online (time needed to boot Linux, K3s, and initialize the Spring Boot container). Don't worry if you receive *502 Bad Gateway* or *504 Gateway Time-out* errors from the Load Balancer during these 3-4 minutes: it's normal during warm-up.

⚠️ **WARNING:** When you perform a *Stop* on AWS, **the Public IP of the instances will change** upon the next startup. 
- The website will continue to work automatically (as the ALB uses internal references).
- **For subsequent deployments (CI/CD)** you must update the `K3S_MASTER_IP` secret on GitHub Actions with the new IP address.
  - *💡 note:* you can find the new IP by running this command from the local terminal:
    ```bash
    aws ec2 describe-instances --region eu-west-1 --filters "Name=tag:Name,Values=maps-k3s-master" --query "Reservations[*].Instances[*].PublicIpAddress" --output text
    ```
  - *Alternative (via AWS Console):* 
    1. Go to the EC2 Dashboard -> **Instances**.
    2. Select the `maps-k3s-master` instance (ensure you're in the *Ireland eu-west-1* region).
    3. In the lower pane (*Details* tab), copy the **Public IPv4 address** value.

---

## 🧹 Cost Saving (Permanent Destruction)

To dismantle the entire Cloud infrastructure and avoid unwanted charges at the end of testing/exam:

```bash
cd infrastructure/aws-terraform
terraform destroy -auto-approve -var="db_password=PASS" -var="google_api_key=API_KEY"
```