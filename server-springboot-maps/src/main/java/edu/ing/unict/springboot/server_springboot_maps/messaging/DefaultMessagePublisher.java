package edu.ing.unict.springboot.server_springboot_maps.messaging;

import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("default")
public class DefaultMessagePublisher implements TrafficMessagePublisher {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessagePublisher.class);

    @Override
    public void publishTrafficData(TrafficData trafficData) {
        logger.info("Default publisher: no message broker configured (RabbitMQ/SQS disabled).");
    }
}
