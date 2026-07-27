package com.FitnessApp.UserService.service;

import com.FitnessApp.UserService.dto.RegisterRequest;
import com.FitnessApp.UserService.dto.UserResponse;
import com.FitnessApp.UserService.exception.UniqueEmailConstraintException;
import com.FitnessApp.UserService.exception.UserNotFoundException;
import com.FitnessApp.UserService.model.User;
import com.FitnessApp.UserService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    public UserResponse registerUser(RegisterRequest registerRequest) {
        User existingUser = userRepository.findByEmail(registerRequest.getEmail());
        if (existingUser != null) {
            throw new UniqueEmailConstraintException("email already exists");
        }

        User user = User.builder().
                email(registerRequest.getEmail()).
                firstName(registerRequest.getFirstName()).
                lastName(registerRequest.getLastName()).
                password(registerRequest.getPassword()).
                build();
        User savedUser = userRepository.save(user);
        return MapToResponse(savedUser);
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
