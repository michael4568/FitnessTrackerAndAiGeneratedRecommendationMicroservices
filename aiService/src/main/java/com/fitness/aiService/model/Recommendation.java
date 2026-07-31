package com.fitness.aiService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "recommendation_activity")
@Data
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true) // PREVENTS CRASHES IF AI ADDS EXTRA FIELDS
public class Recommendation {
    @Id
    private String id;
    private String activityId;
    private String userId;
    
    private RecommendationStatus status;

    private Analysis analysis;
    private String type;
    private String recommendation;
    private List<String> improvements;
    private List<String> safety;
    private List<String> suggestions;

    @CreatedDate
    private LocalDateTime createdAt;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Analysis {
        private String overall;
        private String pace;
        private String heartRate;
        private String caloriesBurned;
    }
}