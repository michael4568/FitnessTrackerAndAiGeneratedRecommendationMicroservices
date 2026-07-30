package com.fitness.aiService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiService.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceClient {
    private final WebClient webClientActivityService;
    private final ObjectMapper objectMapper;

    public Activity getActivityById(String activityId) {
        try {
            return webClientActivityService.get()
                    .uri("/api/activities/{activityId}", activityId)
                    .retrieve()
                    .bodyToMono(Activity.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch activity by id: {}", activityId, e);
            return null;
        }
    }

    public List<Activity> getRecentActivities(String userId, int limit) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientActivityService.get()
                    .uri("/api/activities/user/{userId}?page=0&size={size}", userId, limit)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("content")) {
                return objectMapper.convertValue(response.get("content"), new TypeReference<List<Activity>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to fetch recent activities for user: {}", userId, e);
        }
        return Collections.emptyList();
    }
}
