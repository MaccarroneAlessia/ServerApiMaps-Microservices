package edu.ing.unict.springboot.server_springboot_maps.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@ConfigurationProperties(prefix = "app") // Prefix for custom properties
@Data // From Lombok, generates getters/setters/toString/equals/hashCode
public class AppConfig {
    @Value("${app.google.directions.api.url:https://maps.googleapis.com/maps/api/directions/json}")
    private String googleDirectionsApiUrl;
    //private String trafficRouteOrigin; // e.g., "40.7128,-74.0060" (New York)
    //private String trafficRouteDestination; // e.g., "34.0522,-118.2437" (Los Angeles)
    //private Integer apiTimeoutSeconds= 10; // TEMPORARY: Initialize directly for testing;
    // Must be Integer (object wrapper), not int (primitive)
                                     // because if missing, an Integer can be null,
                                     // whereas an int would cause a mapping exception.


    @Value("${app.traffic.collection.fixedDelayMs:600000}")
    private long trafficCollectionFixedDelayMs;

    //@Value("${google.maps.api.key:...}")
    private String googleMapsApiKey;

    @Value("${app.api.timeout.seconds:30}")
    private int apiTimeoutSeconds;

    @Value("${app.traffic.route.origin:37.5289035,15.1132497}")
    private String trafficRouteOrigin; 

    @Value("${app.traffic.route.destination:37.5242754,15.0710061}")
    private String trafficRouteDestination;

    // ... getters for all properties ...
    public long getTrafficCollectionFixedDelayMs() { return trafficCollectionFixedDelayMs; }
    public String getGoogleMapsApiKey() { return googleMapsApiKey; }
    public int getApiTimeoutSeconds() { return apiTimeoutSeconds; }
    public String getTrafficRouteOrigin() { return trafficRouteOrigin; }
    public String getTrafficRouteDestination() { return trafficRouteDestination; }

    public String getGoogleDirectionsApiUrl() {
        return googleDirectionsApiUrl;
    }

    public void setGoogleDirectionsApiUrl(String directionsApiUrl) {
        this.googleDirectionsApiUrl = directionsApiUrl;
    }

    // Custom executor for @Async and CompletableFuture
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Traffic-");
        executor.initialize();
        return executor;
    }


}

/*
 * google.directions.api.url, traffic.route.origin, traffic.route.destination and api.timeout.seconds under the app prefix -> to group them
 * -> application.properties 
 */
