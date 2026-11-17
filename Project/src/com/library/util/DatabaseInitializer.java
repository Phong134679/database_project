package com.library.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/**
 * Utility class to initialize library_db and execute schema.sql / sample_data.sql
 * directly through JDBC.
 *
 * <p>Hỗ trợ hai chế độ:
 * <ul>
 *   <li>{@link #createDatabaseAndSchema(Connection, String)} – chỉ tạo database + schema (không nạp data)</li>
 *   <li>{@link #loadSampleData(Connection, String)} – nạp dữ liệu mẫu vào database đang mở</li>
 *   <li>{@link #isDatabaseEmpty(Connection)} – kiểm tra database có rỗng không</li>
 *   <li>{@link #initializeDatabase(String, String)} – khởi tạo toàn bộ (giữ tương thích cũ)</li>
 * </ul>
 */
public class DatabaseInitializer {

    // =========================================================================
    // Public API mới – tách biệt từng bước
    // =========================================================================

    /**
     * Kiểm tra database library_db có rỗng không (không có bảng nào, hoặc bảng PERSON trống).
     * Dùng để quyết định có cần nạp dữ liệu mẫu hay không.
     *
     * @param conn kết nối đang mở tới library_db
     * @return {@code true} nếu database rỗng (chưa có dữ liệu), {@code false} nếu đã có dữ liệu
     */
    public static boolean isDatabaseEmpty(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Kiểm tra số bảng trong library_db
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';"
            );
            if (rs.next() && rs.getInt(1) == 0) {
                return true; // Chưa có bảng nào -> rỗng
            }

            // Có bảng rồi, kiểm tra bảng PERSON có dữ liệu không (bảng gốc/trung tâm)
            try {
                ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM PERSON;");
                if (rsCount.next()) {
                    return rsCount.getInt(1) == 0;
                }
            } catch (Exception ignored) {
                // Bảng PERSON chưa tồn tại -> coi như rỗng
                return true;
            }
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] isDatabaseEmpty error: " + e.getMessage());
        }
        return true;
    }

    /**
     * Tạo database library_db (nếu chưa có) và chạy schema.sql.
     * Dùng khi kết nối tới database trả về {@code null}.
     *
     * @param serverConn kết nối tới MySQL server (không có database name)
     * @param schemaPath đường dẫn tới file schema.sql
     * @return {@code true} nếu thành công
     */
    public static boolean createDatabaseAndSchema(Connection serverConn, String schemaPath) {
        try (Statement stmt = serverConn.createStatement()) {
            System.out.println("[DatabaseInitializer] Tạo database library_db (nếu chưa tồn tại)...");
            stmt.execute("CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            stmt.execute("USE library_db;");

            if (schemaPath != null && new File(schemaPath).exists()) {
                System.out.println("[DatabaseInitializer] Đang chạy schema.sql (" + schemaPath + ")...");
                executeSqlScript(stmt, schemaPath);
                System.out.println("[DatabaseInitializer] Schema khởi tạo thành công.");
            }
            return true;
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Lỗi tạo database/schema: " + e.getMessage());
            return false;
        }
    }

    /**
     * Nạp dữ liệu mẫu vào database library_db đang kết nối.
     * Có thể dùng để nạp lần đầu (database rỗng) hoặc ghi đè (database đã có data).
     *
     * @param dbConn   kết nối đang mở tới library_db
     * @param dataPath đường dẫn tới file sample_data.sql
     * @return {@code true} nếu nạp thành công
     */
    public static boolean loadSampleData(Connection dbConn, String dataPath) {
        if (dataPath == null || !new File(dataPath).exists()) {
            System.err.println("[DatabaseInitializer] Không tìm thấy file: " + dataPath);
            return false;
        }
        try (Statement stmt = dbConn.createStatement()) {
            System.out.println("[DatabaseInitializer] Đang nạp dữ liệu từ " + dataPath + "...");
            executeSqlScript(stmt, dataPath);
            System.out.println("[DatabaseInitializer] Nạp dữ liệu mẫu thành công.");
            return true;
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Lỗi nạp dữ liệu: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Legacy API – giữ tương thích với code cũ
    // =========================================================================

    /**
     * Khởi tạo toàn bộ database: tạo schema rồi nạp dữ liệu mẫu trong một lần gọi.
     * Giữ nguyên để tương thích với các lời gọi cũ.
     *
     * @param schemaPath đường dẫn file schema.sql
     * @param dataPath   đường dẫn file sample_data.sql
     * @return {@code true} nếu hoàn tất thành công
     */
    public static boolean initializeDatabase(String schemaPath, String dataPath) {
        String username = "root";
        String password = "";

        try {
            Properties props = new Properties();
            InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties");
            if (in != null) {
                props.load(in);
                username = props.getProperty("db.username", username);
                password = props.getProperty("db.password", password);
                in.close();
            }
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Error reading db.properties: " + e.getMessage());
        }

        System.out.println("[DatabaseInitializer] Đang kết nối MySQL server để khởi tạo database...");
        try (Connection serverConn = DriverManager.getConnection(DatabaseConnection.getServerUrl(), username, password)) {
            boolean schemaOk = createDatabaseAndSchema(serverConn, schemaPath);
            if (!schemaOk) return false;

            // Sau khi tạo schema, kết nối lại vào library_db để nạp data
            try (Connection dbConn = DatabaseConnection.getConnection()) {
                return loadSampleData(dbConn, dataPath);
            }
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Initialization failed: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Internal helper
    // =========================================================================

    private static void executeSqlScript(Statement stmt, String filePath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Loại bỏ inline comments
                int commentIdx = line.indexOf("--");
                if (commentIdx >= 0) {
                    line = line.substring(0, commentIdx).trim();
                }
                if (line.startsWith("//") || line.isEmpty()) {
                    continue;
                }
                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                    sb.setLength(0);
                }
            }
        }
    }

    public static void main(String[] args) {
        String schema = "Project/schema.sql";
        String sampleData = "Project/sample_data.sql";

        if (!new File(schema).exists()) {
            schema = "schema.sql";
        }
        if (!new File(sampleData).exists()) {
            sampleData = "sample_data.sql";
        }

        boolean success = initializeDatabase(schema, sampleData);
        if (success) {
            System.out.println("[DatabaseInitializer] HOÀN TẤT KHỞI TẠO CƠ SỞ DỮ LIỆU THÀNH CÔNG!");
        } else {
            System.err.println("[DatabaseInitializer] KHỞI TẠO THẤT BẠI.");
        }
    }
}
