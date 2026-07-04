package edu.ing.unict.springboot.server_springboot_maps.config;

/*
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.TimeoutException;

import java.time.Duration;
*/

// esempio di configurazione programmatica,
// ma configurazioni in application.properties per semplicità.
// duplicazione delle configurazioni.
//@Configuration
public class Resulience4jConfig {
    
/*
    //@Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Percentuale di fallimenti per aprire il circuito
                .minimumNumberOfCalls(5)  // Numero minimo di chiamate prima di calcolare il failureRateThreshold
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)    // Numero di chiamate nell'ultimo periodo
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Tempo di attesa in stato OPEN
                .permittedNumberOfCallsInHalfOpenState(3) // Chiamate consentite in HALF_OPEN
                .build();
    }

    //@Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    //@Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // Numero massimo di tentativi
                .waitDuration(Duration.ofSeconds(2)) // Tempo di attesa tra un tentativo e l'altro
                .retryExceptions(TimeoutException.class, ResourceAccessException.class) // Eccezioni da ritentare
                .build();
    }

    //@Bean
    public RetryRegistry retryRegistry(RetryConfig retryConfig) {
        return RetryRegistry.of(retryConfig);
    }

    //@Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5)) // Timeout per la chiamata
                .cancelRunningFuture(true) // Cancella il future se il timeout scatta
                .build();
    }

    //@Bean
    public TimeLimiterRegistry timeLimiterRegistry(TimeLimiterConfig timeLimiterConfig) {
        return TimeLimiterRegistry.of(timeLimiterConfig);
    }
*/
    
}

/*
 * prova non necessaria
 * configurazione in application.properties,
 *  una configurazione Java esplicita per Resilience4j, se necessario
 */