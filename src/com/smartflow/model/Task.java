package com.smartflow.model;

import java.time.LocalDate;

/**
 * Encapsulated Task Model.
 * Represents work items, assignments, deadlines, and assigned resources.
 */
public class Task {
    private int id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private int assignedUserId;
    private String assignedUserName;
    private int resourceId;
    private String resourceName;
    private LocalDate dueDate;
    private int estimatedHours;
    private int progress; // 0 to 100%

    // Default Constructor
    public Task() {
        this.priority = TaskPriority.MEDIUM;
        this.status = TaskStatus.BACKLOG;
        this.progress = 0;
    }

    // Overloaded Constructor (Constructor Overloading)
    public Task(String title, String description, TaskPriority priority, LocalDate dueDate) {
        this();
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    // Full Parameterized Constructor
    public Task(int id, String title, String description, TaskPriority priority, TaskStatus status,
                int assignedUserId, String assignedUserName, int resourceId, String resourceName,
                LocalDate dueDate, int estimatedHours, int progress) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assignedUserId = assignedUserId;
        this.assignedUserName = assignedUserName;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.dueDate = dueDate;
        this.estimatedHours = estimatedHours;
        this.progress = progress;
    }

    public boolean isOverdue() {
        if (dueDate == null || status == TaskStatus.COMPLETED) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }

    public boolean isDueSoon() {
        if (dueDate == null || status == TaskStatus.COMPLETED) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return !now.isAfter(dueDate) && dueDate.minusDays(2).isBefore(now);
    }

    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.COMPLETED) {
            this.progress = 100;
        }
    }

    public int getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(int assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public String getAssignedUserName() {
        return assignedUserName;
    }

    public void setAssignedUserName(String assignedUserName) {
        this.assignedUserName = assignedUserName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(int estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        if (this.progress == 100 && this.status != TaskStatus.COMPLETED) {
            this.status = TaskStatus.COMPLETED;
        }
    }

    @Override
    public String toString() {
        return "[" + priority + "] " + title + " (" + status + ")";
    }
}
