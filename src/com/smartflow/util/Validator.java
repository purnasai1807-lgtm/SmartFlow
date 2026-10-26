package com.smartflow.util;

import com.smartflow.exception.ValidationException;
import java.time.LocalDate;

/**
 * Input validation utility enforcing business rules and throwing custom exceptions.
 */
public class Validator {

    public static void requireNonEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName, fieldName + " cannot be empty.");
        }
    }

    public static void requirePositive(int number, String fieldName) throws ValidationException {
        if (number <= 0) {
            throw new ValidationException(fieldName, fieldName + " must be greater than zero.");
        }
    }

    public static void requireNonNegative(double number, String fieldName) throws ValidationException {
        if (number < 0) {
            throw new ValidationException(fieldName, fieldName + " cannot be negative.");
        }
    }

    public static void requireValidEmail(String email) throws ValidationException {
        requireNonEmpty(email, "Email");
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Email", "Invalid email address format.");
        }
    }

    public static void validateTaskDates(LocalDate dueDate) throws ValidationException {
        if (dueDate == null) {
            throw new ValidationException("Due Date", "Due date must be specified.");
        }
    }
}
