# GitHub Actions (CI/CD Pipelines)

**GitHub Actions** è una piattaforma di automazione nativa di GitHub che permette di creare pipeline di **Continuous Integration (CI)** e **Continuous Deployment (CD)**. Sfrutta macchine virtuali temporanee (runners) per reagire ad eventi del repository (come un `git push` o un `pull request`) per automatizzare compiti ripetitivi come test, compilazione e rilascio.

Lo scopo di questa cartella è garantire che ogni modifica al codice sorgente dell'applicazione venga compilata, verificata, impacchettata in modo sicuro dentro un contenitore Docker e caricata su un repository in Cloud (Amazon ECR) in maniera completamente autonoma.

## 🛠️ Flusso di Esecuzione
All'interno della directory `.github/workflows/` si trovano i file YAML (es. `deploy.yml`) che definiscono le *Actions*. Ecco il flusso di esecuzione:

1. **Trigger:** Il workflow si innesca automaticamente ogni volta che del codice viene mergiato o pushato sul branch `main`.
2. **Checkout e Setup Java:** Il runner di GitHub scarica il codice del repository e installa una specifica versione del JDK (Java Development Kit) necessaria per compilare Spring Boot.
3. **Build dell'Applicazione:** Esegue `mvn clean package` per compilare il codice sorgente in un file JAR e lanciare i test unitari (se presenti) per validare la correttezza della build.
4. **Autenticazione Cloud:** Utilizza delle *GitHub Secrets* per autenticarsi in modo sicuro su Amazon Web Services (AWS CLI/IAM).
5. **Login a ECR:** Si collega al registry privato **Amazon ECR (Elastic Container Registry)**.
6. **Docker Build & Push:** Il runner costruisce l'immagine dell'applicazione usando il **Dockerfile Multi-Stage** del progetto, tagga l'immagine appena creata con uno sha (ID univoco del commit) o come `latest`, e la spinge in Amazon ECR.
7. Da questo momento, i nodi Worker EC2 di Kubernetes potranno scaricare la nuova immagine aggiornata effettuando un rolling-update del deployment.


## 📂 Struttura e File Principali
All'interno della directory `.github/workflows/` risiedono i file YAML che definiscono le *Actions*:
- **`deploy.yml`**: Il file workflow principale. Contiene i vari **Job** e gli **Step** necessari per:
  1. Eseguire il checkout del repository.
  2. Effettuare il setup di Java 17.
  3. Costruire l'app tramite Maven (`mvn package`).
  4. Autenticarsi in sicurezza su AWS via IAM.
  5. Eseguire la `docker build` multi-stage dell'applicazione.
  6. Effettuare il `docker push` sul registry privato (Amazon Elastic Container Registry - ECR).

## 💡 Architettura e Design
1. **GitHub Actions vs Jenkins**: Si è scelto GitHub Actions al posto di server self-hosted (come Jenkins) per azzerare il costo di manutenzione del nodo CI. I runner managed da GitHub sono immediatamente pronti ed evitano la necessità di un'ulteriore istanza EC2 costantemente accesa.
2. **AWS ECR (Elastic Container Registry)**: Utilizziamo un registry privato all'interno dello stesso ambiente AWS (e non ad es. DockerHub) per ridurre la latenza di scaricamento delle immagini sui nodi worker (che risiedono nella medesima region) e per non esporre l'immagine aziendale pubblicamente.
3. **Sicurezza (GitHub Secrets)**: L'autenticazione verso AWS e altri servizi avviene usando *Secrets*, prevenendo totalmente hard-coding di credenziali nei file YAML che sono pubblici.

## 🚀 Comandi per il Deploy
I workflow di GitHub Actions girano in server remoti, quindi non ci sono comandi manuali diretti da lanciare in locale per questa cartella.

Per innescare il processo di CI:
```bash
# 1. Modificare il codice
# 2. Eseguire il commit e il push sul branch associato alla Action (es. main) 
```
A questo punto, la tab **"Actions"** su GitHub mostrerà la pipeline scorrere in tempo reale. Dopodiché i worker Kubernetes effettueranno un rolling update e scaricheranno la nuova immagine dal Cloud.
