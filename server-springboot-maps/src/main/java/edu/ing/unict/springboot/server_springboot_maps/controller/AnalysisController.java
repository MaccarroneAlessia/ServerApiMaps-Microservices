package edu.ing.unict.springboot.server_springboot_maps.controller;
import edu.ing.unict.springboot.server_springboot_maps.model.*;
import edu.ing.unict.springboot.server_springboot_maps.service.TrafficCollectionService;
import edu.ing.unict.springboot.server_springboot_maps.service.LatLngService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest; // Import per HttpServletRequest

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AnalysisController {
    // Inietta i servizi necessari
    @Autowired
    private TrafficCollectionService trafficService;
    @Autowired
    private LatLngService latLngService;

    // Formatter per le timestamp da usare nel grafico
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gestisce la richiesta GET per la pagina di analisi.
     * Prepara il modello con i dati necessari per popolare i dropdown delle località
     * @param model Il Model di Spring per aggiungere attributi alla vista
     * @param request L'HttpServletRequest per ottenere l'URI corrente (per la navbar)
     * @return Il nome del template Thymeleaf per la pagina di analisi.
     */
    @GetMapping("/analysis")
    public String showAnalysisPage(Model model, HttpServletRequest request) {
        // Aggiunge l'URI corrente al modello per la logica di evidenziazione nella navbar
        model.addAttribute("currentUri", request.getRequestURI());

        // Recupera tutte le località e le aggiunge al modello.
        // Questo è necessario per popolare i dropdown di origine e destinazione
        // nella pagina di analisi, permettendo all'utente di selezionare il percorso.
        List<LatLng> locations = latLngService.getAllLocations();
        model.addAttribute("locations", locations);

        // Restituisce il nome della vista Thymeleaf (es. analysis.html)
        return "analysis";
    }

    /**
     * Fornisce i dati di traffico in formato JSON per la visualizzazione grafica
     * Questo endpoint viene chiamato tramite AJAX dalla pagina di analisi quando
     * l'utente seleziona un'origine e una destinazione.
     * @param originId L'ID della località di origine selezionata dall'utente.
     * @param destinationId L'ID della località di destinazione selezionata dall'utente.
     * @return Una stringa JSON contenente i dati di traffico formattati per Chart.js.
     */
    @GetMapping("/api/traffic-data-for-chart")
    @ResponseBody // Indica a Spring di convertire il valore di ritorno direttamente in corpo HTTP (JSON)
    public String getTrafficDataForChart(@RequestParam Long originId, @RequestParam Long destinationId) {
        // Recupera i dati di traffico specifici per il percorso selezionato
        // I dati sono ordinati per timestamp per una corretta visualizzazione nel grafico a linee.
        List<TrafficData> data = trafficService.getTrafficDataByOriginAndDestination(originId, destinationId);

        // ObjectMapper di Jackson per costruire il JSON
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode dataArray = mapper.createArrayNode(); // Array JSON per contenere gli oggetti dati

        // Itera sui dati di traffico recuperati e li formatta per Chart.js
        for (TrafficData td : data) {
            ObjectNode node = mapper.createObjectNode(); // Un oggetto JSON per ogni punto dati
            
            // Aggiunge la timestamp formattata per l'asse X del grafico
            node.put("timestamp", td.getTimestamp().format(dateTimeFormatter));
            
            // Aggiunge la durata normale (in minuti)
            // td.getRouteDetails().getDuration() restituisce un oggetto DurationInfo
            // DurationInfo.getValue() restituisca la durata in secondi.
            node.put("duration", td.getRouteDetails().getDuration().getValue() / 60.0);
            
            // Aggiunge la durata nel traffico (in minuti)
            // Assumi lo stesso per getDurationInTraffic()
            node.put("durationInTraffic", td.getRouteDetails().getDurationInTraffic().getValue() / 60.0);
            
            dataArray.add(node); // Aggiunge l'oggetto dati all'array
        }

        // Restituisce l'array JSON come stringa
        return dataArray.toString();
    }
    
}
