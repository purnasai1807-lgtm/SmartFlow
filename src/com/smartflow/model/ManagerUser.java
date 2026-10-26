package com.smartflow.model;

/**
 * Subclass demonstrating Inheritance and Polymorphism.
 * Project Manager role: can allocate resources and manage tasks, but not system users.
 */
public class ManagerUser extends User {

    public ManagerUser() {
        super();
        setRole("MANAGER");
    }

    public ManagerUser(int id, String username, String fullName, String email) {
        super(id, username, fullName, email, "MANAGER");
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }

    @Override
    public boolean canManageResources() {
        return true;
    }

    @Override
    public boolean canDeleteTasks() {
        return true;
    }

    @Override
    public String getRoleBadge() {
        return "[MANAGER - Task & Resource Lead]";
    }
}
