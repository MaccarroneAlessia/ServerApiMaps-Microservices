# Backend Application (Spring Boot)


This folder contains the logical core of the project: a microservices application developed in **Java 17+** and based on the **Spring Boot** framework. The application handles CRUD operations, traffic analysis, and interfaces with Google Maps APIs for geocoding services.


It represents the pure application layer of the system (Software Development). Everything related to infrastructure, hardware provisioning, or cloud orchestration is kept strictly outside of this folder. 

## 📂 Structure
- **`src/main/java/`**: Contains the entire source code, structured according to the MVC / 3-Tier architectural pattern (Controller, Service, Repository).
- **`pom.xml`**: The **Maven** configuration file, which manages project dependencies (Spring Data JPA, Hibernate, Resilience4j, MySQL Driver, etc.).
- **`Dockerfile`**: The file defining how to compile and package the application into a Docker container in two phases (Multi-stage build).
- **`application.yml`** (or `application.properties`): Contains Spring Boot configurations (variables read at runtime via env vars, such as DB URL, credentials, and ports).

## 💡 Architecture & Design
1. **3-Tier / MVC / 3-Layer Architecture**: The code is divided into the Presentation Layer (REST APIs), Business Logic (Services), and Data Access Layer (JPA).
2. **Resilience (Resilience4j)**: Communication with Google Maps servers is intrinsically unstable or subject to rate limits. Implementing the **Circuit Breaker** pattern (with retry/fallback mechanisms) ensures that failing requests do not saturate Tomcat threads, preventing a total service halt.
3. **Docker Optimization (Multi-stage & Distroless)**: 
   - The Maven build occurs within an initial container (builder), avoiding the need to install the JDK or Maven on the host machine.
   - The runtime executes on a *distroless* image (e.g., `gcr.io/distroless/java17`), stripped of shells, package managers, or system utilities, drastically reducing the attack surface. The user running the app is restricted (`springuser`).
4. **Logs as event streams (12-Factor App)**: Application logs are directed to `STDOUT`/`STDERR`, allowing Kubernetes or Docker to collect and centralize them natively.

## 🚀 Commands

### Local Execution and Testing (without Docker/K8s)
To quickly test code during development:

```bash
# Enter the backend directory
cd server-springboot-maps

# 1. Compile the project (running unit tests)
mvn clean install

# 2. Start the Spring Boot application locally
# NOTE: will require a running MySQL DB and Google keys set in the environment
mvn spring-boot:run
```

### Docker Image Build
To compile the Docker image locally without launching the entire K8s automation:
```bash
docker build -t maps-app:latest .
```

### Standalone Docker Execution
```bash
docker run -p 8080:8080 --env-file ../.env maps-app:latest
```
