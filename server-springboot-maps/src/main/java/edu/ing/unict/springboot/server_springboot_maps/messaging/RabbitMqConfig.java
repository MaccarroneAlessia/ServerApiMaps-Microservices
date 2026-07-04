package edu.ing.unict.springboot.server_springboot_maps.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class RabbitMqConfig {
    public static final String QUEUE_NAME = "traffic-data-queue";

    @Bean
    public Queue trafficDataQueue() {
        return new Queue(QUEUE_NAME, true);
    }
}
