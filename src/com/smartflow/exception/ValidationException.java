package com.smartflow.exception;

/**
 * Exception thrown when input validation fails (e.g. empty fields, negative numbers).
 */
public class ValidationException extends SmartFlowException {
    private final String fieldName;

    public ValidationException(String fieldName, String message) {
        super("Validation error on [" + fieldName + "]: " + message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
