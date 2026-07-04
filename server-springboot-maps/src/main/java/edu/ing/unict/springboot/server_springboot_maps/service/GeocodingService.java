package edu.ing.unict.springboot.server_springboot_maps.service;

import edu.ing.unict.springboot.server_springboot_maps.util.ApiKeyManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.errors.ApiException;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class GeocodingService {
    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);

    //@Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    @Autowired
    private ApiKeyManager apiKeyManager;

    private GeoApiContext context;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeocodingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Recupera la chiave API tramite ApiKeyManager
        this.googleMapsApiKey = apiKeyManager.getGoogleApiKey();

        // Inizializza il contesto dell'API di Google Maps solo una volta
        // Se la chiave API non è valida, il contesto sarà nullo

        if (googleMapsApiKey != null && !googleMapsApiKey.isEmpty()) {
            context = new GeoApiContext.Builder()
                    .apiKey(googleMapsApiKey)
                    .build();
        } else {
            System.err.println("        *****AVVISO: Chiave API di Google Maps non configurata! La geocodifica non funzionerà...");
        }
    }

    public GeocodingResult geocodeAddress(String address) throws IOException, InterruptedException, ApiException {
        if (context == null) {
            throw new IllegalStateException("Google Maps API context non inizializzato. Controlla la tua chiave API.");
        }
        GeocodingResult[] results = GeocodingApi.newRequest(context).address(address).await();
        if (results != null && results.length > 0) {
            return results[0]; // Restituisce il primo risultato più rilevante
        }
        return null; // Nessun risultato trovato
    }

    /**
     * Performs reverse geocoding to get the address for given latitude and longitude.
     * @param latitude The latitude.
     * @param longitude The longitude.
     * @return The formatted address string, or null if not found/error.
     */
    public String getAddressFromCoordinates(double latitude, double longitude) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", latitude + "," + longitude)
                .queryParam("key", googleMapsApiKey)
                .toUriString();

        try {
            logger.info("Calling Google Geocoding API: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            logger.debug("Geocoding API response: {}", response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode status = root.path("status");

            if ("OK".equals(status.asText())) {
                JsonNode results = root.path("results");
                if (results.isArray() && results.size() > 0) {
                    // Get the first result's formatted_address
                    String address = results.get(0).path("formatted_address").asText();
                    logger.info("Found address for ({},{}): {}", latitude, longitude, address);
                    return address;
                }
            } else {
                String errorMessage = root.path("error_message").asText();
                logger.warn("Geocoding API error for ({},{}): Status: {}, Message: {}", latitude, longitude, status.asText(), errorMessage);
            }
        } catch (Exception e) {
            logger.error("Error during reverse geocoding for ({},{}): {}", latitude, longitude, e.getMessage());
        }
        return null; // Return null if address not found or an error occurs
    }

    // per chiudere il contesto quando l'applicazione si spegne
    // anche se per Spring Boot perché il contesto è gestito come un bean
    public void shutdown() {
        if (context != null) {
            context.shutdown();
        }
    }
    
}
