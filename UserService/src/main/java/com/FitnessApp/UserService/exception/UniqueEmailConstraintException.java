package com.FitnessApp.UserService.exception;

public class UniqueEmailConstraintException extends RuntimeException {
    public UniqueEmailConstraintException(String message) {
        super(message);
    }
}
