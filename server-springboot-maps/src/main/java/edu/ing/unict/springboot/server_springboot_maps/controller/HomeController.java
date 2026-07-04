package edu.ing.unict.springboot.server_springboot_maps.controller;

//import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import edu.ing.unict.springboot.server_springboot_maps.util.ApiKeyManager;

@Controller
public class HomeController {
    
    //@Value("${google.maps.api.key}") // Inietta la chiave API dal application.properties
    //private String googleMapsApiKey;

    private final ApiKeyManager apiKeyManager; // Dichiarazione come finale per l'iniezione

    // Inietta ApiKeyManager tramite il costruttore
    public HomeController(ApiKeyManager apiKeyManager) {
        this.apiKeyManager = apiKeyManager;
    }

    @GetMapping("/") // URL radice localhost:8080
    public String home(Model model) {
        // chiave API
        String googleMapsApiKey = apiKeyManager.getGoogleApiKey();
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "home"; // template HTML
    }

}
