package com.fitness.aiService.service;

import com.fitness.aiService.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityListener {
    private final ActivityAiService activityAiService;
    @KafkaListener(topics = "${kafka.topic.name}" , groupId = "${spring.kafka.consumer.group-id}")
    public void ListenAtivity(Activity activity){
        log.info("Activity consumed from producer with userId: " + activity.getUserId());
        activityAiService.getResponseRecommendation(activity);

    }
}
