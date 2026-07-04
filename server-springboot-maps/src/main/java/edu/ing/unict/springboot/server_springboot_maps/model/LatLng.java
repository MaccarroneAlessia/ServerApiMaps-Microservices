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

//Embeddable per latitudine e longitudine
//@Embeddable
@Entity // entità JPA
@Table(name = "locations") // tabella per le latitudini/longitudini -> posti 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatLng {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Aggiungiamo un ID per la tabella separata

    @Column(nullable = false) // -> @Column per specificare NOT NULL a livello di database
    private Double latitude;

    @Column(nullable = false) 
    private Double longitude;

    private String name; // nome leggibile

    // Costruttore per creare LatLng senza ID
    /*public LatLng(Double latitude, Double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }*/

    //private String adress;

    public LatLng(Double latitude, Double longitude, String name) {
        this(null, latitude, longitude, name); // costruttore senza indirizzo 
    }

    /*
    public LatLng(Double latitude, Double longitude, String name, String adress) {
        this(null, latitude, longitude, name,adress); // costruttore full senza id
    }*/
}
