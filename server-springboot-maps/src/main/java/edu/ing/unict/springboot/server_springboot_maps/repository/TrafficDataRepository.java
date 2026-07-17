package edu.ing.unict.springboot.server_springboot_maps.repository;

import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficDataRepository extends JpaRepository<TrafficData, Long> {
    // Automatic CRUD methods from Spring Data JPA 
    // Used for validation checks prior to deleting a location
    //List<TrafficData> findByOriginIdOrDestinationId(Long locationId);
    
    List<TrafficData> findByOrigin_IdOrDestination_Id(Long originLocationId, Long destinationLocationId);
    List<TrafficData> findByOriginIdAndDestinationIdOrderByTimestampAsc(Long originId, Long destinationId);
}
