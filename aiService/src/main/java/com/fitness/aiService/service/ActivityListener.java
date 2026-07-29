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

            Recommendation recommendation = activityAiService.getResponseRecommendation(activity);
            recommendationRepository.save(recommendation);


    }
}
