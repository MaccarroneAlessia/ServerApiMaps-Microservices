package edu.ing.unict.springboot.server_springboot_maps.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Embeddable for latitude and longitude
//@Embeddable
@Entity // JPA entity
@Table(name = "locations") // Table mapping latitudes/longitudes -> places 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatLng {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Adds an ID for the separate table

    @Column(nullable = false) // @Column specifying NOT NULL at the database level
    private Double latitude;

    @Column(nullable = false) 
    private Double longitude;

    private String name; // Human-readable name

    // Constructor to instantiate LatLng without an ID
    /*public LatLng(Double latitude, Double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }*/

    //private String address;

    public LatLng(Double latitude, Double longitude, String name) {
        this(null, latitude, longitude, name); // Constructor without address 
    }

    /*
    public LatLng(Double latitude, Double longitude, String name, String address) {
        this(null, latitude, longitude, name, address); // Full constructor without id
    }*/
}
