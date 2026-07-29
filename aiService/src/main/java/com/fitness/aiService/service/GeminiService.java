package com.fitness.aiService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper; // Best practice: reuse a single ObjectMapper instance

    public GeminiService(ObjectMapper objectMapper) {
        this.webClient = WebClient.create();
        this.objectMapper = objectMapper;
    }

    public String getRecommendation(String details) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gemini-3.6-flash");
        requestBody.put("input", details);

        return webClient.post()
                .uri(geminiUrl)
                .header("x-goog-api-key", geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)

                // STEP 1: Unwrap the envelope immediately upon success
                .map(rawResponse -> {
                    try {
                        String cleanJson = objectMapper.readTree(rawResponse)
                                .findValue("text")
                                .asText()
                                .replace("```json", "") // Removes the opening Markdown tag
                                .replace("```", "")     // Removes the closing Markdown tag
                                .trim();                // Cleans up any leftover physical newlines or spaces at the top/bottom
                        log.info("Clean Gemini Response:\n{}", cleanJson);
                        return cleanJson;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to extract text from Gemini response", e);
                    }
                })

                // STEP 2: Retry logic
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException &&
                                (((WebClientResponseException) throwable).getStatusCode().value() == 503 ||
                                        ((WebClientResponseException) throwable).getStatusCode().value() == 429)))

                // STEP 3: Fallback (Because of Step 1, this now expects Clean JSON, not Google JSON!)
                .onErrorResume(e -> {
                    log.error("Gemini API call failed. Returning fallback recommendation. Cause: {}", e.getMessage());


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
                .block(); // Blocks and returns the final clean string (either from Step 1 or Step 3)
    }
}