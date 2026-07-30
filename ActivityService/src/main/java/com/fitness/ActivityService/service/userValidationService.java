package com.fitness.ActivityService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class userValidationService {
    private final WebClient userWebClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "validateUserFallback")
    public boolean validateUser(String userId){
        try {
            return userWebClient.get().uri("/api/user/validate/{userId}", userId)
                    .retrieve().bodyToMono(Boolean.class).block();
        }
        catch(WebClientResponseException e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean validateUserFallback(String userId, Throwable throwable) {
        log.warn("UserService validation failed for userId: {} due to exception: {}. Cascading fallback applied (user assumed valid).", userId, throwable.getMessage());
        return true; // Resilient fallback: allow tracking activity even if UserService is temporarily down.
    }
}

