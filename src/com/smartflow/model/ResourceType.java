package com.smartflow.model;

/**
 * Types of resources that can be allocated to tasks.
 */
public enum ResourceType {
    HARDWARE("Hardware Equipment"),
    SOFTWARE_LICENSE("Software License"),
    CLOUD_SERVER("Cloud Server"),
    WORKSPACE("Workstation/Lab");

    private final String label;

    ResourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
