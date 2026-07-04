package edu.ing.unict.springboot.server_springboot_maps.adapter;

/*
 * un'interfaccia per il fornitore di dati sul traffico e un adapter specifico per Google Maps. Questo disaccoppia il client GoogleMapsClient dal servizio di raccolta, permettendo di cambiare facilmente la sorgente dati in futuro (es. Bing Maps, OpenStreetMap) senza modificare la logica di raccolta.
 */

import edu.ing.unict.springboot.server_springboot_maps.model.RouteDetails;
import java.util.concurrent.CompletableFuture;

import com.google.maps.model.GeocodingResult;

//interfaccia Adapter
public interface TrafficDataProvider {
    CompletableFuture<RouteDetails> getTrafficDetails(String origin, String destination);

    CompletableFuture<GeocodingResult> geocodeAddressAsync(String address);
    CompletableFuture<String> getAddressFromCoordinatesAsync(double latitude, double longitude);
    void shutdown();
}
