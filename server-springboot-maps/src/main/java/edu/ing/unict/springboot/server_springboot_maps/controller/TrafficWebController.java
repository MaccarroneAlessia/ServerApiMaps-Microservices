package edu.ing.unict.springboot.server_springboot_maps.controller;

import edu.ing.unict.springboot.server_springboot_maps.model.LatLng;
import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import edu.ing.unict.springboot.server_springboot_maps.repository.LatLngRepository;
import edu.ing.unict.springboot.server_springboot_maps.repository.TrafficDataRepository;
import edu.ing.unict.springboot.server_springboot_maps.service.GeocodingService;
import edu.ing.unict.springboot.server_springboot_maps.service.LatLngService;
import edu.ing.unict.springboot.server_springboot_maps.service.TrafficCollectionService; 
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

// Controller to manage web dashboard requests and forward data to the Thymeleaf template
@Controller
public class TrafficWebController {

    private final LatLngService latLngService;

    @Autowired
    private TrafficCollectionService trafficCollectionService; // Traffic collection service

    @Autowired // Google Maps geocoding service
    private GeocodingService geocodingService;

    // Formatter for date rendering -> static final for efficiency
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Logger logger = LoggerFactory.getLogger(TrafficCollectionService.class);

    
    TrafficWebController(LatLngService latLngService) {
        this.latLngService = latLngService;
    }

    @PostConstruct
    public void init() {
        // Initial setup or interface-only initialization
    }

    // New method to add locations via physical address
    @PostMapping("/add-location-by-address")
    public String addLocationByAddress(@RequestParam("addressInput") String address, RedirectAttributes redirectAttributes) {
        try {
            GeocodingResult result = geocodingService.geocodeAddress(address);

            if (result != null) {
                // If the name field was left blank in the form, fall back to Google's formatted address
                String finalName = result.formattedAddress;
                Double latitude = result.geometry.location.lat;
                Double longitude = result.geometry.location.lng;

                LatLng newLocation = new LatLng(latitude, longitude, finalName);
                latLngService.addLocation(newLocation);
                redirectAttributes.addAttribute("success", "Location '" + finalName + "' added successfully!");
            } else {
                redirectAttributes.addAttribute("error", "Address not found: " + address);
            }
        } catch (ApiException e) {
            redirectAttributes.addAttribute("error", "Google Maps API Error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            redirectAttributes.addAttribute("error", "I/O Error during geocoding: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state
            redirectAttributes.addAttribute("error", "Operation interrupted during geocoding.");
            e.printStackTrace();
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Generic error while adding the location: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/traffic-dashboard";
    }


    /**
     * Handles the GET request for the traffic dashboard.
     * Retrieves all traffic data and locations, adding them to the view model.
     * @param model The model to pass data to the Thymeleaf template.
     * @return The Thymeleaf template name to render.
     */
    @GetMapping("/traffic-dashboard")
    public String showDashboard(Model model) {
        List<TrafficData> allTrafficData = trafficCollectionService.getAllTrafficData();
        List<LatLng> allLocations = latLngService.getAllLocations();

        model.addAttribute("trafficDataList", allTrafficData);
        model.addAttribute("locations", allLocations);
        model.addAttribute("newLocation", new LatLng()); // Backing object for the new location form
        model.addAttribute("dateTimeFormatter", DATE_TIME_FORMATTER); // For date formatting
        return "traffic-dashboard"; // HTML filename in src/main/resources/templates
    }

    /**
     * Handles the POST request to add a new location.
     * Checks if the location already exists before persisting it.
     * @param newLocation The LatLng object submitted from the form.
     * @param redirectAttributes Attributes for redirection messages.
     * @return Redirection to the dashboard.
     */
    @PostMapping("/add-location")
    public String addLocation(LatLng newLocation, RedirectAttributes redirectAttributes) {
        // Verifies if a location with identical lat/lng already exists
        Optional<LatLng> existingLocation = latLngService.findByCordinate(newLocation.getLatitude(), newLocation.getLongitude());

        if (existingLocation.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A location with this latitude and longitude already exists!");
        } else {
            // Adds a default name if omitted by the user
            latLngService.addLocation(newLocation);
            redirectAttributes.addFlashAttribute("successMessage", "Location added successfully!");
        }
        return "redirect:/traffic-dashboard";
    }

    /**
     * Handles the POST request to check traffic between two existing locations.
     * Leverages the TrafficCollectionService to initiate data collection.
     * @param originId ID of the origin location.
     * @param destinationId ID of the destination location.
     * @param redirectAttributes Attributes for redirection messages.
     * @return Redirection to the dashboard.
     */
    @PostMapping("/check-traffic")
    public String checkTraffic(
            @RequestParam("originId") Long originId,
            @RequestParam("destinationId") Long destinationId,
            RedirectAttributes redirectAttributes) {

        Optional<LatLng> originOpt = latLngService.findById(originId);
        Optional<LatLng> destinationOpt = latLngService.findById(destinationId);

        if (originOpt.isEmpty() || destinationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Origin or Destination not found!");
            return "redirect:/traffic-dashboard";
        }

        LatLng origin = originOpt.get();
        LatLng destination = destinationOpt.get();

        try {
            // Calls the service to collect and persist traffic data for this specific route
            CompletableFuture<Void> future = trafficCollectionService.collectTrafficDataForRoute(origin, destination);
            future.get(); // Await asynchronous operation completion
            redirectAttributes.addFlashAttribute("successMessage", "Traffic data collected and saved successfully!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state
            logger.error("Interruption error during traffic collection: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Operation interrupted during traffic collection: " + e.getMessage());
        } catch (ExecutionException e) {
            logger.error("Error during asynchronous traffic collection execution: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error during traffic collection: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        } catch (Exception e) {
            logger.error("Generic error during traffic collection: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Generic error during traffic collection: " + e.getMessage());
        }

        return "redirect:/traffic-dashboard";
    }

    /**
     * Handles the POST request to delete a traffic data entry.
     * @param id ID of the traffic data to delete.
     * @param redirectAttributes Attributes for redirection messages.
     * @return Redirection to the dashboard.
     */
    @PostMapping("/delete-traffic-data")
    public String deleteTrafficData(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            trafficCollectionService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Traffic data deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting traffic data: " + e.getMessage());
        }
        return "redirect:/traffic-dashboard";
    }

    /**
     * Handles the POST request to delete a location.
     * Checks if the location is associated with any existing traffic data prior to deletion.
     * @param id ID of the location to delete.
     * @param redirectAttributes Attributes for redirection messages.
     * @return Redirection to the dashboard.
     */
    @PostMapping("/delete-location")
    public String deleteLocation(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            // Check for dependent TrafficData before removing the location
            // As an alternative, cascade constraints could be handled by setting foreign keys to NULL
            List<TrafficData> relatedTrafficData = trafficCollectionService.getTrafficDataByOriginAndDestination(id, id); 
            if (!relatedTrafficData.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unable to delete location: It is bound to existing traffic data.");
            } else {
                latLngService.deleteLocation(id);
                redirectAttributes.addFlashAttribute("successMessage", "Location deleted successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting location: " + e.getMessage());
        }
        return "redirect:/traffic-dashboard";
    }

    // Inner class mapping the JSON payload
    static class LatLngRequest {
        private Double latitude;
        private Double longitude;

        // Getters and setters 
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    /**
     * Handles the POST request from AJAX calls to get traffic for a specific point.
     * Uses TrafficCollectionService to find/create the location and collect data.
     * @param request The LatLngRequest payload containing latitude and longitude.
     * @return A JSON string indicating success or failure.
     */
    @PostMapping("/get-traffic-for-point")
    @ResponseBody // Maps the return value straight to the HTTP response body (JSON)
    public String getTrafficForPoint(@RequestBody LatLngRequest request) {
        try {
            // Find or create the LatLng for the clicked point
            // By using the same point as origin and destination, we get localized point data
            LatLng clickedLocation = latLngService.findOrCreateLatLng(request.getLatitude(), request.getLongitude());

            // Traffic query using the same location for both ends
            // yielding localized traffic insights around that node
            CompletableFuture<Void> future = trafficCollectionService.collectTrafficDataForRoute(clickedLocation, clickedLocation);
            future.get(); // Await asynchronous request completion

            return "{\"success\": true, \"message\": \"Traffic data collected for " + clickedLocation.getName() + ".\"}";
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state
            logger.error("       ****Error collecting traffic for the specific point: {}", e.getMessage(), e);
            return "{\"success\": false, \"message\": \"Error collecting traffic: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            logger.error("       ****Generic error collecting traffic for the specific point: {}", e.getMessage(), e);
            return "{\"success\": false, \"message\": \"Generic error: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Handles the POST request to rename an existing location.
     * Redirects to the dashboard with a success or error message.
     */
    @PostMapping("/rename-location") 
    public String renameLocation(@RequestParam Long id, @RequestParam String newName, RedirectAttributes redirectAttributes) {
        try {
            // Delegates to the LatLngService rename method
            latLngService.renameLocation(id, newName);
            redirectAttributes.addFlashAttribute("successMessage", "Location renamed successfully!");
        } catch (Exception e) {
            // Generic error handling
            redirectAttributes.addFlashAttribute("errorMessage", "Error renaming location: " + e.getMessage());
        }
        // Consistently redirects to the dashboard after the operation
        return "redirect:/traffic-dashboard";
    }

}