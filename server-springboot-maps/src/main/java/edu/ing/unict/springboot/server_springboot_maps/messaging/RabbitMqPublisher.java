package edu.ing.unict.springboot.server_springboot_maps.messaging;

import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class RabbitMqPublisher implements TrafficMessagePublisher {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishTrafficData(TrafficData trafficData) {
        logger.info("Publishing to RabbitMQ...");
        rabbitTemplate.convertAndSend(RabbitMqConfig.QUEUE_NAME, trafficData);
    }
}
