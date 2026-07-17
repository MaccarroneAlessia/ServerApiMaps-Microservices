package edu.ing.unict.springboot.server_springboot_maps.controller;

//import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import edu.ing.unict.springboot.server_springboot_maps.util.ApiKeyManager;

@Controller
public class HomeController {
    
    //@Value("${google.maps.api.key}") // Injects API key from application.properties
    //private String googleMapsApiKey;

    private final ApiKeyManager apiKeyManager; // Declared as final for injection

    // Injects ApiKeyManager via constructor
    public HomeController(ApiKeyManager apiKeyManager) {
        this.apiKeyManager = apiKeyManager;
    }

    @GetMapping("/") // Root URL localhost:8080
    public String home(Model model) {
        // API key
        String googleMapsApiKey = apiKeyManager.getGoogleApiKey();
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "home"; // HTML template
    }

}
