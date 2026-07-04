package edu.ing.unict.springboot.server_springboot_maps;

import edu.ing.unict.springboot.server_springboot_maps.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import org.springframework.scheduling.annotation.EnableScheduling; // Abilita la schedulazione
import org.springframework.http.client.SimpleClientHttpRequestFactory; // Per configurare timeout RestTemplate

@SpringBootApplication // annotazione principale
@EnableScheduling // Abilita la scansione dei metodi annotati con @Scheduled -> funzionalità di schedulazione di Spring
// Abilita la schedulazione per il TrafficCollectionService
public class TrafficApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficApplication.class, args);
	}

	// RestTemplate Bean: Inietta AppConfig correttamente come parametro
    // Configura RestTemplate con i timeout definiti in AppConfig
	@Bean
    public RestTemplate restTemplate(AppConfig appConfig) { // AppConfig iniettato qui
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(appConfig.getApiTimeoutSeconds() * 1000);
        factory.setReadTimeout(appConfig.getApiTimeoutSeconds() * 1000);
        return new RestTemplate(factory);
    }

	// ObjectMapper Bean:
    // Bean per la serializzazione/deserializzazione JSON
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules(); // per LocalDateTime
    }

}