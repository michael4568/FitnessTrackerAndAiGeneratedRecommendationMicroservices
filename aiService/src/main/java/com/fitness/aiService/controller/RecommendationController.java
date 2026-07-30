package com.fitness.aiService.controller;

import com.fitness.aiService.model.Activity;
import com.fitness.aiService.model.Recommendation;
import com.fitness.aiService.repository.RecommendationRepository;
import com.fitness.aiService.service.RecommendationService;
import com.fitness.aiService.service.ActivityServiceClient;
import com.fitness.aiService.service.GeminiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ActivityServiceClient activityServiceClient;
    private final GeminiService geminiService;
    private final RecommendationRepository recommendationRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Recommendation>> getRecommendationByUser(@PathVariable String userId){
        return ResponseEntity.ok(recommendationService.getUserRecommendation(userId));
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Recommendation> getRecommendationByactivity(
            @PathVariable String activityId,
            @RequestHeader(value = "X-Gemini-API-Key", required = false) String customApiKey){
        // Changed to on-demand generation if not already generated
        return ResponseEntity.ok(recommendationService.getOrGenerateActivityRecommendation(activityId, customApiKey));
    }

    @PostMapping("/chat/user")
    public ResponseEntity<ChatResponse> chatOnUserPage(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Gemini-API-Key", required = false) String customApiKey,
            @RequestBody ChatRequest request) {
        
        log.info("Chat requested on user page for user: {}", userId);
        
        // 1. Fetch last 5 activities
        List<Activity> recentActivities = activityServiceClient.getRecentActivities(userId, 5);
        String activitiesText = "None logged yet.";
        if (!recentActivities.isEmpty()) {
            activitiesText = recentActivities.stream().map(a -> {
                String metricsStr = (a.getAdditionalMetrics() != null && !a.getAdditionalMetrics().isEmpty()) 
                        ? a.getAdditionalMetrics().toString() 
                        : "None";
                return String.format("- Type: %s, Duration: %d mins, Calories: %d, Date: %s, Notes/Metrics: %s",
                        a.getActivityType(), a.getDuration(), a.getCaloriesBurned(), a.getStartTime(), metricsStr);
            }).collect(Collectors.joining("\n"));
        }

        // 2. Prepare guard-railed prompt
        String prompt = "You are a strict fitness, health, and diet coaching assistant. You are guard-railed: you MUST ONLY answer questions directly related to fitness, health, nutrition, workout targets, meal plans, or sports biomechanics. Questions like 'what can be my next target', 'what meal should I take', or recovery advice are fully safe.\n" +
                "If the user's query is off-topic, not related to health/fitness, or asks for clinical medical diagnoses, or is dangerous, you MUST respond EXACTLY with: 'cant help with that try aqsking differnt'. Do not explain or add any other text.\n" +
                "\n" +
                "Below are the user's recent physical activities logs:\n" +
                activitiesText + "\n" +
                "\n" +
                "User Query: " + request.getMessage();

        // 3. Query Gemini
        String answer = geminiService.askGemini(prompt, customApiKey);
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @PostMapping("/chat/recommendation")
    public ResponseEntity<ChatResponse> chatOnRecommendationPage(
            @RequestHeader(value = "X-Gemini-API-Key", required = false) String customApiKey,
            @RequestBody RecommendationChatRequest request) {
        
        log.info("Chat requested on recommendation page for recommendation ID: {}", request.getRecommendationId());
        
        // 1. Fetch recommendation details
        Recommendation rec = recommendationRepository.findById(request.getRecommendationId()).orElse(null);
        if (rec == null) {
            return ResponseEntity.badRequest().body(new ChatResponse("Recommendation not found."));
        }

        String improvements = rec.getImprovements() != null ? String.join(", ", rec.getImprovements()) : "None";
        String safety = rec.getSafety() != null ? String.join(", ", rec.getSafety()) : "None";
        String suggestions = rec.getSuggestions() != null ? String.join(", ", rec.getSuggestions()) : "None";

        // 2. Prepare guard-railed prompt
        String prompt = "You are a strict fitness, health, and diet coaching assistant. You are guard-railed: you MUST ONLY answer questions directly related to fitness, health, nutrition, workout targets, meal plans, or sports biomechanics. Questions like 'what can be my next target', 'what meal should I take', or recovery advice are fully safe.\n" +
                "If the user's query is off-topic, not related to health/fitness, or asks for clinical medical diagnoses, or is dangerous, you MUST respond EXACTLY with: 'cant help with that try aqsking differnt'. Do not explain or add any other text.\n" +
                "\n" +
                "Below are the details of the specific AI Recommendation you are answering questions about:\n" +
                "Activity Type: " + rec.getType() + "\n" +
                "Analysis Overall: " + (rec.getAnalysis() != null ? rec.getAnalysis().getOverall() : "N/A") + "\n" +
                "Analysis Pace: " + (rec.getAnalysis() != null ? rec.getAnalysis().getPace() : "N/A") + "\n" +
                "Analysis Heart Rate: " + (rec.getAnalysis() != null ? rec.getAnalysis().getHeartRate() : "N/A") + "\n" +
                "Analysis Calories: " + (rec.getAnalysis() != null ? rec.getAnalysis().getCaloriesBurned() : "N/A") + "\n" +
                "Core Recommendation: " + rec.getRecommendation() + "\n" +
                "Improvements: " + improvements + "\n" +
                "Safety Tips: " + safety + "\n" +
                "Suggestions: " + suggestions + "\n" +
                "\n" +
                "User Query: " + request.getMessage();

        // 3. Query Gemini
        String answer = geminiService.askGemini(prompt, customApiKey);
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class RecommendationChatRequest {
        private String recommendationId;
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String response;
        public ChatResponse(String response) {
            this.response = response;
        }
    }
}