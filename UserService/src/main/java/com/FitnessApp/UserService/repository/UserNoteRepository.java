package com.FitnessApp.UserService.repository;

import com.FitnessApp.UserService.model.UserNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNoteRepository extends JpaRepository<UserNote, String> {
    List<UserNote> findByUserIdAndTargetId(String userId, String targetId);
    Optional<UserNote> findByUserIdAndTargetIdAndNoteDate(String userId, String targetId, String noteDate);
    List<UserNote> findByUserId(String userId);
}
