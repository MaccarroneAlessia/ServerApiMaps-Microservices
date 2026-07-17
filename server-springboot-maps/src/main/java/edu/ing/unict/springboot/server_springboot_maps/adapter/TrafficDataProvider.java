package edu.ing.unict.springboot.server_springboot_maps.adapter;

/*
 * An interface for the traffic data provider and a specific adapter for Google Maps. This decouples the GoogleMapsClient from the collection service, making it easy to swap the data source in the future (e.g., Bing Maps, OpenStreetMap) without modifying the collection logic.
 */

import edu.ing.unict.springboot.server_springboot_maps.model.RouteDetails;
import java.util.concurrent.CompletableFuture;

import com.google.maps.model.GeocodingResult;

// Adapter interface
public interface TrafficDataProvider {
    CompletableFuture<RouteDetails> getTrafficDetails(String origin, String destination);

    CompletableFuture<GeocodingResult> geocodeAddressAsync(String address);
    CompletableFuture<String> getAddressFromCoordinatesAsync(double latitude, double longitude);
    void shutdown();
}
