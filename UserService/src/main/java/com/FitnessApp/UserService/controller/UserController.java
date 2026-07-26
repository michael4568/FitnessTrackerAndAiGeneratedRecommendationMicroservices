package com.FitnessApp.UserService.controller;

import com.FitnessApp.UserService.dto.RegisterRequest;
import com.FitnessApp.UserService.dto.UserResponse;
import com.FitnessApp.UserService.model.User;
import com.FitnessApp.UserService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


}
