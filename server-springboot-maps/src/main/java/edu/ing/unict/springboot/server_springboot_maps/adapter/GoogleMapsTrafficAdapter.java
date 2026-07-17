package edu.ing.unict.springboot.server_springboot_maps.adapter;

import edu.ing.unict.springboot.server_springboot_maps.api.GoogleMapsRemoteFacade;
import edu.ing.unict.springboot.server_springboot_maps.model.RouteDetails;
import org.springframework.stereotype.Component;

import edu.ing.unict.springboot.server_springboot_maps.service.GeocodingService;
import com.google.maps.model.GeocodingResult;
import com.google.maps.errors.ApiException;


import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Adapter Implementation
@Component
public class GoogleMapsTrafficAdapter implements TrafficDataProvider {
    private final GoogleMapsRemoteFacade googleMapsRemoteFacade;
    private final GeocodingService geocodingService;
    // Thread pool to execute geocoding calls asynchronously
    private final ExecutorService executorService = Executors.newCachedThreadPool();


    public GoogleMapsTrafficAdapter(GoogleMapsRemoteFacade googleMapsRemoteFacade, GeocodingService geocodingService) {
        this.googleMapsRemoteFacade = googleMapsRemoteFacade;
        this.geocodingService = geocodingService;
    }

    @Override
    public CompletableFuture<RouteDetails> getTrafficDetails(String origin, String destination) {
        // The adapter delegates the call to the specific client via the facade
        return googleMapsRemoteFacade.fetchRouteDetails(origin, destination);
    }

    /**
     * Geocodes an address asynchronously.
     *
     * @param address The address to geocode.
     * @return A CompletableFuture containing the GeocodingResult or an exception.
     */
    public CompletableFuture<GeocodingResult> geocodeAddressAsync(String address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return geocodingService.geocodeAddress(address);
            } catch (IOException | InterruptedException | ApiException e) {
                // Wraps the checked exception into a runtime exception for the CompletableFuture
                throw new RuntimeException("Error during address geocoding: " + address, e);
            }
        }, executorService); // Executes the task on the executorService
    }

    /**
     * Performs reverse geocoding to retrieve an address from coordinates asynchronously.
     *
     * @param latitude The latitude.
     * @param longitude The longitude.
     * @return A CompletableFuture containing the formatted address or null.
     */
    public CompletableFuture<String> getAddressFromCoordinatesAsync(double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return geocodingService.getAddressFromCoordinates(latitude, longitude);
            } catch (Exception e) {
                // Logs the error and returns null or throws an appropriate exception
                System.err.println("Error during reverse geocoding for coordinates (" + latitude + "," + longitude + "): " + e.getMessage());
                return null; 
            }
        }, executorService); // Executes the task on the executorService
    }

    // Gracefully shuts down the executor service 
    public void shutdown() {
        executorService.shutdown();
    }
}
