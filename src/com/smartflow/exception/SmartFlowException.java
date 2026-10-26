package com.smartflow.exception;

/**
 * Base Exception Class for SmartFlow application.
 * Demonstrates custom checked exception hierarchy in Java.
 */
public class SmartFlowException extends Exception {

    public SmartFlowException(String message) {
        super(message);
    }

    public SmartFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
