package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.Task;
import com.smartflow.model.TaskPriority;
import com.smartflow.model.TaskStatus;
import com.smartflow.util.DateUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Implementation for Task persistence.
 * Uses PreparedStatements, parameterized queries, and SQL transactions.
 */
public class JdbcTaskDao implements TaskDao {
    private final DatabaseManager dbManager;

    public JdbcTaskDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public List<Task> findAll() throws DatabaseOperationException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT t.*, u.full_name AS user_name, r.name AS res_name "
                + "FROM tasks t "
                + "LEFT JOIN users u ON t.assigned_user_id = u.id "
                + "LEFT JOIN resources r ON t.resource_id = r.id "
                + "ORDER BY t.id DESC;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to retrieve tasks from database: " + e.getMessage(), e);
        }
        return tasks;
    }

    @Override
    public Task findById(int id) throws DatabaseOperationException {
        String sql = "SELECT t.*, u.full_name AS user_name, r.name AS res_name "
                + "FROM tasks t "
                + "LEFT JOIN users u ON t.assigned_user_id = u.id "
                + "LEFT JOIN resources r ON t.resource_id = r.id "
                + "WHERE t.id = ?;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTask(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find task with ID " + id, e);
        }
        return null;
    }

    @Override
    public int save(Task task) throws DatabaseOperationException {
        String sql = "INSERT INTO tasks (title, description, priority, status, assigned_user_id, resource_id, due_date, estimated_hours, progress) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getPriority().name());
            stmt.setString(4, task.getStatus().name());
            stmt.setInt(5, task.getAssignedUserId());
            stmt.setInt(6, task.getResourceId());
            stmt.setString(7, DateUtil.format(task.getDueDate()));
            stmt.setInt(8, task.getEstimatedHours());
            stmt.setInt(9, task.getProgress());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        task.setId(keys.getInt(1));
                        return task.getId();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to create new task: " + e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public boolean update(Task task) throws DatabaseOperationException {
        String sql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, "
                + "assigned_user_id = ?, resource_id = ?, due_date = ?, estimated_hours = ?, progress = ? "
                + "WHERE id = ?;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getPriority().name());
            stmt.setString(4, task.getStatus().name());
            stmt.setInt(5, task.getAssignedUserId());
            stmt.setInt(6, task.getResourceId());
            stmt.setString(7, DateUtil.format(task.getDueDate()));
            stmt.setInt(8, task.getEstimatedHours());
            stmt.setInt(9, task.getProgress());
            stmt.setInt(10, task.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update task #" + task.getId(), e);
        }
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        String sql = "DELETE FROM tasks WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete task #" + id, e);
        }
    }

    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setPriority(TaskPriority.valueOf(rs.getString("priority")));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        task.setAssignedUserId(rs.getInt("assigned_user_id"));
        task.setAssignedUserName(rs.getString("user_name"));
        task.setResourceId(rs.getInt("resource_id"));
        task.setResourceName(rs.getString("res_name"));
        task.setDueDate(DateUtil.parse(rs.getString("due_date")));
        task.setEstimatedHours(rs.getInt("estimated_hours"));
        task.setProgress(rs.getInt("progress"));
        return task;
    }
}
