package com.FitnessApp.UserService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    private String firstName;
    private String lastName;
    @NotBlank(message = "email must not be empty")
    @Email(message ="invalid Email")
    private String email;
    @NotBlank(message = "Password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,32}$",
            message = "Password must be 8-32 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String password;
}
