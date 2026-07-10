# Backend Application (Spring Boot)


Questa cartella contiene il cuore logico del progetto: un'applicazione a microservizi sviluppata in **Java 17+** e basata sul framework **Spring Boot**. L'applicazione gestisce le operazioni CRUD, l'analisi del traffico e si interfaccia con le API di Google Maps per servizi di geocoding.


Rappresenta il livello applicativo puro del sistema (Software Development). Tutto ciò che riguarda l'infrastruttura, il provisioning hardware o l'orchestrazione cloud è mantenuto rigorosamente all'esterno di questa cartella. 

## 📂 Struttura
- **`src/main/java/`**: Contiene l'intero codice sorgente strutturato secondo il pattern architetturale MVC / 3-Tier (Controller, Service, Repository).
- **`pom.xml`**: Il file di configurazione di **Maven**, che gestisce le dipendenze del progetto (Spring Data JPA, Hibernate, Resilience4j, MySQL Driver, ecc.).
- **`Dockerfile`**: Il file che definisce come compilare e impacchettare l'applicazione in un contenitore Docker in due fasi (Multi-stage build).
- **`application.yml`** (o `application.properties`): Contiene le configurazioni di Spring Boot (variabili lette a runtime tramite env vars, come URL del DB, credenziali e porte).

## 💡 Architettura e Design
1. **Architettura 3-Tier / MVC / 3-Layer**: Il codice è suddiviso in Presentation Layer (API REST), Business Logic (Servizi) e Data Access Layer (JPA).
2. **Resilienza (Resilience4j)**: La comunicazione con i server di Google Maps è intrinsecamente instabile o soggetta a limiti di rate. L'implementazione del pattern **Circuit Breaker** (con eventuali retry/fallback) fa in modo che le richieste in fallimento non saturino i thread di Tomcat provocando il blocco totale del servizio.
3. **Ottimizzazione Docker (Multi-stage & Distroless)**: 
   - La build Maven avviene all'interno di un primo container (builder), evitando di dover installare JDK o Maven sulla macchina host.
   - Il runtime avviene su un'immagine *distroless* (es. `gcr.io/distroless/java17`), priva di shell, package manager o utility di sistema, riducendo drasticamente la superficie di attacco. L'utente che esegue l'app è limitato (`springuser`).
4. **Log come stream di eventi (12-Factor App)**: I log dell'applicazione sono diretti su `STDOUT`/`STDERR`, permettendo a Kubernetes o a Docker di raccoglierli e centralizzarli in modo nativo.

## 🚀 Comandi

### Esecuzione e Test Locali (senza Docker/K8s)
Per testare il codice rapidamente durante lo sviluppo:

```bash
# Entra nella directory del backend
cd server-springboot-maps

# 1. Compilare il progetto (eseguendo i test unitari)
mvn clean install

# 2. Avviare l'applicazione Spring Boot localmente
# NOTA: richiederà un DB MySQL in esecuzione e le chiavi Google settate nell'ambiente
mvn spring-boot:run
```

### Build dell'Immagine Docker
Per compilare l'immagine Docker in locale senza lanciare l'intera automazione K8s:
```bash
docker build -t maps-app:latest .
```

### Esecuzione Docker Standalone
```bash
docker run -p 8080:8080 --env-file ../.env maps-app:latest
```
