package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.SystemRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC persistence for audit logs.
 */
public class JdbcRecordDao implements RecordDao {
    private final DatabaseManager dbManager;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JdbcRecordDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public List<SystemRecord> findAll() throws DatabaseOperationException {
        List<SystemRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM audit_records ORDER BY id DESC LIMIT 200;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                LocalDateTime ldt;
                try {
                    ldt = LocalDateTime.parse(rs.getString("timestamp"), FORMATTER);
                } catch (Exception e) {
                    ldt = LocalDateTime.now();
                }

                records.add(new SystemRecord(
                        rs.getInt("id"),
                        rs.getString("action"),
                        rs.getString("entity_type"),
                        rs.getInt("entity_id"),
                        rs.getString("details"),
                        rs.getString("performed_by"),
                        ldt
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch audit records: " + e.getMessage(), e);
        }
        return records;
    }

    @Override
    public void log(SystemRecord record) throws DatabaseOperationException {
        String sql = "INSERT INTO audit_records (action, entity_type, entity_id, details, performed_by, timestamp) "
                + "VALUES (?, ?, ?, ?, ?, ?);";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, record.getAction());
            stmt.setString(2, record.getEntityType());
            stmt.setInt(3, record.getEntityId());
            stmt.setString(4, record.getDetails());
            stmt.setString(5, record.getPerformedBy());
            stmt.setString(6, record.getFormattedTimestamp());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to record audit log: " + e.getMessage(), e);
        }
    }

    @Override
    public void clearAll() throws DatabaseOperationException {
        String sql = "DELETE FROM audit_records;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to clear audit records: " + e.getMessage(), e);
        }
    }
}
