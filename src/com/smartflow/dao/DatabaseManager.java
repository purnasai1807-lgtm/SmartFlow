package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC Database Connection Manager.
 * Uses portable, embedded SQLite database that creates schema & seed data automatically.
 */
public class DatabaseManager {
    private static final String DB_FILE = "smartflow.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeSchema();
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found on classpath: " + e.getMessage());
        } catch (DatabaseOperationException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void initializeSchema() throws DatabaseOperationException {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE NOT NULL, "
                + "full_name TEXT NOT NULL, "
                + "email TEXT NOT NULL, "
                + "role TEXT NOT NULL"
                + ");";

        String createResourcesTable = "CREATE TABLE IF NOT EXISTS resources ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "type TEXT NOT NULL, "
                + "total_capacity INTEGER NOT NULL, "
                + "allocated_quantity INTEGER NOT NULL DEFAULT 0, "
                + "hourly_cost REAL NOT NULL DEFAULT 0.0, "
                + "location TEXT"
                + ");";

        String createTasksTable = "CREATE TABLE IF NOT EXISTS tasks ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT NOT NULL, "
                + "description TEXT, "
                + "priority TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "assigned_user_id INTEGER, "
                + "resource_id INTEGER, "
                + "due_date TEXT NOT NULL, "
                + "estimated_hours INTEGER DEFAULT 0, "
                + "progress INTEGER DEFAULT 0, "
                + "FOREIGN KEY(assigned_user_id) REFERENCES users(id), "
                + "FOREIGN KEY(resource_id) REFERENCES resources(id)"
                + ");";

        String createRecordsTable = "CREATE TABLE IF NOT EXISTS audit_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "action TEXT NOT NULL, "
                + "entity_type TEXT NOT NULL, "
                + "entity_id INTEGER NOT NULL, "
                + "details TEXT, "
                + "performed_by TEXT NOT NULL, "
                + "timestamp TEXT NOT NULL"
                + ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createResourcesTable);
            stmt.execute(createTasksTable);
            stmt.execute(createRecordsTable);
            seedInitialDataIfEmpty(conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to initialize database tables: " + e.getMessage(), e);
        }
    }

    /**
     * Creates only the three requested user profiles. Tasks, resources, and audit
     * records start empty so the dashboard does not present invented activity.
     */
    private void seedInitialDataIfEmpty(Connection conn) throws SQLException {
        try (Statement checkStmt = conn.createStatement();
             var rs = checkStmt.executeQuery("SELECT COUNT(*) AS total FROM users;")) {
            if (rs.next() && rs.getInt("total") == 0) {
                checkStmt.execute("INSERT INTO users (username, full_name, email, role) VALUES "
                        + "('admin', 'Dr. Kumar', 'dr.kumar@example.com', 'ADMIN'), "
                        + "('younus_mgr', 'Younus', 'younus@example.com', 'MANAGER'), "
                        + "('purna_member', 'Purna', 'purna@example.com', 'MEMBER');");
            }
        }

        try (Statement checkStmt = conn.createStatement();
             var rs = checkStmt.executeQuery("SELECT COUNT(*) AS total FROM resources;")) {
            if (rs.next() && rs.getInt("total") == 0) {
                checkStmt.execute("INSERT INTO resources (name, type, total_capacity, allocated_quantity, hourly_cost, location) VALUES "
                        + "('AWS Cloud GPU Cluster (A100)', 'CLOUD_SERVER', 2, 0, 45.0, 'us-east-1'), "
                        + "('Jenkins CI/CD Build Node', 'SOFTWARE_LICENSE', 3, 0, 15.0, 'Local Lab Server'), "
                        + "('Hardware Prototyping Workstation', 'HARDWARE', 1, 0, 25.0, 'Embedded IoT Lab Rm 204'), "
                        + "('Collaborative Workspace Bay', 'WORKSPACE', 4, 0, 10.0, 'Building B - Bay 3');");
            }
        }
    }

}
