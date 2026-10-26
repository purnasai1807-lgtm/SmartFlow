import com.smartflow.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Smoke test for SmartFlow's existing SQLite connection. Run from the project root. */
public class DatabaseConnectionTest {
    public static void main(String[] args) {
        String[] tables = {"users", "tasks", "resources", "audit_records"};
        try (Connection connection = DatabaseManager.getInstance().getConnection();
             Statement statement = connection.createStatement()) {
            System.out.println("SQLite connection: SUCCESS");
            for (String table : tables) {
                try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    System.out.println(table + " rows: " + (rs.next() ? rs.getInt(1) : 0));
                }
            }
        } catch (Exception e) {
            System.err.println("SQLite connection: FAILED");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
