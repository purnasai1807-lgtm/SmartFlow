package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.AdminUser;
import com.smartflow.model.ManagerUser;
import com.smartflow.model.MemberUser;
import com.smartflow.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User persistence with Polymorphic user creation.
 */
public class JdbcUserDao implements UserDao {
    private final DatabaseManager dbManager;

    public JdbcUserDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public List<User> findAll() throws DatabaseOperationException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to query users: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public User findById(int id) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find user by ID " + id, e);
        }
        return null;
    }

    @Override
    public User findByUsername(String username) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by username: " + username, e);
        }
        return null;
    }

    @Override
    public int save(User user) throws DatabaseOperationException {
        String sql = "INSERT INTO users (username, full_name, email, role) VALUES (?, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getRole());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                        return user.getId();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to create user: " + e.getMessage(), e);
        }
        return -1;
    }

    // Polymorphic instantiation based on user role
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String role = rs.getString("role");

        User user;
        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new AdminUser(id, username, fullName, email);
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            user = new ManagerUser(id, username, fullName, email);
        } else {
            user = new MemberUser(id, username, fullName, email);
        }
        return user;
    }
}
