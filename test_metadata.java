import java.sql.*;
import java.util.*;

public class test_metadata {
    public static void main(String[] args) {
        try {
            // Create database and tables
            createTestDatabase();

            // Insert test data
            insertTestMetadata();

            // Retrieve test data
            retrieveTestMetadata();

            System.out.println("ReadOnlyMetaData implementation test completed successfully!");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTestDatabase() throws SQLException {
        String dbPath = "jdbc:sqlite:test_service_index.sqlite";

        try (Connection conn = DriverManager.getConnection(dbPath)) {
            // Create Package table
            String createPackageTable = """
                CREATE TABLE IF NOT EXISTS Package (
                    package_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    org TEXT NOT NULL,
                    version TEXT NOT NULL,
                    keywords TEXT
                );
                """;

            // Create ServiceReadOnlyMetaData table
            String createMetaDataTable = """
                CREATE TABLE IF NOT EXISTS ServiceReadOnlyMetaData (
                    metadata_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    package_id INTEGER,
                    service_type TEXT NOT NULL,
                    metadata_key TEXT NOT NULL,
                    display_name TEXT,
                    description TEXT,
                    FOREIGN KEY (package_id) REFERENCES Package(package_id) ON DELETE CASCADE
                );
                """;

            conn.createStatement().execute(createPackageTable);
            conn.createStatement().execute(createMetaDataTable);

            System.out.println("Database tables created successfully!");
        }
    }

    private static void insertTestMetadata() throws SQLException {
        String dbPath = "jdbc:sqlite:test_service_index.sqlite";

        try (Connection conn = DriverManager.getConnection(dbPath)) {
            // Insert test package
            String insertPackage = "INSERT INTO Package (name, org, version, keywords) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertPackage, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, "http");
                stmt.setString(2, "ballerina");
                stmt.setString(3, "2.14.0");
                stmt.setString(4, "");
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int packageId = rs.getInt(1);

                    // Insert readOnlyMetaData
                    String insertMetaData = "INSERT INTO ServiceReadOnlyMetaData (package_id, service_type, metadata_key, display_name, description) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement metaStmt = conn.prepareStatement(insertMetaData)) {
                        // HTTP service metadata
                        String[] httpMetadata = {"basePath", "port", "host", "config"};
                        for (String key : httpMetadata) {
                            metaStmt.setInt(1, packageId);
                            metaStmt.setString(2, "Service");
                            metaStmt.setString(3, key);
                            metaStmt.setString(4, key);
                            metaStmt.setString(5, "");
                            metaStmt.executeUpdate();
                        }
                    }
                }
            }

            System.out.println("Test metadata inserted successfully!");
        }
    }

    private static void retrieveTestMetadata() throws SQLException {
        String dbPath = "jdbc:sqlite:test_service_index.sqlite";

        try (Connection conn = DriverManager.getConnection(dbPath)) {
            String sql = """
                SELECT
                    metadata_key,
                    display_name,
                    description
                FROM ServiceReadOnlyMetaData srmd
                JOIN Package p ON srmd.package_id = p.package_id
                WHERE p.name = ? AND p.org = ? AND srmd.service_type = ?
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "http");
                stmt.setString(2, "ballerina");
                stmt.setString(3, "Service");

                ResultSet rs = stmt.executeQuery();
                System.out.println("Retrieved metadata:");
                while (rs.next()) {
                    System.out.println("  - " + rs.getString("metadata_key") +
                                     " (display: " + rs.getString("display_name") + ")");
                }
            }
        }
    }
}