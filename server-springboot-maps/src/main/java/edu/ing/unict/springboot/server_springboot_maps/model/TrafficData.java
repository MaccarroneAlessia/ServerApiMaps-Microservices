package edu.ing.unict.springboot.server_springboot_maps.model;
// Entità JPA Principale *

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull; // validazioni a livello di bean
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

    @ManyToOne // Relazione One-to-Many (1 a MOLTI) da LatLng
    // Molti traffic_data possono avere lo stesso LatLng -> posto considerato più volte
    @JoinColumn(name = "origin_location_id", nullable = false) // Colonna FK per l'origine, NOT NULL
    @NotNull // Potresti volere questa validazione a livello di bean per assicurarti che il LatLng non sia null prima di salvare
    private LatLng origin;

    @ManyToOne // Relazione One-to-Many
    @JoinColumn(name = "destination_location_id", nullable = false) // Colonna FK per la destinazione, NOT NULL
    @NotNull // Potresti volere questa validazione a livello di bean
    private LatLng destination;


    private String mode; // ad esempio "driving" se alla guida -> può essere null

    @Embedded
    private RouteDetails routeDetails; // summary, duration, duration_in_traffic

    // Costruttore senza ID per la creazione di nuovi oggetti
    public TrafficData(LocalDateTime timestamp, LatLng originLatLng, LatLng destinationLatLng, String mode, RouteDetails routeDetails) {
        this.timestamp = timestamp;
        this.origin = originLatLng;
        this.destination = destinationLatLng;
        this.mode = mode;
        this.routeDetails = routeDetails;
    }
}
