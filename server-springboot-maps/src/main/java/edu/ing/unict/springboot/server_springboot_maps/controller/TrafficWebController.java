package edu.ing.unict.springboot.server_springboot_maps.controller;

import edu.ing.unict.springboot.server_springboot_maps.model.LatLng;
import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import edu.ing.unict.springboot.server_springboot_maps.repository.LatLngRepository;
import edu.ing.unict.springboot.server_springboot_maps.repository.TrafficDataRepository;
import edu.ing.unict.springboot.server_springboot_maps.service.GeocodingService;
import edu.ing.unict.springboot.server_springboot_maps.service.LatLngService;
import edu.ing.unict.springboot.server_springboot_maps.service.TrafficCollectionService; // Useremo il servizio esistente
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

//controller che gestirà le richieste per la pagina web dashboard e invierà i dati al template Thymeleaf
@Controller
public class TrafficWebController {

    //@Autowired
    private final LatLngService latLngService;

    // Iniezione delle dipendenze tramite @Autowired
    //@Autowired
    //private LatLngRepository latLngRepository;

    //@Autowired
    //private TrafficDataRepository trafficDataRepository;

    @Autowired
    private TrafficCollectionService trafficCollectionService; // Servizio per la raccolta traffico

    @Autowired // GeocodingService servizio google maps
    private GeocodingService geocodingService;

    // Formatter per la visualizzazione delle date -> static final per efficienza
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Logger logger = LoggerFactory.getLogger(TrafficCollectionService.class);

    
    TrafficWebController(LatLngService latLngService) {
        this.latLngService = latLngService;
    }

    @PostConstruct
    public void init() {
        //  dati iniziali o  solo l'interfaccia
    }

    // Nuovo metodo per aggiungere località tramite indirizzo
    @PostMapping("/add-location-by-address")
    public String addLocationByAddress(@RequestParam("addressInput") String address, RedirectAttributes redirectAttributes) {
        try {
            GeocodingResult result = geocodingService.geocodeAddress(address);

            if (result != null) {
                // Se il nome è stato lasciato vuoto nel form, usa l'indirizzo formattato da Google
                String finalName = result.formattedAddress;
                Double latitude = result.geometry.location.lat;
                Double longitude = result.geometry.location.lng;

                LatLng newLocation = new LatLng(latitude, longitude, finalName);
                latLngService.addLocation(newLocation);
                redirectAttributes.addAttribute("success", "Località '" + finalName + "' aggiunta con successo!");
            } else {
                redirectAttributes.addAttribute("error", "Indirizzo non trovato: " + address);
            }
        } catch (ApiException e) {
            redirectAttributes.addAttribute("error", "Errore API Google Maps: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            redirectAttributes.addAttribute("error", "Errore di I/O durante la geocodifica: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Ripristina l'interruzione
            redirectAttributes.addAttribute("error", "Operazione interrotta durante la geocodifica.");
            e.printStackTrace();
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Errore generico durante l'aggiunta della località: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/traffic-dashboard";
    }



    /**
     * Gestisce la richiesta GET per la dashboard del traffico.
     * Recupera tutti i dati di traffico e le località e li aggiunge al modello.
     * @param model Il modello per passare i dati al template Thymeleaf.
     * @return Il nome del template Thymeleaf da renderizzare.
     */
    @GetMapping("/traffic-dashboard")
    public String showDashboard(Model model) {
        List<TrafficData> allTrafficData = trafficCollectionService.getAllTrafficData();
        List<LatLng> allLocations = latLngService.getAllLocations();

        model.addAttribute("trafficDataList", allTrafficData);
        model.addAttribute("locations", allLocations);
        model.addAttribute("newLocation", new LatLng()); // Per il form di aggiunta nuova località
        model.addAttribute("dateTimeFormatter", DATE_TIME_FORMATTER); // Per formattare le date
        return "traffic-dashboard"; // Nome del file HTML in src/main/resources/templates
    }

    /**
     * Gestisce la richiesta POST per aggiungere una nuova località.
     * Controlla se la località esiste già prima di salvarla.
     * @param newLocation L'oggetto LatLng inviato dal form.
     * @param redirectAttributes Attributi per messaggi di reindirizzamento.
     * @return Reindirizzamento alla dashboard.
     */
    @PostMapping("/add-location")
    public String addLocation(LatLng newLocation, RedirectAttributes redirectAttributes) {
        // Controlla se una località con la stessa lat/lng esiste già
        Optional<LatLng> existingLocation = latLngService.findByCordinate(newLocation.getLatitude(), newLocation.getLongitude());

        if (existingLocation.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Località con la stessa latitudine e longitudine già esistente!");
        } else {
            // Se il nome non è stato fornito dal form, imposta un nome di default
            latLngService.addLocation(newLocation);
            redirectAttributes.addFlashAttribute("successMessage", "Località aggiunta con successo!");
        }
        return "redirect:/traffic-dashboard";
    }

    /**
     * Gestisce la richiesta POST per controllare il traffico tra due località esistenti.
     * Utilizza il servizio TrafficCollectionService per avviare la raccolta dati.
     * @param originId ID della località di origine.
     * @param destinationId ID della località di destinazione.
     * @param redirectAttributes Attributi per messaggi di reindirizzamento.
     * @return Reindirizzamento alla dashboard.
     */
    @PostMapping("/check-traffic")
    public String checkTraffic(
            @RequestParam("originId") Long originId,
            @RequestParam("destinationId") Long destinationId,
            RedirectAttributes redirectAttributes) {

        Optional<LatLng> originOpt = latLngService.findById(originId);
        Optional<LatLng> destinationOpt = latLngService.findById(destinationId);

        if (originOpt.isEmpty() || destinationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Origine o Destinazione non trovate!");
            return "redirect:/traffic-dashboard";
        }

        LatLng origin = originOpt.get();
        LatLng destination = destinationOpt.get();

        try {
            // Chiama il servizio per raccogliere e salvare i dati del traffico per queste specifiche rotte
            // Nota: Se trafficCollectionService.collectTrafficDataScheduled() non è adatta,
            // potresti dover aggiungere un nuovo metodo a TrafficCollectionService
            // che accetti LatLng origin e LatLng destination.
            CompletableFuture<Void> future = trafficCollectionService.collectTrafficDataForRoute(origin, destination);
            future.get(); // Aspetta il completamento dell'operazione asincrona
            redirectAttributes.addFlashAttribute("successMessage", "Dati traffico raccolti e salvati con successo!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
            logger.error("Errore di interruzione durante la raccolta del traffico: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Operazione interrotta durante la raccolta del traffico: " + e.getMessage());
        } catch (ExecutionException e) {
            logger.error("Errore durante l'esecuzione asincrona della raccolta del traffico: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante la raccolta del traffico: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        } catch (Exception e) {
            logger.error("Errore generico durante la raccolta del traffico: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore generico durante la raccolta del traffico: " + e.getMessage());
        }

        return "redirect:/traffic-dashboard";
    }

    /**
     * Gestisce la richiesta POST per eliminare un dato di traffico.
     * @param id ID del dato traffico da eliminare.
     * @param redirectAttributes Attributi per messaggi di reindirizzamento.
     * @return Reindirizzamento alla dashboard.
     */
    @PostMapping("/delete-traffic-data")
    public String deleteTrafficData(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            trafficCollectionService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Dato traffico eliminato con successo!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Errore nell'eliminazione del dato traffico: " + e.getMessage());
        }
        return "redirect:/traffic-dashboard";
    }

    /**
     * Gestisce la richiesta POST per eliminare una località.
     * Controlla se la località è utilizzata in dati di traffico esistenti prima di eliminarla.
     * @param id ID della località da eliminare.
     * @param redirectAttributes Attributi per messaggi di reindirizzamento.
     * @return Reindirizzamento alla dashboard.
     */
    @PostMapping("/delete-location")
    public String deleteLocation(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            // Prima di eliminare una località, controlla se è usata in TrafficData
            // Questo è un controllo semplice, potresti voler gestire casi più complessi
            // ad esempio, settare a NULL le relazioni in TrafficData o eliminare i TrafficData correlati
            //findByOriginIdOrDestinationId
            List<TrafficData> relatedTrafficData = trafficCollectionService.getTrafficDataByOriginAndDestination(id, id); // ?????
            if (!relatedTrafficData.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Impossibile eliminare la località: è usata in dati di traffico esistenti.");
            } else {
                latLngService.deleteLocation(id);
                redirectAttributes.addFlashAttribute("successMessage", "Località eliminata con successo!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Errore nell'eliminazione della località: " + e.getMessage());
        }
        return "redirect:/traffic-dashboard";
    }

    // Classe interna per mappare la richiesta JSON (se non vuoi creare un file separato)
    static class LatLngRequest {
        private Double latitude;
        private Double longitude;

        //private static final Logger logger = LoggerFactory.getLogger(TrafficCollectionService.class);

        // Getters and setters (Lombok @Data would generate these automatically if used)
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    /**
     * Gestisce la richiesta POST da una chiamata AJAX per ottenere il traffico per un punto specifico.
     * Utilizza il servizio TrafficCollectionService per trovare/creare la località e raccogliere i dati.
     * @param request L'oggetto LatLngRequest contenente latitudine e longitudine.
     * @return Una stringa JSON che indica successo o fallimento.
     */
    @PostMapping("/get-traffic-for-point")
    @ResponseBody // ritorna direttamente il corpo della risposta (JSON)
    public String getTrafficForPoint(@RequestBody LatLngRequest request) {
        try {
            // Trova o crea la LatLng per il punto cliccato
            // stesso punto come origine e destinazione per avere dati "sul" punto
            LatLng clickedLocation = latLngService.findOrCreateLatLng(request.getLatitude(), request.getLongitude());

            // traffico con la stessa località come origine e destinazione
            // qiondi -> traffico "attorno" o "in" quel punto
            CompletableFuture<Void> future = trafficCollectionService.collectTrafficDataForRoute(clickedLocation, clickedLocation);
            future.get(); // Aspetta che la richiesta asincrona sia completata

            return "{\"success\": true, \"message\": \"Dati traffico raccolti per " + clickedLocation.getName() + ".\"}";
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
            logger.error("       ****Errore durante la raccolta del traffico per punto: {}", e.getMessage(), e);
            return "{\"success\": false, \"message\": \"Errore durante la raccolta del traffico: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            logger.error("       ****Errore generico durante la raccolta del traffico per punto: {}", e.getMessage(), e);
            return "{\"success\": false, \"message\": \"Errore generico: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Gestisce la richiesta POST per rinominare una località esistente.
     * Reindirizza alla dashboard con un messaggio di successo o errore.
     */
    @PostMapping("/rename-location") // Questa è l'annotazione chiave!
    public String renameLocation(@RequestParam Long id, @RequestParam String newName, RedirectAttributes redirectAttributes) {
        try {
            // Chiama il metodo renameLocation dal tuo LocationService
            latLngService.renameLocation(id, newName);
            redirectAttributes.addFlashAttribute("successMessage", "Località rinominata con successo!");
        } catch (Exception e) {
            // Gestione generica dell'errore (puoi renderla più specifica)
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante la rinomina della località: " + e.getMessage());
        }
        // Reindirizza sempre alla dashboard dopo l'operazione
        return "redirect:/traffic-dashboard"; // Assicurati che questo sia l'URL corretto per la tua dashboard
    }

}