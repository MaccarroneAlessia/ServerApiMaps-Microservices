package edu.ing.unict.springboot.server_springboot_maps.messaging;

import edu.ing.unict.springboot.server_springboot_maps.model.TrafficData;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Profile("cloud")
public class SqsPublisher implements TrafficMessagePublisher {
    private static final Logger logger = LoggerFactory.getLogger(SqsPublisher.class);
    
    @Autowired(required = false)
    private SqsTemplate sqsTemplate;
    
    public static final String QUEUE_NAME = "traffic-data-queue";

    public SqsPublisher() {
    }

    @Override
    public void publishTrafficData(TrafficData trafficData) {
        if (sqsTemplate != null) {
            logger.info("Publishing to Amazon SQS...");
            sqsTemplate.send(QUEUE_NAME, trafficData);
        } else {
            logger.warn("Amazon SQS is disabled or no credentials found. Skipping message publish.");
        }
    }
}
