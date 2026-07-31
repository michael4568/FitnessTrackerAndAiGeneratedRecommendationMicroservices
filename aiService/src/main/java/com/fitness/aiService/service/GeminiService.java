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
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiService(ObjectMapper objectMapper) {
        this.webClient = WebClient.create();
        this.objectMapper = objectMapper;
    }

    public String getRecommendation(String details, String customApiKey) {
        String apiKey = (customApiKey != null && !customApiKey.trim().isEmpty()) ? customApiKey : geminiApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", details);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        String targetUrl = geminiUrl;
        if (targetUrl == null || targetUrl.contains("interactions") || targetUrl.trim().isEmpty()) {
            targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
        }

        return webClient.post()
                .uri(targetUrl)
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(rawResponse -> {
                    try {
                        String cleanJson = objectMapper.readTree(rawResponse)
                                .findValue("text")
                                .asText()
                                .replace("```json", "")
                                .replace("```", "")
                                .trim();
                        log.info("Clean Gemini Response:\n{}", cleanJson);
                        return cleanJson;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to extract text from Gemini response", e);
                    }
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException &&
                                (((WebClientResponseException) throwable).getStatusCode().value() == 503 ||
                                        ((WebClientResponseException) throwable).getStatusCode().value() == 429)))
                .block();
    }

    public String askGemini(String prompt, String customApiKey) {
        String apiKey = (customApiKey != null && !customApiKey.trim().isEmpty()) ? customApiKey : geminiApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        String targetUrl = geminiUrl;
        if (targetUrl == null || targetUrl.contains("interactions") || targetUrl.trim().isEmpty()) {
            targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
        }

        try {
            String rawResponse = webClient.post()
                    .uri(targetUrl)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return objectMapper.readTree(rawResponse)
                    .findValue("text")
                    .asText()
                    .trim();
        } catch (Exception e) {
            log.error("Gemini chatbot query failed: {}", e.getMessage());
            throw new RuntimeException("Gemini chatbot query failed", e);
        }
    }
}