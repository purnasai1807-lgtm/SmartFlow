package com.smartflow.exception;

/**
 * Exception thrown when a resource is over-allocated or conflicting.
 */
public class ResourceConflictException extends SmartFlowException {
    private final int resourceId;

    public ResourceConflictException(int resourceId, String message) {
        super("Resource Conflict (ID: " + resourceId + "): " + message);
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }
}
