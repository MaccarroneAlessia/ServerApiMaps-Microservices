package edu.ing.unict.springboot.server_springboot_maps.util;

// Handles the API key
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyManager {
    @Value("${google.maps.api.key}")
    private String googleApiKey;

    public String getGoogleApiKey() {
        return googleApiKey;
    }
}
