package edu.ing.unict.springboot.server_springboot_maps.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@ConfigurationProperties(prefix = "app") // Prefisso per le proprietà custom
@Data // Da Lombok, genera getter/setter/toString/equals/hashCode
public class AppConfig {
    @Value("${app.google.directions.api.url:https://maps.googleapis.com/maps/api/directions/json}")
    private String googleDirectionsApiUrl;
    //private String trafficRouteOrigin; // es. "40.7128,-74.0060" (New York)
    //private String trafficRouteDestination; // es. "34.0522,-118.2437" (Los Angeles)
    //private Integer apiTimeoutSeconds= 10; // TEMPORARY: Initialize directly for testing;
    // Deve essere Integer (oggetto), non int (primitivo)
                                     // perché se non trovato, un Integer può essere null,
                                     // mentre un int causerebbe un'eccezione di mappatura.


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

    // Executor personalizzato per @Async e CompletableFuture
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
 * google.directions.api.url, traffic.route.origin, traffic.route.destination e api.timeout.seconds sotto il prefisso app -> per raggrupparli
 * -> application.properties 
 */
