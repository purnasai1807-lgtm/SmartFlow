package com.smartflow.model;

/**
 * Abstract Base Class demonstrating Encapsulation and Inheritance.
 * Represents a generic user in the SmartFlow system.
 */
public abstract class User {
    private int id;
    private String username;
    private String fullName;
    private String email;
    private String role;

    // Default Constructor
    public User() {
    }

    // Parameterized Constructor (Constructors)
    public User(int id, String username, String fullName, String email, String role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    // Abstract method to be overridden by subclasses (Polymorphism)
    public abstract boolean canManageUsers();
    public abstract boolean canManageResources();
    public abstract boolean canDeleteTasks();
    public abstract String getRoleBadge();

    public boolean canClearAuditLogs() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    // Getters and Setters (Encapsulation)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
