package edu.ing.unict.springboot.server_springboot_maps.service;

import java.util.List;

import edu.ing.unict.springboot.server_springboot_maps.model.LatLng;
import edu.ing.unict.springboot.server_springboot_maps.repository.LatLngRepository;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional; 

// Invokes methods from the LatLng repository
@Service // Marks this class as a Spring service component
public class LatLngService {
    // Injects the repository to interact with the database
    @Autowired
    private LatLngRepository locationRepository;

    /**
     * Retrieves all locations saved in the database
     * @return A list of LatLng objects (from the locations table)
     */
    public List<LatLng> getAllLocations() {
        // findAll() method provided by JpaRepository
        return locationRepository.findAll();
    }

    /**
     * Saves a new location in the database
     * @param location the LatLng object to save
     * @return the saved object **(with generated ID)**
     */
    public LatLng addLocation(LatLng location) {
        return locationRepository.save(location);
    }

    /**
     * Deletes a location from the database by its ID.
     * @param id The ID of the location to delete
     */
    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    /**
     * Renames an existing location.
     * @param id The ID of the location to rename
     * @param newName The new name for the location
     * @return The updated location, or null if not found
     */
    public LatLng renameLocation(Long id, String newName) {
        Optional<LatLng> optionalLocation = locationRepository.findById(id);
        if (optionalLocation.isPresent()) {
            LatLng location = optionalLocation.get();
            location.setName(newName);
            return locationRepository.save(location); // Saves the updated location
        }
        return null; // Or throw an exception if the location does not exist
    }

    /**
     * Finds a location by its ID.
     * @param id The ID of the location to find.
     * @return An Optional containing the Location if found, otherwise empty.
     */
    public Optional<LatLng> findById(Long id) {
        return locationRepository.findById(id);
    }

    /**
     * Finds a location by its coordinates.
     * @param latitude The latitude.
     * @param longitude The longitude.
     * @return An Optional containing the Location if found, otherwise empty.
     */
    public Optional<LatLng> findByCordinate(Double latitude, Double longitude){
        return locationRepository.findByLatitudeAndLongitude(latitude, longitude);
    }
    
    // Finds or creates a LatLng -> 
    // This method can be annotated with @Transactional to ensure find and save are atomic operations
    public LatLng findOrCreateLatLng(Double latitude, Double longitude) {
        // Looks for an existing LatLng with the same coordinates
        return locationRepository.findByLatitudeAndLongitude(latitude, longitude)
            .orElseGet(() -> {
                // If not found, creates a new LatLng
                String defaultName = "Loc " + String.format("%.4f", latitude) + "," + String.format("%.4f", longitude);
                // Saves the new LatLng in the database
                // This makes it a "managed" entity and assigns an ID
                return locationRepository.save(new LatLng(latitude, longitude, defaultName));
            });
    }

    // Helper method to parse a LatLng from a "lat,lng" string into a transient LatLng object.
    // This LatLng does not have an ID yet and is not managed by JPA
    public LatLng parseLatLngFromString(String latLngString) throws NumberFormatException{
        String[] parts = latLngString.split(",");
        if (parts.length == 2) {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            
            String defaultName = "Configured Loc: " + String.format("%.4f", lat) + "," + String.format("%.4f", lon);
            // Creates a new LatLng with a null ID -> "transient" object.
            return new LatLng(lat, lon, defaultName); 
            // Returns a new LatLng object (not yet managed by JPA)
        }
    
        return null; // Returns null if parsing fails
    }


    // Helper method to format a LatLng object into a "lat,lng" string
    public String formatLatLng(LatLng latLng) {
        return latLng.getLatitude() + "," + latLng.getLongitude();
    }
}
