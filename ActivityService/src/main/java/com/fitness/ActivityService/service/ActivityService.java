package com.fitness.ActivityService.service;

import com.fitness.ActivityService.dto.ActivityRequest;
import com.fitness.ActivityService.dto.ActivityResponse;
import com.fitness.ActivityService.exception.UserNotFoundException;
import com.fitness.ActivityService.model.Activity;
import com.fitness.ActivityService.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {
    private final ActivityRepository activityRepository;
    private  final userValidationService userValidationService;
    private final KafkaTemplate<String , Activity> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String topicName;
    public ActivityResponse trackActivity(ActivityRequest activityRequest) {
        String userId = activityRequest.getUserId();
        if(!userValidationService.validateUser(userId)){
            throw new UserNotFoundException("no user exist with Id: " + userId + " or a server error occured");
        }
        Activity activity = Activity.builder().
                userId(activityRequest.getUserId()).activityType(activityRequest.getActivityType()).
                startTime(activityRequest.getStartTime()).
                Duration(activityRequest.getDuration()).
                caloriesBurned(activityRequest.getCaloriesBurned()).
                additionalMetrics(activityRequest.getAdditionalMetrics()).
                build();
        Activity savedActivity = activityRepository.save(activity);
        try {
            kafkaTemplate.send(topicName, savedActivity.getId(), savedActivity);
        } catch (Exception e) {
            log.error("activity message was not sent to Kafka");
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return MapToResponse(savedActivity);
    }

    private ActivityResponse MapToResponse(Activity savedActivity) {
        ActivityResponse activityResponse = new ActivityResponse();
        activityResponse.setActivityType(savedActivity.getActivityType());
        activityResponse.setId(savedActivity.getId());
        activityResponse.setDuration(savedActivity.getDuration());
        activityResponse.setStartTime(savedActivity.getStartTime());
        activityResponse.setCaloriesBurned(savedActivity.getCaloriesBurned());
        activityResponse.setUpdatedAt(savedActivity.getUpdatedAt());
        activityResponse.setAdditionalMetrics(savedActivity.getAdditionalMetrics());
        activityResponse.setUserId(savedActivity.getUserId());
        activityResponse.setCreatedAt(savedActivity.getCreatedAt());
        return activityResponse;
    }

    public org.springframework.data.domain.Page<Activity> getActivitiesByUser(String userId, org.springframework.data.domain.Pageable pageable) {
        return activityRepository.findByUserId(userId, pageable);
    }

    public Activity getActivityById(String activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new com.fitness.ActivityService.exception.ActivityNotFoundException("Activity not found with id: " + activityId));
    }

    public java.util.Map<String, Object> getActivityStats(String userId) {
        java.util.List<Activity> activities = activityRepository.findByUserId(userId);
        int totalWorkouts = activities.size();
        int totalDuration = activities.stream().mapToInt(a -> a.getDuration() != null ? a.getDuration() : 0).sum();
        int totalCalories = activities.stream().mapToInt(a -> a.getCaloriesBurned() != null ? a.getCaloriesBurned() : 0).sum();

        java.util.Map<String, Long> workoutsByType = activities.stream()
                .filter(a -> a.getActivityType() != null)
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getActivityType().name(), java.util.stream.Collectors.counting()));

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalWorkouts", totalWorkouts);
        stats.put("totalDuration", totalDuration);
        stats.put("totalCalories", totalCalories);
        stats.put("workoutsByType", workoutsByType);

        return stats;
    }
}

