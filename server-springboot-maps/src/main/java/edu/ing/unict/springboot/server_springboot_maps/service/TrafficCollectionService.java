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

@Service // Framework-managed service class
public class TrafficCollectionService {
    // Logger for informational, warning, and error messages
    private static final Logger logger = LoggerFactory.getLogger(TrafficCollectionService.class);

    // Spring-injected dependencies
    private final TrafficDataProvider trafficDataProvider; // Adapter interface for fetching traffic data (GoogleMapsClient)
    private final AppConfig appConfig; // App configuration
    private final TrafficDataRepository trafficDataRepository; // Repository to persist traffic data

    @Autowired // Injects the custom Executor defined in AppConfig
    @Qualifier("taskExecutor") // "taskExecutor" bean in AppConfig
    private Executor executor; // Thread pool for asynchronous CompletableFuture execution
    
    private final GoogleMapsTrafficAdapter googleMapsTrafficAdapter; // Specific adapter for Google Maps Traffic (implements TrafficDataProvider)

    private final LatLngService locationService;
    private final TrafficMessagePublisher trafficMessagePublisher;

    // Constructor for dependency injection
    // Used by Spring to instantiate the service.
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
     * Retrieves all locations saved in the database.
     * @return A list of TrafficData objects.
     */
    public List<TrafficData> getAllTrafficData() {
        // findAll() is provided by JpaRepository
        return trafficDataRepository.findAll();
    }

    // Scheduled method for data collection
    // fixedDelayString: delay between the end of one execution and the start of the next
    @Scheduled(fixedDelayString = "${traffic.collection.fixedDelayMs}")
    @Transactional // <--- TRANSACTIONAL METHOD
    // Ensures all database operations within this method occur in a single transaction
    public void collectTrafficDataScheduled() {
        logger.info("           --- Starting traffic data collection cycle (scheduled) ---");
        String originStr = appConfig.getTrafficRouteOrigin(); // Retrieves origin string (e.g., "lat,lng") from config
        String destinationStr = appConfig.getTrafficRouteDestination(); // Retrieves destination string from config

        // 1. Parse strings into temporary LatLng DTOs
        // Transient objects (new, not yet managed by JPA)
        LatLng tempOriginLatLng = locationService.parseLatLngFromString(originStr);
        LatLng tempDestinationLatLng = locationService.parseLatLngFromString(destinationStr);

        if (tempOriginLatLng == null || tempDestinationLatLng == null) {
            logger.error("       ****ERROR: Unable to parse origin or destination coordinates. Check application.properties.");
            return; // Halts execution if parsing fails
        }

        // 2. Fetch LatLng entities from the database or create them if they do not exist
        // Verifies locations are persistent in the DB before usage
        // Ensuring LatLng objects are "managed" by JPA
        // and persistent in the DB before associating with TrafficData
        LatLng originLatLng = locationService.findOrCreateLatLng(tempOriginLatLng.getLatitude(), tempOriginLatLng.getLongitude());
        LatLng destinationLatLng = locationService.findOrCreateLatLng(tempDestinationLatLng.getLatitude(), tempDestinationLatLng.getLongitude());

        try {
            // Requests traffic details from Google API asynchronously
            CompletableFuture<RouteDetails> futureRouteDetails = trafficDataProvider.getTrafficDetails(originStr, destinationStr);

            // Awaits CompletableFuture result with a timeout
            // Throws TimeoutException if the timeout expires
            RouteDetails routeDetails = futureRouteDetails.get(appConfig.getApiTimeoutSeconds() + 5, TimeUnit.SECONDS);

            if (routeDetails != null) {
                // Creates a new TrafficData object.
                // Uses 'originLatLng' and 'destinationLatLng'
                // which have been retrieved or created (hence "managed").

                // this.timestamp originLatLng destinationLatLng mode routeDetails
                TrafficData trafficData = new TrafficData(
                        LocalDateTime.now(),
                        originLatLng,        // Uses JPA-managed LatLng entities
                        destinationLatLng,   // Uses JPA-managed LatLng entities
                        "driving",
                        routeDetails
                );
                
                // Dispatches the TrafficData object to the message broker (RabbitMQ/SQS)
                // Instead of direct DB save (will be saved by background Listener)
                trafficMessagePublisher.publishTrafficData(trafficData);
                logger.info("       ****Collection and storage cycle completed successfully for route {}-{}", originStr, destinationStr);
            } else {
                logger.warn("       ****Traffic data is null (probable Circuit Breaker fallback or API error). Skipping storage.");
            }
        } catch (TimeoutException e) {
            logger.error("       ****Traffic collection operation exceeded the CompletableFuture timeout: {}", e.getMessage());
        } catch (Exception e) {
            // Catches any other exceptions during the process
            logger.error("      *****Critical error during traffic collection cycle: {}*****", e.getMessage(), e);
        }
    }

    /**
     * Collects and saves traffic data for a specific route
     * (invoked by web controllers)
     * @param origin The origin location
     * @param destination The destination location
     * @return CompletableFuture<Void> resolving upon operation completion
     */
    @Transactional // Atomic DB operations
    public CompletableFuture<Void> collectTrafficDataForRoute(LatLng origin, LatLng destination) {
        logger.info("       ****Collecting traffic for route: {} ({}) -> {} ({})",
                origin.getName(), locationService.formatLatLng(origin), destination.getName(), locationService.formatLatLng(destination));

        String originCoords = locationService.formatLatLng(origin);
        String destinationCoords = locationService.formatLatLng(destination);

        return googleMapsTrafficAdapter.getTrafficDetails(originCoords, destinationCoords)
            .thenApplyAsync(routeDetails -> {
                if (routeDetails != null) {
                    TrafficData trafficData = new TrafficData(
                            LocalDateTime.now(), 
                            // Uses managed LatLng entities
                            origin,
                            destination,
                            "driving",
                            routeDetails
                    );
                    // Dispatches the TrafficData object to the message broker (RabbitMQ/SQS)
                    trafficMessagePublisher.publishTrafficData(trafficData);
                    logger.info("       ****Traffic data saved for route: {} -> {}", origin.getName(), destination.getName());
                } else {
                    logger.warn("       ****No traffic details obtained for route: {} -> {}", origin.getName(), destination.getName());
                }
                return (Void) null;
            }, executor)
            .exceptionally(ex -> {
                logger.error("       ****Error collecting traffic for route {}-{}: {}",
                        origin.getName(), destination.getName(), ex.getMessage());
                throw new RuntimeException("Error collecting traffic data: " + ex.getMessage(), ex);
            });
    }
    
    public List<TrafficData> getTrafficDataByOriginAndDestination(Long originId, Long destinationId) {
        // Example with Spring Data JPA 
        return trafficDataRepository.findByOriginIdAndDestinationIdOrderByTimestampAsc(originId, destinationId);
    }

    public void delete(Long id) throws Exception{
        trafficDataRepository.deleteById(id);
    }
}
