package com.FitnessApp.UserService.controller;

import com.FitnessApp.UserService.dto.RegisterRequest;
import com.FitnessApp.UserService.dto.UserResponse;
import com.FitnessApp.UserService.model.User;
import com.FitnessApp.UserService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.FitnessApp.UserService.model.UserNote;
import com.FitnessApp.UserService.model.UserRole;
import lombok.Data;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        UserResponse userResponse = userService.registerUser(registerRequest);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userId){
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/validate/{userId}")
    public ResponseEntity<Boolean> validateUser(@PathVariable String userId){
        return ResponseEntity.ok(userService.getUserbyId(userId));
    }

    @PostMapping("/sync")
    public ResponseEntity<UserResponse> syncUser(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName) {
        
        System.out.println("=== SYNC USER CALLED ===");
        System.out.println("Received X-User-Id: '" + userId + "'");
        System.out.println("Received X-User-Email: '" + email + "'");
        
        return ResponseEntity.ok(userService.syncUser(userId, email, firstName, lastName));
    }

    @PostMapping("/notes")
    public ResponseEntity<UserNote> saveNote(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UserNoteSaveRequest request) {
        return ResponseEntity.ok(userService.saveNote(userId, request.getTargetId(), request.getNoteDate(), request.getContent()));
    }

    @GetMapping("/notes")
    public ResponseEntity<List<UserNote>> getNotes(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("targetId") String targetId) {
        return ResponseEntity.ok(userService.getNotes(userId, targetId));
    }

    @GetMapping("/notes/dates")
    public ResponseEntity<List<String>> getNoteDates(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(userService.getNoteDates(userId));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/admin/users/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable("userId") String userId,
            @RequestParam("role") UserRole role) {
        return ResponseEntity.ok(userService.updateUserRole(userId, role));
    }

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class UserNoteSaveRequest {
        private String targetId;
        private String noteDate;
        private String content;
    }
}

