package com.fitness.ActivityService.dto;

import com.fitness.ActivityService.model.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private String id;
    private String userId;
    private ActivityType activityType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startTime;
    private Integer Duration;
    private Integer caloriesBurned;
    private Map<String , Object> additionalMetrics;
}
