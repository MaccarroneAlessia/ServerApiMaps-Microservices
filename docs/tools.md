# 🛠️ Technology Stack and Tools

This document summarizes the entire architectural stack used to build, automate, and release the application, detailing the main files governing its logic.

---

## 1. Backend Development & Application Logic

### 🍃 Spring Boot (Application Core)
- The Java framework underlying the entire core of the application. It manages business logic, REST API exposure, and database interaction.
- Source code is located in [../server-springboot-maps/src/main/java/](../server-springboot-maps/src/main/java/).
  - Contains **Controllers** (handling HTTP requests), **Services** (for business logic), and **Entity/Repository** classes for MySQL data mapping.

### 🐇 RabbitMQ & Amazon SQS (Asynchronous Messaging)
- Implements the *Publisher-Subscriber* pattern to decouple traffic reception from database saving, protecting the system from load spikes.
- Located in the [messaging/](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/) package.
  - [RabbitMqConfig.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/RabbitMqConfig.java): Initializes the queue infrastructure.
  - [RabbitMqPublisher.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/RabbitMqPublisher.java) / [SqsPublisher.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/SqsPublisher.java): Receives data from the Controller and sends it instantly to the asynchronous queue (RabbitMQ locally, SQS on Cloud via `@Profile("cloud")`).
  - [RabbitMqTrafficMessageListener.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/RabbitMqTrafficMessageListener.java) / [SqsTrafficMessageListener.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/messaging/SqsTrafficMessageListener.java): Listens to the background queue and physically writes data to the database at its own pace.

### 🛡️ Resilience4j (Fault Tolerance)
- Protects the application from external failures by implementing the *Circuit Breaker* pattern.
- The library is integrated via annotations directly in Java Services (e.g., [GeocodingService.java](../server-springboot-maps/src/main/java/edu/ing/unict/springboot/server_springboot_maps/service/GeocodingService.java)), intercepting and handling potential unavailabilities of Google Maps APIs without crashing the backend.

---

## 2. Infrastructure as Code (IaC) and Automation

### 🏗️ Terraform
- Executes hardware *Provisioning* on the Cloud (AWS). Builds immutable infrastructure.
- Modules in [../infrastructure/aws-terraform/](../infrastructure/aws-terraform/).
  - [network.tf](../infrastructure/aws-terraform/network.tf): Creates the Virtual Private Cloud (VPC) and public Subnets.
  - [ec2.tf](../infrastructure/aws-terraform/ec2.tf): Provisions virtual servers (Master and Worker).
  - [rds.tf](../infrastructure/aws-terraform/rds.tf): Creates the managed MySQL relational database.
  - [alb.tf](../infrastructure/aws-terraform/alb.tf): Configures the Application Load Balancer to route internet traffic to EC2 nodes.

### ⚙️ Ansible
- Performs agent-less *Configuration Management*. Connects via SSH into the raw servers created by Terraform and transforms them into a Kubernetes cluster.
- Playbooks in [../infrastructure/ansible/](../infrastructure/ansible/).
  - [inventory.tmpl](../infrastructure/ansible/inventory.tmpl): The server map template, dynamically populated with Terraform IPs.
  - [playbook-k3s.yml](../infrastructure/ansible/playbook-k3s.yml): The playbook that connects to the Master, downloads K3s, extracts the security token, and then connects to the Worker injecting the token to join the cluster.

### 📜 PowerShell / Bash Scripts
- The imperative "glue". Merges Terraform and server configuration into a single "one-click" automated flow.
- Script [deploy_k3s_windows.ps1](../infrastructure/deploy_k3s_windows.ps1) (and Linux variant [deploy_k3s_linux.sh](../infrastructure/deploy_k3s_linux.sh)).
  - Launches Terraform, reads its JSON output to extract IPs, connects via SSH to configure K3s (acting as a Windows workaround for Ansible), injects encrypted Secrets into the cluster, and launches Kubernetes manifests. Slashes release times by eliminating human error.

---

## 3. Container & Orchestration

### 🐳 Docker
- Containerizes the Spring Boot application making it portable and isolated.
- [Dockerfile](../server-springboot-maps/Dockerfile): Uses a *Multi-stage build* (Maven to compile, lightweight JRE to run), ensuring a secure and optimized final image.

### ☸️ Kubernetes (K3s)
- Orchestrates containers ensuring *High Availability* and *Self-Healing*. Abstracts the underlying hardware.
- YAML Manifests in [../infrastructure/k8s/](../infrastructure/k8s/).
  - [app-deployment.yaml](../infrastructure/k8s/app-deployment.yaml): Defines Spring Boot container replicas (Deployment) and how to route incoming traffic from the ALB to Pods (NodePort type Service).
  - [app-configmap.yaml](../infrastructure/k8s/app-configmap.yaml) / [mysql-secret.yaml](../infrastructure/k8s/mysql-secret.yaml): Separates configuration from code (Zero-Trust), injecting environment variables and encrypted passwords without touching the Docker image.

---

## 4. Continuous Integration and Deployment (CI/CD)

### 🐙 GitHub Actions
- Cloud-native automation pipeline. Upon every push, it tests, builds, and releases the new version to production without downtime.
- Workflow [.github/workflows/deploy.yml](../.github/workflows/deploy.yml).
  - Configures the environment (Java), runs Maven tests to verify stability, builds the Docker image, logs into AWS, pushes the image to ECR registry, and finally updates the K3s cluster via SSH (performing a *Rolling Update* of the pods).

---

## 🚀 Getting Started

### 🔴 Phase 1: Local Deployment (Testing and Development)
This phase allows testing the application on your own computer before proceeding with cloud automation.
1. Open the backend project (`server-springboot-maps`) in an IDE.
2. Ensure you have an active local MySQL database (e.g., XAMPP or Docker) and verify credentials in the `application-local.properties` file.
3. Start the Spring Boot application by running the main class `ServerSpringbootMapsApplication.java`.
4. Test APIs via Postman or browser at `http://localhost:8080`.

### 🔵 Phase 2: Cloud Deployment (Automated Production)
This phase automates AWS infrastructure creation and software release via CI/CD.

**Step A: Infrastructure Creation (Terraform + Ansible)**
1. Insert valid AWS credentials in the local `~/.aws/credentials` file.
2. Open the terminal in the `infrastructure/` folder and launch the automation script (inserting password and API key):
   - **On Windows:** `powershell -ExecutionPolicy Bypass -File .\deploy_k3s_windows.ps1 -DbPassword "PASS" -GoogleApiKey "API_KEY"`
   - **On Linux/Mac:** `./deploy_k3s_linux.sh -p "PASS" -g "API_KEY"`
3. Upon completion (about 5 minutes), the script will print the **K3s Master IP** and the **RDS Database URL** in the console.

**Step B: Software Release (GitHub Actions)**
1. Go to the GitHub repository under **Settings > Secrets and variables > Actions**.
2. Update the secrets with the values just returned by the script (`K3S_MASTER_IP`, `SPRING_DATASOURCE_URL`, `K3S_SSH_KEY`) and AWS credentials.
3. Go to the **Actions** tab on GitHub, select the deployment workflow, and click **Run workflow**. 
4. At the end of the pipeline (green light), the application will be online and reachable via the Application Load Balancer DNS address (printed by the script in step 3).

*(After use, to destroy the entire infrastructure and zero costs, execute `terraform destroy -auto-approve` inside the `infrastructure/aws-terraform/` folder).*
