package com.fitness.aiService.repository;

import com.fitness.aiService.model.Recommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation , String> {

    List<Recommendation> findByUserId(String userId);

    // Returns List to safely handle any duplicate documents for same activityId
    List<Recommendation> findByActivityId(String activityId);
}
