# Kubernetes (K8s / K3s) Manifests

**Kubernetes (K8s)** is the open-source de facto standard for the automation, deployment, and operational management of containerized applications. This project utilizes an extremely lightweight and optimized derivative named **K3s** (Rancher). This folder is the repository for the so-called "Manifests", which are YAML files that declaratively describe how the runtime ecosystem should be modeled.

The runtime software infrastructure is defined here. Instead of launching individual containers in isolation (as one would via the Docker CLI), the configurations instruct the Kubernetes control plane on how many pods we want, which ports to expose them on, how they communicate with each other, and how to inject configuration. Kubernetes will then ensure to maintain the "desired state", autonomously replacing any failing pods via self-healing.

## 📂 Structure

The directory houses the YAML Manifests to be applied:
- **`app-deployment.yaml`**: Describes the Spring Boot Application instance. It establishes from which repository to download the Docker image, what resource limits (CPU/RAM) to impose, and the replica count. It also includes the associated Service (`NodePort`) for exposure.
- **`mysql-deployment.yaml`**: Defines the local MySQL database infrastructure in the event that managed AWS RDS is not being used. It includes the Deployment, Service, and Persistent Volume declaration.
- **`app-configmap.yaml`**: Contains the map of non-sensitive environment variables (e.g., the connection URL).
- **`*-secret.yaml`**: (Note: intentionally ignored via `.gitignore`). Files encapsulating sensitive data such as API keys or script-generated credentials.
- **`cloud/`**: Folder designated to host Kubernetes manifests specifically tailored for the cloud environment (AWS).

## 💡 Architecture & Design

1. **NodePort Exposure**: The application pods are exposed using a high static NodePort (e.g., `30080`) at the EC2 instance level. Subsequently, the Application Load Balancer (ALB) provisioned by Terraform steps in, hooking onto this port by intercepting and routing inbound traffic toward the cluster nodes.
2. **Configuration Isolation**: Cryptographic keys (Google Maps) and MySQL database passwords are isolated in the Secret layer, strictly separated from the Docker image. The `mysql-secret.yaml` file remains excluded from version control.
3. **Environmental Decoupling**: The manifests are environment-agnostic. The application can be seamlessly applied whether on a local Docker Desktop or the remote AWS ecosystem without needing to overhaul the deployment base.

## 🚀 Deployment Commands

The root PowerShell script takes care of autonomously injecting these manifests into AWS.
If you prefer to launch the cluster in a local environment (e.g., on Docker Desktop):

```bash
cd infrastructure/k8s

# Apply all manifests in bulk
kubectl apply -f .

# Check Pod status
kubectl get pods

# Delete all created resources
kubectl delete -f .
```
