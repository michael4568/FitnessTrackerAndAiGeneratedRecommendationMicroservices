package com.FitnessApp.UserService.service;

import com.FitnessApp.UserService.dto.RegisterRequest;
import com.FitnessApp.UserService.dto.UserResponse;
import com.FitnessApp.UserService.exception.UniqueEmailConstraintException;
import com.FitnessApp.UserService.exception.UserNotFoundException;
import com.FitnessApp.UserService.model.User;
import com.FitnessApp.UserService.model.UserNote;
import com.FitnessApp.UserService.model.UserRole;
import com.FitnessApp.UserService.repository.UserNoteRepository;
import com.FitnessApp.UserService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserNoteRepository userNoteRepository;

    public UserResponse registerUser(RegisterRequest registerRequest) {
        User existingUser = userRepository.findByEmail(registerRequest.getEmail());
        if (existingUser != null) {
            throw new UniqueEmailConstraintException("email already exists");
        }

        User user = User.builder().
                id(java.util.UUID.randomUUID().toString()).
                email(registerRequest.getEmail()).
                firstName(registerRequest.getFirstName()).
                lastName(registerRequest.getLastName()).
                password(registerRequest.getPassword()).
                role(UserRole.USER).
                build();
        User savedUser = userRepository.save(user);
        return MapToResponse(savedUser);
    }

    public UserResponse syncUser(String userId, String email, String firstName, String lastName) {
        // Protect against empty/null userId from Gateway or Frontend
        if (userId == null || userId.trim().isEmpty()) {
            userId = null; 
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // If not found by ID, try finding by email
        if (user == null && email != null && !email.isEmpty()) {
            user = userRepository.findByEmail(email);
        }

        if (user != null) {
            // UPDATE existing user (prevents 500 errors from foreign key constraints)
            user.setFirstName(firstName != null && !firstName.isEmpty() ? firstName : user.getFirstName());
            if (lastName != null && !lastName.isEmpty()) {
                user.setLastName(lastName);
            }
            user = userRepository.save(user);
        } else {
            // CREATE new user
            String finalId = userId != null ? userId : java.util.UUID.randomUUID().toString();
            user = User.builder()
                    .id(finalId)
                    .email(email)
                    .firstName(firstName != null && !firstName.isEmpty() ? firstName : "User")
                    .lastName(lastName != null ? lastName : "")
                    .password("OIDC_USER")
                    .role(UserRole.USER)
                    .build();
            user = userRepository.save(user);
        }
        
        return MapToResponse(user);
    }

    public UserNote saveNote(String userId, String targetId, String noteDate, String content) {
        UserNote note = userNoteRepository.findByUserIdAndTargetIdAndNoteDate(userId, targetId, noteDate)
                .orElse(null);
        if (note == null) {
            note = UserNote.builder()
                    .userId(userId)
                    .targetId(targetId)
                    .noteDate(noteDate)
                    .content(content)
                    .build();
        } else {
            note.setContent(content);
        }
        return userNoteRepository.save(note);
    }

    public List<UserNote> getNotes(String userId, String targetId) {
        return userNoteRepository.findByUserIdAndTargetId(userId, targetId);
    }

    public List<String> getNoteDates(String userId) {
        return userNoteRepository.findByUserId(userId).stream()
                .map(UserNote::getNoteDate)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::MapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUserRole(String userId, UserRole role) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("no user exist with id : " + userId)
        );
        user.setRole(role);
        return MapToResponse(userRepository.save(user));
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    public UserResponse MapToResponse(User savedUser) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(savedUser.getId());
        userResponse.setEmail(savedUser.getEmail());
        userResponse.setPassword(savedUser.getPassword());
        userResponse.setLastName(savedUser.getLastName());
        userResponse.setFirstName(savedUser.getFirstName());
        userResponse.setCreatedAt(savedUser.getCreatedAt());
        userResponse.setUpdatedAt(savedUser.getUpdatedAt());
        return userResponse;
    }

    public UserResponse getUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("no user exist with id : " + userId)
        );
        return MapToResponse(user);
    }

    public Boolean getUserbyId(String userId) {
        User user = userRepository.findById(userId).orElse(
                null
        );
        if(user == null) return false;
        return true;
    }
}
