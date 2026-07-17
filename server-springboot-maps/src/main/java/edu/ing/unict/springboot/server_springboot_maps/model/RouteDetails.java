package edu.ing.unict.springboot.server_springboot_maps.model;

import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Embeddable for route details
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDetails {
    private String summary;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "text", column = @Column(name = "duration_text")),
        @AttributeOverride(name = "value", column = @Column(name = "duration_value"))
    })
    private DurationInfo duration; // Duration without traffic

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "text", column = @Column(name = "duration_in_traffic_text")),
        @AttributeOverride(name = "value", column = @Column(name = "duration_in_traffic_value"))
    })
    private DurationInfo durationInTraffic; // Duration with traffic
}
