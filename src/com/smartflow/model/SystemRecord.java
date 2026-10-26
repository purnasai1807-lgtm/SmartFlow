package com.smartflow.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulated Record / Audit Log Model.
 * Tracks historical events, audits, and status changes across the system.
 */
public class SystemRecord {
    private int id;
    private String action;
    private String entityType; // TASK, RESOURCE, USER, SYSTEM
    private int entityId;
    private String details;
    private String performedBy;
    private LocalDateTime timestamp;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SystemRecord() {
        this.timestamp = LocalDateTime.now();
    }

    public SystemRecord(String action, String entityType, int entityId, String details, String performedBy) {
        this();
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.performedBy = performedBy;
    }

    public SystemRecord(int id, String action, String entityType, int entityId, String details, String performedBy, LocalDateTime timestamp) {
        this.id = id;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FORMATTER) : "";
    }

    @Override
    public String toString() {
        return "[" + getFormattedTimestamp() + "] " + action + " by " + performedBy + ": " + details;
    }
}
