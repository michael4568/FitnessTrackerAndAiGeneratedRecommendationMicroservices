package com.fitness.aiService.service;

import com.fitness.aiService.model.Activity;
import com.fitness.aiService.model.Recommendation;
import com.fitness.aiService.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityListener {
    private final ActivityAiService activityAiService;
    private final RecommendationRepository recommendationRepository;
    @KafkaListener(topics = "${kafka.topic.name}" , groupId = "${spring.kafka.consumer.group-id}")
    public void ListenAtivity(Activity activity){
        log.info("Activity consumed from producer with userId: " + activity.getUserId());

        java.util.List<Recommendation> existing = recommendationRepository.findByActivityId(activity.getId());
        
        if (!existing.isEmpty()) {
            Recommendation rec = existing.get(existing.size() - 1);
            if (rec.getStatus() == com.fitness.aiService.model.RecommendationStatus.COMPLETED || 
                rec.getStatus() == com.fitness.aiService.model.RecommendationStatus.PROCESSING) {
                log.info("Activity recommendation already processing or completed. Skipping Kafka message.");
                return;
            }
        }

        // 1. Create a PROCESSING lock in MongoDB
        Recommendation lockRec = Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .status(com.fitness.aiService.model.RecommendationStatus.PROCESSING)
                .build();
        lockRec = recommendationRepository.save(lockRec);
        
        try {
            // 2. Call Gemini
            Recommendation recommendation = activityAiService.getResponseRecommendation(activity, null);
            
            // 3. Update the lock with the actual result
            recommendation.setId(lockRec.getId());
            recommendation.setStatus(com.fitness.aiService.model.RecommendationStatus.COMPLETED);
            recommendationRepository.save(recommendation);
            
        } catch (Exception e) {
            log.error("Failed to generate recommendation via Kafka", e);
            // 4. Update status to FAILED so REST API can retry later
            lockRec.setStatus(com.fitness.aiService.model.RecommendationStatus.FAILED);
            recommendationRepository.save(lockRec);
        }
    }
}
