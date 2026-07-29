package com.fitness.aiService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    private final WebClient webClient;

    // No-arg constructor prevents Spring BeanCreationException
    public GeminiService() {
        this.webClient = WebClient.create();
    }

    public String getRecommendation(String details) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gemini-3.6-flash");
        requestBody.put("input", details);

        String rawResponse = webClient.post()
                .uri(geminiUrl)
                .header("x-goog-api-key", geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                // Automatically retry up to 3 times with a 2-second delay if Google returns 503 (Overloaded) or 429 (Rate Limit)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException &&
                                (((WebClientResponseException) throwable).getStatusCode().value() == 503 ||
                                        ((WebClientResponseException) throwable).getStatusCode().value() == 429)))
                // Fallback to safe JSON if retries are exhausted or any other HTTP error occurs
                .onErrorResume(e -> {
                    log.error("Gemini API call failed after retries. Returning fallback recommendation. Root cause: {}", e.getMessage());
                    String fallbackJson = """
                            {
                              "analysis": {
                                "overall": "Workout logged successfully. AI biomechanical analysis is temporarily unavailable due to high server demand.",
                                "pace": "N/A",
                                "heartRate": "N/A",
                                "caloriesBurned": "N/A"
                              },
                              "recommendation": "Great job completing your workout! Focus on hydration and recovery today.",
                              "improvements": [
                                "Maintain consistent pacing in your next session.",
                                "Ensure proper pre-workout stretching and cool-down."
                              ],
                              "safety": [
                                "Listen to your body and rest if you feel unusual fatigue or sharp pain."
                              ],
                              "suggestions": [
                                "Try a light recovery walk or flexibility session tomorrow."
                              ]
                            }
                            """;
                    return Mono.just(fallbackJson);
                })
                .block();
        try {
            String cleanJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(rawResponse)
                    .findValue("text")
                    .asText();


            log.info("Clean Gemini Response:\n{}", cleanJson);

            return cleanJson;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from Gemini response", e);
        }
    }
}