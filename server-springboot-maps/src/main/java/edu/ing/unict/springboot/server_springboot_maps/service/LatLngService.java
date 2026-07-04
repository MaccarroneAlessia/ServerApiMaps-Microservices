package edu.ing.unict.springboot.server_springboot_maps.service;

import java.util.List;

import edu.ing.unict.springboot.server_springboot_maps.model.LatLng;
import edu.ing.unict.springboot.server_springboot_maps.repository.LatLngRepository;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional; // Per findById

//chiama i metodi della repository di LatLng
@Service // Indica che questa classe è un componente di servizio di Spring
public class LatLngService {
    // Inietta il repository per interagire con il database
    @Autowired
    private LatLngRepository locationRepository;

    /**
     * Recupera tutte le località salvate nel database
     * @return lista di oggetti LatLng (tabella locations)
     */
    public List<LatLng> getAllLocations() {
        // metodo findAll() fornito da JpaRepository
        return locationRepository.findAll();
    }

    /**
     * Salva una nuova località nel database
     * @param LatLng l'oggetto da salvare
     * @return l'oggetto salvato **(con ID generato)**
     */
    public LatLng addLocation(LatLng location) {
        return locationRepository.save(location);
    }

    /**
     * Elimina una località dal database tramite ID.
     * @param id della località da eliminare
     */
    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    /**
     * Rinomina una località se esistente.
     * @param id della località da rinominare
     * @param newName nuovo nome per la località
     * @return località aggiornata, o null se non trovata
     */
    public LatLng renameLocation(Long id, String newName) {
        Optional<LatLng> optionalLocation = locationRepository.findById(id);
        if (optionalLocation.isPresent()) {
            LatLng location = optionalLocation.get();
            location.setName(newName);
            return locationRepository.save(location); // Salva la località aggiornata
        }
        return null; // O lancia un'eccezione se la località non esiste
    }

    /**
     * Trova una località per ID.
     * @param id L'ID della località da trovare.
     * @return Un Optional contenente la Location se trovata, altrimenti vuoto.
     */
    public Optional<LatLng> findById(Long id) {
        return locationRepository.findById(id);
    }

    /**
     * Trova una località per coordinate.
     * @param latitudine e longitudine della località da trovare.
     * @return Un Optional contenente la Location se trovata, altrimenti vuoto.
     */
    public Optional<LatLng> findByCordinate(Double latitude, Double longitude){
        return locationRepository.findByLatitudeAndLongitude(latitude, longitude);
    }
    
    // per trovare o creare un LatLng -> 
    // metodo è @Transactional per garantire che le operazioni di find e save siano atomiche
    //@Transactional // <---  per garantire che save() funzioni correttamente
    public LatLng findOrCreateLatLng(Double latitude, Double longitude) {
        // Cerca un LatLng esistente con le stesse coordinate
        return locationRepository.findByLatitudeAndLongitude(latitude, longitude)
            .orElseGet(() -> {
                // Se non trovato, crea un nuovo LatLng
                String defaultName = "Loc " + String.format("%.4f", latitude) + "," + String.format("%.4f", longitude);
                // Salva il nuovo LatLng nel database
                // Questo lo rende un'entità "managed" e gli assegna un ID
                return locationRepository.save(new LatLng(latitude, longitude, defaultName));
            });
    }

    // Metodo helper per parsare LatLng da stringa -> "lat,lng" in un oggetto LatLng temporaneo.
    // Questo LatLng non ha ancora un ID e non è gestito da JPA
    public LatLng parseLatLngFromString(String latLngString) throws NumberFormatException{
        String[] parts = latLngString.split(",");
        if (parts.length == 2) {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            // default name 
            String defaultName = "Configured Loc: " + String.format("%.4f", lat) + "," + String.format("%.4f", lon);
            // Crea un nuovo LatLng con ID nullo -> oggetto "transient".
            return new LatLng(lat, lon, defaultName); // costruttore con ID null
            // Restituisce un nuovo oggetto LatLng (non ancora gestito da JPA)
        }
    
        return null; // // Restituisce null se il parsing fallisce
    }


    // Metodo helper per formattare un oggetto LatLng in una stringa "lat,lng"
    public String formatLatLng(LatLng latLng) {
        return latLng.getLatitude() + "," + latLng.getLongitude();
    }
}
