package edu.ing.unict.springboot.server_springboot_maps.messaging;

import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import edu.ing.unict.springboot.server_springboot_maps.repository.TrafficDataRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cloud")
public class SqsTrafficMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(SqsTrafficMessageListener.class);
    private final TrafficDataRepository trafficDataRepository;

    public SqsTrafficMessageListener(TrafficDataRepository trafficDataRepository) {
        this.trafficDataRepository = trafficDataRepository;
    }

    @SqsListener("traffic-data-queue")
    public void receiveMessage(TrafficData trafficData) {
        logger.info("Received from Amazon SQS: Saving TrafficData to DB asynchronoulsy");
        trafficDataRepository.save(trafficData);
    }
}
