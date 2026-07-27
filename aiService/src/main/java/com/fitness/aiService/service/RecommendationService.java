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
    public List<Recommendation> getUserRecommendation(String userId) {
       return  recommendationRepository.findByUserId(userId);
    }

    public Recommendation getactivityRecommendation(String activityId) {
        Recommendation recommendation = recommendationRepository.findByActivityId(activityId).orElseThrow(
                () -> new ActivityNotFoundException("no recommendation for activity id: " + activityId)
        );
        return  recommendation;
    }
}
