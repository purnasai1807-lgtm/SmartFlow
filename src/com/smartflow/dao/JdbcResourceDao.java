package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.Resource;
import com.smartflow.model.ResourceType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Implementation for Resource management.
 */
public class JdbcResourceDao implements ResourceDao {
    private final DatabaseManager dbManager;

    public JdbcResourceDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public List<Resource> findAll() throws DatabaseOperationException {
        List<Resource> list = new ArrayList<>();
        String sql = "SELECT * FROM resources ORDER BY id ASC;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch resources: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Resource findById(int id) throws DatabaseOperationException {
        String sql = "SELECT * FROM resources WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToResource(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching resource #" + id, e);
        }
        return null;
    }

    @Override
    public int save(Resource resource) throws DatabaseOperationException {
        String sql = "INSERT INTO resources (name, type, total_capacity, allocated_quantity, hourly_cost, location) "
                + "VALUES (?, ?, ?, ?, ?, ?);";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, resource.getName());
            stmt.setString(2, resource.getType().name());
            stmt.setInt(3, resource.getTotalCapacity());
            stmt.setInt(4, resource.getAllocatedQuantity());
            stmt.setDouble(5, resource.getHourlyCost());
            stmt.setString(6, resource.getLocation());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        resource.setId(keys.getInt(1));
                        return resource.getId();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to insert resource: " + e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public boolean update(Resource resource) throws DatabaseOperationException {
        String sql = "UPDATE resources SET name = ?, type = ?, total_capacity = ?, "
                + "allocated_quantity = ?, hourly_cost = ?, location = ? WHERE id = ?;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, resource.getName());
            stmt.setString(2, resource.getType().name());
            stmt.setInt(3, resource.getTotalCapacity());
            stmt.setInt(4, resource.getAllocatedQuantity());
            stmt.setDouble(5, resource.getHourlyCost());
            stmt.setString(6, resource.getLocation());
            stmt.setInt(7, resource.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to update resource #" + resource.getId(), e);
        }
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        String sql = "DELETE FROM resources WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete resource #" + id, e);
        }
    }

    @Override
    public boolean adjustAllocation(int resourceId, int delta) throws DatabaseOperationException {
        String sql = "UPDATE resources SET allocated_quantity = MAX(0, allocated_quantity + ?) WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, resourceId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to adjust resource allocation for #" + resourceId, e);
        }
    }

    private Resource mapResultSetToResource(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setId(rs.getInt("id"));
        r.setName(rs.getString("name"));
        r.setType(ResourceType.valueOf(rs.getString("type")));
        r.setTotalCapacity(rs.getInt("total_capacity"));
        r.setAllocatedQuantity(rs.getInt("allocated_quantity"));
        r.setHourlyCost(rs.getDouble("hourly_cost"));
        r.setLocation(rs.getString("location"));
        return r;
    }
}
