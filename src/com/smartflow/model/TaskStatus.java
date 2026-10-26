package com.smartflow.model;

/**
 * Lifecycle states for a project task.
 */
public enum TaskStatus {
    BACKLOG("Backlog"),
    IN_PROGRESS("In Progress"),
    REVIEW("In Review"),
    COMPLETED("Completed");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
