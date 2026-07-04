package edu.ing.unict.springboot.server_springboot_maps.service;

import edu.ing.unict.springboot.server_springboot_maps.adapter.GoogleMapsTrafficAdapter;
import edu.ing.unict.springboot.server_springboot_maps.adapter.TrafficDataProvider;
import edu.ing.unict.springboot.server_springboot_maps.config.AppConfig;
import edu.ing.unict.springboot.server_springboot_maps.model.LatLng;
import edu.ing.unict.springboot.server_springboot_maps.model.RouteDetails;
import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import edu.ing.unict.springboot.server_springboot_maps.service.LatLngService;
import edu.ing.unict.springboot.server_springboot_maps.repository.TrafficDataRepository;
import edu.ing.unict.springboot.server_springboot_maps.messaging.TrafficMessagePublisher;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service // classe di servizio gestita dal framework
public class TrafficCollectionService {
    // Logger per registrare messaggi informativi, di avviso ed errore
    private static final Logger logger = LoggerFactory.getLogger(TrafficCollectionService.class);

    // Dipendenze iniettate da Spring
    private final TrafficDataProvider trafficDataProvider; // interfaccia Adapter per ottenere i dati di traffico (GoogleMapsClient)
    private final AppConfig appConfig; // configurazione dell'app
    private final TrafficDataRepository trafficDataRepository; // Repository per salvare i dati di traffico

    @Autowired // Inietta l'Executor personalizzato definito in AppConfig
    @Qualifier("taskExecutor") // bean "taskExecutor" in AppConfig
    private Executor executor; // Pool di thread per l'esecuzione asincrona dei CompletableFuture
    
    private final GoogleMapsTrafficAdapter googleMapsTrafficAdapter;// Adapter specifico per Google Maps Traffic (implementa TrafficDataProvider)

    private final LatLngService locationService;
    private final TrafficMessagePublisher trafficMessagePublisher;

    // Costruttore per l'iniezione delle dipendenze
    // Spring lo usa per creare l'istanza del servizio.
    public TrafficCollectionService(GoogleMapsTrafficAdapter googleMapsTrafficAdapter, TrafficDataProvider trafficDataProvider, AppConfig appConfig,
                                    TrafficDataRepository trafficDataRepository, LatLngService latLngservice, TrafficMessagePublisher trafficMessagePublisher) {
        this.trafficDataProvider = trafficDataProvider;
        this.appConfig = appConfig;
        this.trafficDataRepository = trafficDataRepository;
        this.locationService = latLngservice;
        this.googleMapsTrafficAdapter = googleMapsTrafficAdapter;
        this.trafficMessagePublisher = trafficMessagePublisher;
    }

    /**
     * Recupera tutte le località salvate nel database.
     * @return Una lista di oggetti Location.
     */
    public List<TrafficData> getAllTrafficData() {
        // Il metodo findAll() è fornito da JpaRepository
        return trafficDataRepository.findAll();
    }

    // Metodo schedulato per la raccolta dati
    // fixedDelayString: l'intervallo tra la fine di un'esecuzione e l'inizio della successiva
    @Scheduled(fixedDelayString = "${traffic.collection.fixedDelayMs}")
    @Transactional // <--- METODO TRANSAZIONALE
    //Assicura che tutte le operazioni di database in questo metodo avvengano in una singola transazione
    public void collectTrafficDataScheduled() {
        logger.info("           --- Avvio ciclo di raccolta dati sul traffico  (schedulato)---");
        String originStr = appConfig.getTrafficRouteOrigin(); // Ottiene la stringa di origine (es. "lat,lng") dalla configurazione
        String destinationStr = appConfig.getTrafficRouteDestination(); // Ottiene la stringa di destinazione dalla configurazione

        // 1. Parsa le stringhe in LatLng DTO temporanei
        // oggetti "transient" (nuovi, non ancora gestiti da JPA)
        LatLng tempOriginLatLng = locationService.parseLatLngFromString(originStr); // Implementa o recupera dal DB
        LatLng tempDestinationLatLng = locationService.parseLatLngFromString(destinationStr);

        if (tempOriginLatLng == null || tempDestinationLatLng == null) {
            logger.error("       ****ERRORE : Impossibile parsare le coordinate di origine o destinazione. Controlla application.properties.");
            return; // Interrompe l'esecuzione se il parsing fallisce
        }

        // 2. Ottenere le entità LatLng dal database o crearle se non esistono
        // controllo che le località siano persistenti nel DB prima di usarle
        // le LatLng usate siano quindi "managed" (gestite da JPA)
        // e persistenti nel DB prima di essere associate a TrafficData
        LatLng originLatLng = locationService.findOrCreateLatLng(tempOriginLatLng.getLatitude(), tempOriginLatLng.getLongitude());
        LatLng destinationLatLng = locationService.findOrCreateLatLng(tempDestinationLatLng.getLatitude(), tempDestinationLatLng.getLongitude());

        try {
            // Richiede i dettagli del traffico all'API di Google in modo asincrono
            CompletableFuture<RouteDetails> futureRouteDetails = trafficDataProvider.getTrafficDetails(originStr, destinationStr);

            // Attende il risultato del CompletableFuture con un timeout
            // Se il timeout scade, viene lanciata una TimeoutException
            RouteDetails routeDetails = futureRouteDetails.get(appConfig.getApiTimeoutSeconds() + 5, TimeUnit.SECONDS);

            if (routeDetails != null) {
                // Crea un nuovo oggetto TrafficData.
                // si usano gli oggetti 'originLatLng' e 'destinationLatLng'
                // che sono stati recuperati o creati (quindi sono "managed").

                //this.timestamp originLatLng destinationLatLng mode routeDetails
                TrafficData trafficData = new TrafficData(
                        LocalDateTime.now(),
                        originLatLng,        // Usa le entità LatLng gestite da JPA
                        destinationLatLng,   // Usa le entità LatLng gestite da JPA
                        "driving",
                        routeDetails
                );
                
                // Salva l'oggetto TrafficData nel DB

                // Invia l'oggetto TrafficData al message broker (RabbitMQ/SQS)
                // Invece di salvare direttamente nel DB (sarà salvato dal Listener in background)
                trafficMessagePublisher.publishTrafficData(trafficData);
                logger.info("       ****Ciclo di raccolta e memorizzazione completato con successo per rotta {}-{}", originStr, destinationStr);
            } else {
                logger.warn("       ****Dati traffico nulli (probabile fallback di Circuit Breaker o errore API). Saltando la memorizzazione.");
            }
        } catch (TimeoutException e) {
            logger.error("       ****L'operazione di raccolta traffico ha superato il timeout di attesa del CompletableFuture: {}", e.getMessage());
        } catch (Exception e) {
            // Cattura qualsiasi altra eccezione durante il processo
            logger.error("      *****Errore critico durante il ciclo di raccolta traffico: {}*****", e.getMessage(), e);
        }
    }

    

    /**
     * raccoglie e salva i dati del traffico per una specifica rotta
     * (chiamato dai controller web)
     * @param origin La località di origine
     * @param destination La località di destinazione
     * @return CompletableFuture<Void> che si completa quando l'operazione è finita
     */
    @Transactional // operazioni DB atomiche
    public CompletableFuture<Void> collectTrafficDataForRoute(LatLng origin, LatLng destination) {
        logger.info("       ****Raccolta traffico per rotta: {} ({}) -> {} ({})",
                origin.getName(), locationService.formatLatLng(origin), destination.getName(), locationService.formatLatLng(destination));

        String originCoords = locationService.formatLatLng(origin);
        String destinationCoords = locationService.formatLatLng(destination);

        return googleMapsTrafficAdapter.getTrafficDetails(originCoords, destinationCoords)
            .thenApplyAsync(routeDetails -> {
                if (routeDetails != null) {
                    TrafficData trafficData = new TrafficData(
                            LocalDateTime.now(), 
                            // Usa le entità LatLng gestite
                            origin,
                            destination,
                            "driving",
                            routeDetails
                    );
                    // Invia l'oggetto TrafficData al message broker (RabbitMQ/SQS)
                    trafficMessagePublisher.publishTrafficData(trafficData);
                    logger.info("       ****Dato traffico salvato per rotta: {} -> {}", origin.getName(), destination.getName());
                } else {
                    logger.warn("       ****Nessun dettaglio traffico ottenuto per rotta: {} -> {}", origin.getName(), destination.getName());
                }
                return (Void) null;
            }, executor)
            .exceptionally(ex -> {
                logger.error("       ****Errore nella raccolta del traffico per rotta {}-{}: {}",
                        origin.getName(), destination.getName(), ex.getMessage());
                throw new RuntimeException("Errore nella raccolta del traffico: " + ex.getMessage(), ex);
            });
    }

    
    public List<TrafficData> getTrafficDataByOriginAndDestination(Long originId, Long destinationId) {
        // Implementa la logica per recuperare i dati dal tuo database
        // Esempio con Spring Data JPA (dovrai definire il metodo nel tuo TrafficDataRepository)
        return trafficDataRepository.findByOriginIdAndDestinationIdOrderByTimestampAsc(originId, destinationId);
    }

    public void delete(Long id) throws Exception{
        trafficDataRepository.deleteById(id);
    }
}
