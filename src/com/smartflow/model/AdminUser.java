package com.smartflow.model;

/**
 * Subclass demonstrating Inheritance and Polymorphism.
 * Administrator role with full access permissions.
 */
public class AdminUser extends User {

    public AdminUser() {
        super();
        setRole("ADMIN");
    }

    public AdminUser(int id, String username, String fullName, String email) {
        super(id, username, fullName, email, "ADMIN");
    }

    @Override
    public boolean canManageUsers() {
        return true;
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
        return "[ADMIN - Full Access]";
    }
}
