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

// Spring component implementing the GoogleMapsRemoteFacade interface.
// Provides the concrete implementation for the operations defined in the facade.
@Component
public class GoogleMapsClient implements GoogleMapsRemoteFacade {
    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsClient.class);
    private final RestTemplate restTemplate;
    private final ApiKeyManager apiKeyManager;
    private final AppConfig appConfig;

    // Constructor. Spring automatically injects the required dependencies.
    public GoogleMapsClient(RestTemplate restTemplate, ApiKeyManager apiKeyManager, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.apiKeyManager = apiKeyManager;
        this.appConfig = appConfig;
    }

    /**
     * Fetches route details from the Google Maps API asynchronously.
     * Uses resilience patterns (Circuit Breaker, Retry, Time Limiter)
     * to manage failures and timeouts of external calls.
     *
     * @param origin The starting point of the route.
     * @param destination The destination of the route.
     * @return A CompletableFuture containing the RouteDetails 
     * or an exception in case of failure.
     */
    // The Circuit Breaker pattern prevents the application from repeatedly attempting a failing operation.
    // If the circuit opens, control is passed to the fallback method (fetchRouteDetailsFallback).
    // The Retry pattern attempts to re-execute a failed operation a specified number of times.
    // The Time Limiter enforces a time bound on the method execution; if it does not complete in time, it is interrupted.
    // All these configurations are bound to the "googleApi" instance.
    @CircuitBreaker(name = "googleApi", fallbackMethod = "fetchRouteDetailsFallback")
    @Retry(name = "googleApi")
    @TimeLimiter(name = "googleApi")
    @Override // Implements the method defined in the GoogleMapsRemoteFacade interface
    public CompletableFuture<RouteDetails> fetchRouteDetails(String origin, String destination) {
        // Executes the operation in a separate thread to avoid blocking the caller.
        return CompletableFuture.supplyAsync(() -> {
            // Builds the URL for the Google Directions API call.
            // Includes origin, destination, driving mode, departure time "now" for real-time traffic, and the API key.
            String apiUrl = UriComponentsBuilder.fromHttpUrl(appConfig.getGoogleDirectionsApiUrl())
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("mode", "driving")
                    .queryParam("departure_time", "now") // For real-time traffic
                    .queryParam("key", apiKeyManager.getGoogleApiKey())
                    .toUriString();

            logger.info("       ****Fetching route details from Google API: {}", apiUrl);
            try {
                // Executes the HTTP GET request to the Google API and maps the JSON response into a JsonNode.
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(apiUrl, JsonNode.class);

                // Checks if the HTTP call was successful (2xx status code) and the response body is not null.
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("       ****Extracting data: {}", apiUrl);
                    JsonNode root = response.getBody();
                    JsonNode routes = root.path("routes");

                    // Verifies if routes were found.
                    if (routes.isArray() && routes.size() > 0) {
                        JsonNode firstRoute = routes.get(0); // Takes the first route.
                        String summary = firstRoute.path("summary").asText(); // Extracts the route summary.

                        // Extracts the route legs.
                        JsonNode legs = firstRoute.path("legs");

                        // Verifies if there are legs in the route.
                        if (legs.isArray() && legs.size() > 0) {
                            JsonNode firstLeg = legs.get(0); // Takes the first leg.

                            // Extracts the route duration without traffic.
                            JsonNode durationNode = firstLeg.path("duration");
                            DurationInfo duration = null;
                            if (!durationNode.isMissingNode() && !durationNode.isNull()) {
                                duration = new DurationInfo(durationNode.path("text").asText(), durationNode.path("value").asInt());
                            }

                            // Extracts the route duration with real-time traffic.
                            JsonNode durationInTrafficNode = firstLeg.path("duration_in_traffic");
                            DurationInfo durationInTraffic = null;
                            if (!durationInTrafficNode.isMissingNode() && !durationInTrafficNode.isNull()) {
                                durationInTraffic = new DurationInfo(durationInTrafficNode.path("text").asText(), durationInTrafficNode.path("value").asInt());
                            }

                            logger.info("       ****Route details successfully parsed for {}: {} (Traffic: {})", summary, duration != null ? duration.getText() : "N/A", durationInTraffic != null ? durationInTraffic.getText() : "N/A");
                            // Returns a new RouteDetails object with the extracted information.
                            return new RouteDetails(summary, duration, durationInTraffic);
                        }
                    }
                    logger.warn("       *******Google API response did not contain expected route data for {}-{} *****", origin, destination);
                    throw new RuntimeException("Google API response parsing failed: no route data.");
                } else {
                    logger.error("       ****** Google API call failed with status {}: {}*****", response.getStatusCode(), response.getBody());
                    throw new RuntimeException("Google API call failed with status: " + response.getStatusCode());
                }
            } catch (ResourceAccessException e) {
                // Catches exceptions related to connection issues or timeouts.
                logger.error("       ****Google API connection or read TIMEOUT for {}-{}: {}*****", origin, destination, e.getMessage());
                throw new RuntimeException("Google API connection/read timeout", e);
            } catch (Exception e) {
                // Catches any other generic exceptions during the API call or parsing.
                logger.error("       ******Google API call failed for {}-{}: {}*****", origin, destination, e.getMessage());
                throw new RuntimeException("Google API call failed", e);
            }
        });
    }

    /**
     * Fallback method invoked by Resilience4j upon API call failure.
     *
     * @param origin The starting point of the route.
     * @param destination The destination of the route.
     * @param t The cause of the failure.
     * @return A CompletableFuture already completed with a null value, indicating failure.
     */
    public CompletableFuture<RouteDetails> fetchRouteDetailsFallback(String origin, String destination, Throwable t) {
        logger.error("       ******Fallback method triggered for Google API (origin: {}, destination: {}) due to: {}. RETURNING NULL DATA *****.", origin, destination, t.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}
