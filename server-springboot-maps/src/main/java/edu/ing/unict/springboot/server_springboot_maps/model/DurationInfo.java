package edu.ing.unict.springboot.server_springboot_maps.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Embeddable per dettagli durata
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DurationInfo {
    private String text; // es. "10 mins"
    private Integer value; // es. 600 (seconds)
}
