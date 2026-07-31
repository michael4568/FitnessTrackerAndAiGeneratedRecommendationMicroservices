package com.fitness.aiService.service;

import com.fitness.aiService.exception.ActivityNotFoundException;
import com.fitness.aiService.model.Recommendation;
import com.fitness.aiService.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;
    private final ActivityServiceClient activityServiceClient;
    private final ActivityAiService activityAiService;
    private final RateLimiterRegistry rateLimiterRegistry;

    public List<Recommendation> getUserRecommendation(String userId) {
        return recommendationRepository.findByUserId(userId);
    }

    public Recommendation getactivityRecommendation(String activityId) {
        List<Recommendation> results = recommendationRepository.findByActivityId(activityId);
        if (results.isEmpty()) {
            throw new ActivityNotFoundException("no recommendation for activity id: " + activityId);
        }
        return results.get(results.size() - 1);
    }

    public Recommendation getOrGenerateActivityRecommendation(String activityId, String customApiKey) {
        List<Recommendation> existing = recommendationRepository.findByActivityId(activityId);

        if (!existing.isEmpty()) {
            Recommendation rec = existing.get(existing.size() - 1);
            if (rec.getStatus() == com.fitness.aiService.model.RecommendationStatus.COMPLETED) {
                return rec;
            }
            if (rec.getStatus() == com.fitness.aiService.model.RecommendationStatus.PROCESSING) {
                throw new RuntimeException("Recommendation is currently being processed by the background AI service. Please wait.");
            }
        }

        // Apply rate limiting ONLY when we actually need to generate via Gemini
        String limiterName = (customApiKey != null && !customApiKey.trim().isEmpty()) ? "customRecommendation" : "freeRecommendation";
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(limiterName);
        
        try {
            return rateLimiter.executeSupplier(() -> generateWithLock(activityId, customApiKey, existing));
        } catch (RequestNotPermitted e) {
            throw e; // Controller will catch this via ExceptionHandler
        }
    }
    
    private Recommendation generateWithLock(String activityId, String customApiKey, List<Recommendation> existing) {
        com.fitness.aiService.model.Activity activity = activityServiceClient.getActivityById(activityId);
        if (activity == null) {
            throw new ActivityNotFoundException("No activity found with id: " + activityId + " to generate recommendation.");
        }

        Recommendation lockRec;
        if (!existing.isEmpty() && existing.get(existing.size() - 1).getStatus() == com.fitness.aiService.model.RecommendationStatus.FAILED) {
            lockRec = existing.get(existing.size() - 1);
            lockRec.setStatus(com.fitness.aiService.model.RecommendationStatus.PROCESSING);
        } else {
            lockRec = Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .status(com.fitness.aiService.model.RecommendationStatus.PROCESSING)
                    .build();
        }
        lockRec = recommendationRepository.save(lockRec);

        try {
            Recommendation recommendation = activityAiService.getResponseRecommendation(activity, customApiKey);
            recommendation.setId(lockRec.getId());
            recommendation.setStatus(com.fitness.aiService.model.RecommendationStatus.COMPLETED);
            return recommendationRepository.save(recommendation);
            
        } catch (Exception e) {
            lockRec.setStatus(com.fitness.aiService.model.RecommendationStatus.FAILED);
            // We save the failed status so it can be retried later
            recommendationRepository.save(lockRec);
            
            // Generate the fallback JSON to return to the user immediately, but do NOT save this fallback in the DB as COMPLETED
            return createDefaultRecommendation(lockRec.getId(), activity.getId(), activity.getUserId(), activity.getActivityType() != null ? activity.getActivityType().name() : "UNKNOWN");
        }
    }
    
    private Recommendation createDefaultRecommendation(String id, String activityId, String userId, String type) {
        Recommendation.Analysis fallbackAnalysis = new Recommendation.Analysis();
        fallbackAnalysis.setOverall("DEFAULT FALLBACK: Workout logged successfully. AI biomechanical analysis is temporarily unavailable due to high server demand or an invalid Gemini API Key. Once the key/server issue is fixed, click Open again to generate the actual results.");
        fallbackAnalysis.setPace("N/A");
        fallbackAnalysis.setHeartRate("N/A");
        fallbackAnalysis.setCaloriesBurned("N/A");

        return Recommendation.builder()
                .id(id)
                .activityId(activityId)
                .userId(userId)
                .type(type)
                .analysis(fallbackAnalysis)
                .recommendation("Great job completing your workout! Focus on hydration and recovery today.")
                .improvements(List.of("Maintain consistent pacing.", "Ensure proper stretching."))
                .safety(List.of("Listen to your body and rest if you feel unusual fatigue."))
                .suggestions(List.of("Try a light recovery walk tomorrow."))
                .status(com.fitness.aiService.model.RecommendationStatus.FAILED)
                .build();
    }
}
