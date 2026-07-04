# Stage 1: Build dell'applicazione
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia il pom.xml e scarica le dipendenze
# Questo step sfrutta la cache di Docker: le dipendenze vengono scaricate di nuovo solo se il pom.xml cambia
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia il codice sorgente e compila il progetto
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Creazione dell'immagine di runtime
# Utilizziamo una versione Alpine (molto leggera) con il solo JRE (Java Runtime Environment)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Best Practice di sicurezza: Creare un utente non root per eseguire l'app
RUN addgroup -S springgroup && adduser -S springuser -G springgroup
USER springuser:springgroup

# Copia il file JAR compilato dallo Stage 1
COPY --from=build /app/target/*.jar app.jar

# Esponi la porta su cui l'applicazione resta in ascolto
EXPOSE 8080

# Comando di avvio dell'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]
