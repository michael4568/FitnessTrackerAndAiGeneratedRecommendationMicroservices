package com.fitness.aiService.exception;

public class UniqueEmailConstraintException extends RuntimeException {
    public UniqueEmailConstraintException(String message) {
        super(message);
    }
}
