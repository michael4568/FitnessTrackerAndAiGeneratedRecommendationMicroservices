package com.fitness.ActivityService.repository;

import com.fitness.ActivityService.model.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity , String> {
    List<Activity> findByUserId(String userId);
    Page<Activity> findByUserId(String userId, Pageable pageable);
}

