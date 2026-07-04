package edu.ing.unict.springboot.server_springboot_maps.api;
import edu.ing.unict.springboot.server_springboot_maps.config.AppConfig;
import edu.ing.unict.springboot.server_springboot_maps.model.DurationInfo;
import edu.ing.unict.springboot.server_springboot_maps.model.RouteDetails;
import edu.ing.unict.springboot.server_springboot_maps.util.ApiKeyManager;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// Componente di Spring che implementa l'interfaccia GoogleMapsRemoteFacade.
// Questo significa che fornisce l'implementazione concreta per le operazioni definite nella facade.
@Component
public class GoogleMapsClient implements GoogleMapsRemoteFacade {
    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsClient.class);
    private final RestTemplate restTemplate;
    private final ApiKeyManager apiKeyManager;
    private final AppConfig appConfig;

    // Costruttore. Spring inietta automaticamente le dipendenze necessarie.
    public GoogleMapsClient(RestTemplate restTemplate, ApiKeyManager apiKeyManager, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.apiKeyManager = apiKeyManager;
        this.appConfig = appConfig;
    }

    /**
     * Recupera i dettagli del percorso dall'API di Google Maps in modo asincrono.
     * Utilizza pattern di resilienza (Circuit Breaker, Retry, Time Limiter)
     * per gestire fallimenti e timeout delle chiamate esterne.
     *
     * @param origin Il punto di partenza del percorso.
     * @param destination Il punto di arrivo del percorso.
     * @return Un CompletableFuture che conterrà i dettagli del percorso (RouteDetails)
     * o un'eccezione in caso di fallimento.
     */
    // Il pattern Circuit Breaker previene che l'applicazione tenti ripetutamente un'operazione fallita.
    // Se il circuito si apre, il controllo passa al metodo fallback (fetchRouteDetailsFallback).
    // Il pattern Retry tenta di rieseguire un'operazione fallita un certo numero di volte.
    // Il Time Limiter impone un limite di tempo all'esecuzione del metodo; se non completa in tempo, viene interrotto.
    // Tutte queste configurazioni sono definite per il nome "googleApi".
    @CircuitBreaker(name = "googleApi", fallbackMethod = "fetchRouteDetailsFallback")
    @Retry(name = "googleApi")
    @TimeLimiter(name = "googleApi")
    @Override // Implementa il metodo definito nell'interfaccia GoogleMapsRemoteFacade
    public CompletableFuture<RouteDetails> fetchRouteDetails(String origin, String destination) {
        // Esegue l'operazione in un thread separato per non bloccare il thread chiamante.
        return CompletableFuture.supplyAsync(() -> {
            // Costruisce l'URL per la chiamata all'API Google Directions.
            // Include origine, destinazione, modalità di guida, tempo di partenza "now" per il traffico in tempo reale e la chiave API.
            String apiUrl = UriComponentsBuilder.fromHttpUrl(appConfig.getGoogleDirectionsApiUrl())
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("mode", "driving")
                    .queryParam("departure_time", "now") // Per traffico in tempo reale
                    .queryParam("key", apiKeyManager.getGoogleApiKey())
                    .toUriString();

            logger.info("       ****Recupero dei dettagli del percorso dall'API di Google: {}", apiUrl);
            try {
                // Esegue la richiesta HTTP GET all'API di Google e mappa la risposta JSON in un JsonNode.
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(apiUrl, JsonNode.class);

                // Controlla se la chiamata HTTP ha avuto successo (codice di stato 2xx) e se il corpo della risposta non è nullo.
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("       ****Estrazione dei dati: {}", apiUrl);
                    JsonNode root = response.getBody();
                    JsonNode routes = root.path("routes");

                    // Verifica se sono stati trovati percorsi.
                    if (routes.isArray() && routes.size() > 0) {
                        JsonNode firstRoute = routes.get(0); // Prende il primo percorso.
                        String summary = firstRoute.path("summary").asText(); // Estrae il riepilogo del percorso.

                        // Estrae le "gambe" (segments) del percorso.
                        JsonNode legs = firstRoute.path("legs");

                        // Verifica se ci sono "gambe" nel percorso.
                        if (legs.isArray() && legs.size() > 0) {
                            JsonNode firstLeg = legs.get(0); // Prende la prima "gamba".

                            // Estrae la durata del percorso senza traffico.
                            JsonNode durationNode = firstLeg.path("duration");
                            DurationInfo duration = null;
                            if (!durationNode.isMissingNode() && !durationNode.isNull()) {
                                duration = new DurationInfo(durationNode.path("text").asText(), durationNode.path("value").asInt());
                            }

                            // Estrae la durata del percorso con traffico in tempo reale.
                            JsonNode durationInTrafficNode = firstLeg.path("duration_in_traffic");
                            DurationInfo durationInTraffic = null;
                            if (!durationInTrafficNode.isMissingNode() && !durationInTrafficNode.isNull()) {
                                durationInTraffic = new DurationInfo(durationInTrafficNode.path("text").asText(), durationInTrafficNode.path("value").asInt());
                            }

                            logger.info("       ****Dettagli del percorso analizzati con successo per  {}: {} (Traffic: {})", summary, duration != null ? duration.getText() : "N/A", durationInTraffic != null ? durationInTraffic.getText() : "N/A");
                            // Restituisce un nuovo oggetto RouteDetails con le informazioni estratte.
                            return new RouteDetails(summary, duration, durationInTraffic);
                        }
                    }
                    logger.warn("       *******La risposta dell'API di Google non conteneva i dati relativi al percorso previsti per {}-{} *****", origin, destination);
                    throw new RuntimeException("Risposta Google API analizzata fallita: nessun dato del percorso.");
                } else {
                    logger.error("       ****** Chiamata Google API fallita con status {}: {}*****", response.getStatusCode(), response.getBody());
                    throw new RuntimeException("Chiamata Google API fallita con status: " + response.getStatusCode());
                }
            } catch (ResourceAccessException e) {
                // Cattura eccezioni relative a problemi di connessione o timeout.
                logger.error("       ****TIMEOUT di connessione o lettura alle Google API per {}-{}: {}*****", origin, destination, e.getMessage());
                throw new RuntimeException("Timeout di connessione/lettura alle Api Google", e);
            } catch (Exception e) {
                // Cattura altre eccezioni generiche durante la chiamata API o il parsing.
                logger.error("       ******Chiamata alle Google API fallita per {}-{}: {}*****", origin, destination, e.getMessage());
                throw new RuntimeException("Chiamata Api Google fallita", e);
            }
        });
    }

    /**
     * Metodo di fallback chiamato da Resilience4j in caso di fallimento della chiamata API.
     *
     * @param origin Il punto di partenza del percorso.
     * @param destination Il punto di arrivo del percorso.
     * @param t La causa del fallimento.
     * @return Un CompletableFuture già completato con un valore nullo, indicando il fallimento.
     */
    public CompletableFuture<RouteDetails> fetchRouteDetailsFallback(String origin, String destination, Throwable t) {
        logger.error("       ******Metodo Fallback attivato per le Google API (origine: {}, destinazione: {}) a causa di: {}. RESTITUZIONE DI DATI NULLI *****.", origin, destination, t.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}
