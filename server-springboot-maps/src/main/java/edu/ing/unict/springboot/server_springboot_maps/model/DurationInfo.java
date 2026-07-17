package edu.ing.unict.springboot.server_springboot_maps.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Embeddable for duration details
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DurationInfo {
    private String text; // e.g., "10 mins"
    private Integer value; // e.g., 600 (seconds)
}
