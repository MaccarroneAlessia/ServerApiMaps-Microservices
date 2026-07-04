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

// Implementazione Adapter
@Component
public class GoogleMapsTrafficAdapter implements TrafficDataProvider {
    private final GoogleMapsRemoteFacade googleMapsRemoteFacade;
    private final GeocodingService geocodingService;
    // per eseguire le chiamate di geocoding in modo asincrono
    private final ExecutorService executorService = Executors.newCachedThreadPool();


    public GoogleMapsTrafficAdapter(GoogleMapsRemoteFacade googleMapsRemoteFacade, GeocodingService geocodingService) {
    this.googleMapsRemoteFacade = googleMapsRemoteFacade;
    this.geocodingService = geocodingService;
}

    @Override
    public CompletableFuture<RouteDetails> getTrafficDetails(String origin, String destination) {
        // L'adapter delega semplicemente la chiamata al client specifico, ora tramite la facade
        return googleMapsRemoteFacade.fetchRouteDetails(origin, destination);
    }

    /**
     * Esegue la geocodifica di un indirizzo in modo asincrono.
     *
     * @param address L'indirizzo da geocodificare.
     * @return Un CompletableFuture che conterrà il GeocodingResult o un'eccezione.
     */
    public CompletableFuture<GeocodingResult> geocodeAddressAsync(String address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return geocodingService.geocodeAddress(address);
            } catch (IOException | InterruptedException | ApiException e) {
                // Avvolge l'eccezione in un'eccezione di runtime per il CompletableFuture
                throw new RuntimeException("Errore durante la geocodifica dell'indirizzo: " + address, e);
            }
        }, executorService); // Esegue il task sull'executorService
    }

    /**
     * Esegue la geocodifica inversa per ottenere l'indirizzo da coordinate in modo asincrono.
     *
     * @param latitude La latitudine.
     * @param longitude La longitudine.
     * @return Un CompletableFuture che conterrà l'indirizzo formattato o null.
     */
    public CompletableFuture<String> getAddressFromCoordinatesAsync(double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return geocodingService.getAddressFromCoordinates(latitude, longitude);
            } catch (Exception e) {
                // Logga l'errore e restituisce null o rilancia un'eccezione appropriata
                System.err.println("Errore durante la geocodifica inversa per coordinate (" + latitude + "," + longitude + "): " + e.getMessage());
                return null; // O throw new RuntimeException("...", e);
            }
        }, executorService); // Esegue il task sull'executorService
    }

    //  per lo shutdown dell'executor service 
    public void shutdown() {
        executorService.shutdown();
    }
}
