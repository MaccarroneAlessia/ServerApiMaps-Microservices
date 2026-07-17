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

import jakarta.servlet.http.HttpServletRequest; // Import for HttpServletRequest

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AnalysisController {
    // Inject required services
    @Autowired
    private TrafficCollectionService trafficService;
    @Autowired
    private LatLngService latLngService;

    // Formatter for timestamps to be used in the chart
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Handles the GET request for the analysis page.
     * Prepares the model with the necessary data to populate the location dropdowns.
     * @param model The Spring Model for adding view attributes.
     * @param request The HttpServletRequest to fetch the current URI (for navbar highlighting).
     * @return The Thymeleaf template name for the analysis page.
     */
    @GetMapping("/analysis")
    public String showAnalysisPage(Model model, HttpServletRequest request) {
        // Binds the current URI to the model to handle active link highlighting in the navbar
        model.addAttribute("currentUri", request.getRequestURI());

        // Retrieves all locations and appends them to the model.
        // This is required to populate the origin and destination dropdowns
        // on the analysis page, enabling users to pick a specific route.
        List<LatLng> locations = latLngService.getAllLocations();
        model.addAttribute("locations", locations);

        // Returns the Thymeleaf view name (e.g., analysis.html)
        return "analysis";
    }

    /**
     * Supplies traffic data in JSON format for graphical rendering.
     * This endpoint is invoked via AJAX from the analysis page whenever
     * the user selects an origin and destination.
     * @param originId The ID of the origin location selected by the user.
     * @param destinationId The ID of the destination location selected by the user.
     * @return A JSON string containing traffic data structured for Chart.js.
     */
    @GetMapping("/api/traffic-data-for-chart")
    @ResponseBody // Directs Spring to convert the return value straight into the HTTP body (JSON)
    public String getTrafficDataForChart(@RequestParam Long originId, @RequestParam Long destinationId) {
        // Retrieves traffic data bound to the selected route
        // Data is chronologically ordered for accurate line chart plotting.
        List<TrafficData> data = trafficService.getTrafficDataByOriginAndDestination(originId, destinationId);

        // Jackson ObjectMapper to construct the JSON payload
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode dataArray = mapper.createArrayNode(); // JSON array holding the data points

        // Iterates through the fetched traffic data and formats it for Chart.js
        for (TrafficData td : data) {
            ObjectNode node = mapper.createObjectNode(); // Individual JSON object per data point
            
            // Appends the formatted timestamp mapping to the chart's X-axis
            node.put("timestamp", td.getTimestamp().format(dateTimeFormatter));
            
            // Appends standard duration (in minutes)
            // td.getRouteDetails().getDuration() yields a DurationInfo object
            // DurationInfo.getValue() resolves the duration in seconds.
            node.put("duration", td.getRouteDetails().getDuration().getValue() / 60.0);
            
            // Appends traffic-adjusted duration (in minutes)
            node.put("durationInTraffic", td.getRouteDetails().getDurationInTraffic().getValue() / 60.0);
            
            dataArray.add(node); // Injects the data node into the array
        }

        // Serializes the JSON array into a string
        return dataArray.toString();
    }
    
}
