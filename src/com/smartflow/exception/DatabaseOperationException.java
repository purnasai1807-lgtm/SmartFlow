package com.smartflow.exception;

/**
 * Exception thrown when a database or persistence error occurs.
 */
public class DatabaseOperationException extends SmartFlowException {

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
