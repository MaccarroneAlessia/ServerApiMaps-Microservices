package edu.ing.unict.springboot.server_springboot_maps.messaging;

import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import edu.ing.unict.springboot.server_springboot_maps.repository.TrafficDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class RabbitMqTrafficMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqTrafficMessageListener.class);
    private final TrafficDataRepository trafficDataRepository;

    public RabbitMqTrafficMessageListener(TrafficDataRepository trafficDataRepository) {
        this.trafficDataRepository = trafficDataRepository;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    public void receiveMessage(TrafficData trafficData) {
        logger.info("Received from RabbitMQ: Saving TrafficData to DB asynchronoulsy");
        trafficDataRepository.save(trafficData);
    }
}
