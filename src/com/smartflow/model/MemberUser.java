package com.smartflow.model;

/**
 * Subclass demonstrating Inheritance and Polymorphism.
 * Team Member role: standard contributor who works on assigned tasks.
 */
public class MemberUser extends User {

    public MemberUser() {
        super();
        setRole("MEMBER");
    }

    public MemberUser(int id, String username, String fullName, String email) {
        super(id, username, fullName, email, "MEMBER");
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }

    @Override
    public boolean canManageResources() {
        return false;
    }

    @Override
    public boolean canDeleteTasks() {
        return false;
    }

    @Override
    public String getRoleBadge() {
        return "[MEMBER - Contributor]";
    }
}
