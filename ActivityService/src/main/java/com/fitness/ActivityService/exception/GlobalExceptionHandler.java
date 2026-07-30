package com.fitness.ActivityService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({UserNotFoundException.class , ActivityNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(Exception ex) {
        Map<String, Object> errorDetails = new HashMap<>();

        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("status", HttpStatus.BAD_REQUEST.value());
        errorDetails.put("error", "User Validation Failed");
        errorDetails.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    @ExceptionHandler(UniqueEmailConstraintException.class)
    public ResponseEntity<Map<String, Object>> handleEmailConstraint(UniqueEmailConstraintException ex) {
        Map<String, Object> errorDetails = new HashMap<>();

        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("status", HttpStatus.CONFLICT.value());
        errorDetails.put("error", "Conflict"); // Standard HTTP error name
        errorDetails.put("reason", "Email already exists"); // Described cleanly
        errorDetails.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleNotValidArgument(MethodArgumentNotValidException ex) {
        Map<String, Object> errorDetails = new HashMap<>();

        // Map individual field validation errors (e.g., "email": "email must not be empty")
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("status", HttpStatus.BAD_REQUEST.value());
        errorDetails.put("error", "Bad Request");
        errorDetails.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllOtherExceptions(Exception ex) {
        ex.printStackTrace(); // Log the exact error to the console!
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", ex.getClass().getName() + ": " + ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }
}
