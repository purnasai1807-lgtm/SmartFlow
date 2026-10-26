package com.smartflow.model;

/**
 * Priority levels for tasks.
 * Includes ranking value for Collections PriorityQueue sorting.
 */
public enum TaskPriority {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");

    private final int rank;
    private final String displayName;

    TaskPriority(int rank, String displayName) {
        this.rank = rank;
        this.displayName = displayName;
    }

    public int getRank() {
        return rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
