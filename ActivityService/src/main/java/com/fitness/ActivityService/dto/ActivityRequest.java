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
public class ActivityRequest {
    private String userId;
    private ActivityType activityType;
    private LocalDateTime startTime;
    private Integer duration;
    private Integer caloriesBurned;
    private Map<String , Object> additionalMetrics;
}
