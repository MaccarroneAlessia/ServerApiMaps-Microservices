package edu.ing.unict.springboot.server_springboot_maps;

import edu.ing.unict.springboot.server_springboot_maps.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import org.springframework.scheduling.annotation.EnableScheduling; // Enables scheduling
import org.springframework.http.client.SimpleClientHttpRequestFactory; // Configures RestTemplate timeouts

@SpringBootApplication // Main application annotation
@EnableScheduling // Enables scanning for @Scheduled annotated methods -> Spring scheduling functionality
// Enables scheduling for the TrafficCollectionService
public class TrafficApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficApplication.class, args);
	}

	// RestTemplate Bean: Correctly injects AppConfig as a parameter
    // Configures RestTemplate with the timeouts defined in AppConfig
	@Bean
    public RestTemplate restTemplate(AppConfig appConfig) { // AppConfig injected here
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(appConfig.getApiTimeoutSeconds() * 1000);
        factory.setReadTimeout(appConfig.getApiTimeoutSeconds() * 1000);
        return new RestTemplate(factory);
    }

	// ObjectMapper Bean:
    // Bean for JSON serialization/deserialization
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules(); // Registers modules like LocalDateTime
    }

}