package com.fitness.ActivityService.repository;

import com.fitness.ActivityService.model.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends MongoRepository<Activity , String> {

   Activity findByUserId(String userId);
}
