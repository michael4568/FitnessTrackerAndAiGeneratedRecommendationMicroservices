package com.fitness.ActivityService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class userValidationService {
    private final WebClient userWebClient;
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
}
