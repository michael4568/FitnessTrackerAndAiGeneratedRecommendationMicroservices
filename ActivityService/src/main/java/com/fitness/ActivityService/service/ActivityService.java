package com.fitness.ActivityService.service;

import com.fitness.ActivityService.dto.ActivityRequest;
import com.fitness.ActivityService.dto.ActivityResponse;
import com.fitness.ActivityService.exception.UserNotFoundException;
import com.fitness.ActivityService.model.Activity;
import com.fitness.ActivityService.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private  final userValidationService userValidationService;
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

}
