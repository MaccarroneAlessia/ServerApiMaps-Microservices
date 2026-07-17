package edu.ing.unict.springboot.server_springboot_maps.repository;
import edu.ing.unict.springboot.server_springboot_maps.model.LatLng;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LatLngRepository extends JpaRepository<LatLng, Long> {
    Optional<LatLng> findByLatitudeAndLongitude(Double latitude, Double longitude);
    // Method without a body because it is automatically implemented by Spring Data JPA
    List<LatLng> findByName(String name);
    
}
