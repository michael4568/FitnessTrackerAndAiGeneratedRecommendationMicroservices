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
       return  recommendationRepository.findByUserId(userId);
    }

    public Recommendation getactivityRecommendation(String activityId) {
        return recommendationRepository.findByActivityId(activityId).orElseThrow(
                () -> new ActivityNotFoundException("no recommendation for activity id: " + activityId)
        );
    }

    public Recommendation getOrGenerateActivityRecommendation(String activityId, String customApiKey) {
        return recommendationRepository.findByActivityId(activityId).orElseGet(() -> {
            com.fitness.aiService.model.Activity activity = activityServiceClient.getActivityById(activityId);
            if (activity == null) {
                throw new ActivityNotFoundException("No activity found with id: " + activityId + " to generate recommendation.");
            }
            Recommendation recommendation = activityAiService.getResponseRecommendation(activity, customApiKey);
            return recommendationRepository.save(recommendation);
        });
    }
}

