package edu.ing.unict.springboot.server_springboot_maps.model;
// Main JPA Entity *

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull; // Bean-level validations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_data")
@Data // From Lombok
@NoArgsConstructor
@AllArgsConstructor
public class TrafficData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDateTime timestamp;

    @ManyToOne // One-to-Many relationship from LatLng
    // Many traffic_data entries can map to the same LatLng -> location polled multiple times
    @JoinColumn(name = "origin_location_id", nullable = false) // FK column for origin, NOT NULL
    @NotNull // Bean-level validation to ensure LatLng is not null before saving
    private LatLng origin;

    @ManyToOne // One-to-Many relationship
    @JoinColumn(name = "destination_location_id", nullable = false) // FK column for destination, NOT NULL
    @NotNull // Bean-level validation
    private LatLng destination;


    private String mode; // e.g., "driving" if operating a vehicle -> can be null

    @Embedded
    private RouteDetails routeDetails; // summary, duration, duration_in_traffic

    // Constructor without ID for creating new entities
    public TrafficData(LocalDateTime timestamp, LatLng originLatLng, LatLng destinationLatLng, String mode, RouteDetails routeDetails) {
        this.timestamp = timestamp;
        this.origin = originLatLng;
        this.destination = destinationLatLng;
        this.mode = mode;
        this.routeDetails = routeDetails;
    }
}
