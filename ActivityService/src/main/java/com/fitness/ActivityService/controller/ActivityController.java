package com.fitness.ActivityService.controller;

import com.fitness.ActivityService.dto.ActivityRequest;
import com.fitness.ActivityService.dto.ActivityResponse;
import com.fitness.ActivityService.model.Activity;
import com.fitness.ActivityService.service.ActivityService;
import jakarta.websocket.server.PathParam;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;
    
    @PostMapping("/test")
    public ResponseEntity<String> testPost() {
        return ResponseEntity.ok("POST request reached ActivityService!");
    }

    @PostMapping("/track")
    @RateLimiter(name = "tracking")
    public ResponseEntity<ActivityResponse> trackActivity(@RequestBody ActivityRequest activityRequest){
        return ResponseEntity.ok(activityService.trackActivity(activityRequest));
    }

    @GetMapping("/{activityId}")
    @RateLimiter(name = "fetching")
    public ResponseEntity<Activity> getActivityById(@PathVariable("activityId") String activityId) {
        return ResponseEntity.ok(activityService.getActivityById(activityId));
    }

    @GetMapping("/user/{userId}")
    @RateLimiter(name = "fetching")
    public ResponseEntity<org.springframework.data.domain.Page<Activity>> getActivitiesByUser(
            @PathVariable("userId") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("startTime").descending());
        return ResponseEntity.ok(activityService.getActivitiesByUser(userId, pageable));
    }

    @GetMapping("/stats/{userId}")
    @RateLimiter(name = "fetching")
    public ResponseEntity<java.util.Map<String, Object>> getActivityStats(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(activityService.getActivityStats(userId));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(io.github.resilience4j.ratelimiter.RequestNotPermitted.class)
    public ResponseEntity<String> handleRateLimitException(io.github.resilience4j.ratelimiter.RequestNotPermitted e) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
    }
}

