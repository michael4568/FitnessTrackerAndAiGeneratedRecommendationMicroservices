package com.fitness.aiService.service;

import com.fitness.aiService.exception.ActivityNotFoundException;
import com.fitness.aiService.model.Recommendation;
import com.fitness.aiService.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;
    private final ActivityServiceClient activityServiceClient;
    private final ActivityAiService activityAiService;

    public List<Recommendation> getUserRecommendation(String userId) {
        return recommendationRepository.findByUserId(userId);
    }

    public Recommendation getactivityRecommendation(String activityId) {
        List<Recommendation> results = recommendationRepository.findByActivityId(activityId);
        if (results.isEmpty()) {
            throw new ActivityNotFoundException("no recommendation for activity id: " + activityId);
        }
        // Return the most recently saved one if duplicates somehow exist
        return results.get(results.size() - 1);
    }

    public Recommendation getOrGenerateActivityRecommendation(String activityId, String customApiKey) {
        List<Recommendation> existing = recommendationRepository.findByActivityId(activityId);

        // If a recommendation already exists, return the latest one immediately
        if (!existing.isEmpty()) {
            return existing.get(existing.size() - 1);
        }

        // No recommendation found — fetch activity and generate via Gemini
        com.fitness.aiService.model.Activity activity = activityServiceClient.getActivityById(activityId);
        if (activity == null) {
            throw new ActivityNotFoundException("No activity found with id: " + activityId + " to generate recommendation.");
        }

        Recommendation recommendation = activityAiService.getResponseRecommendation(activity, customApiKey);
        return recommendationRepository.save(recommendation);
    }
}
