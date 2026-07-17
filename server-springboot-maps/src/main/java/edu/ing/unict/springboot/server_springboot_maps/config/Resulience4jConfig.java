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

// Example of programmatic configuration,
// but relying on application.properties for simplicity.
// Prevents configuration duplication.
//@Configuration
public class Resulience4jConfig {
    
/*
    //@Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Failure rate percentage threshold to open the circuit
                .minimumNumberOfCalls(5)  // Minimum number of calls before calculating the failure rate
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)    // Number of calls in the sliding window
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Wait time in OPEN state
                .permittedNumberOfCallsInHalfOpenState(3) // Allowed calls in HALF_OPEN state
                .build();
    }

    //@Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    //@Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // Maximum number of retry attempts
                .waitDuration(Duration.ofSeconds(2)) // Wait duration between attempts
                .retryExceptions(TimeoutException.class, ResourceAccessException.class) // Exceptions triggering a retry
                .build();
    }

    //@Bean
    public RetryRegistry retryRegistry(RetryConfig retryConfig) {
        return RetryRegistry.of(retryConfig);
    }

    //@Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5)) // Call timeout duration
                .cancelRunningFuture(true) // Cancels the future if the timeout triggers
                .build();
    }

    //@Bean
    public TimeLimiterRegistry timeLimiterRegistry(TimeLimiterConfig timeLimiterConfig) {
        return TimeLimiterRegistry.of(timeLimiterConfig);
    }
*/
    
}

/*
 * Unnecessary test
 * Configurations are in application.properties,
 * provides explicit Java configuration for Resilience4j if needed
 */