package edu.ing.unict.springboot.server_springboot_maps.messaging;

import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;

public interface TrafficMessagePublisher {
    void publishTrafficData(TrafficData trafficData);
}
