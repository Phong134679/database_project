package com.library.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class for managing MySQL Database Connections.
 * Reads configurations from db.properties or falls back to standard defaults.
 *
 * Tách biệt hai loại kết nối:
 * 
 *   testServerConnection()} – kiểm tra kết nối tới MySQL Server (tài khoản)
 *   getConnection() kết nối tới database cụ thể
 * 
 */
public class DatabaseConnection {

    /** URL kết nối tới database library_db (có tên database). */
    private static String url = "jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    /** URL kết nối tới MySQL server (không có tên database) – dùng để kiểm tra tài khoản. */
    private static String serverUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    private static String username = "root";
    private static String password = "";
    private static String driver = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Properties props = new Properties();
            InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties");
            if (in != null) {
                props.load(in);
                url = props.getProperty("db.url", url);
                username = props.getProperty("db.username", username);
                password = props.getProperty("db.password", password);
                driver = props.getProperty("db.driver", driver);
                in.close();

                // Tự động suy ra serverUrl từ db.url (xóa phần tên database và các tham số sau dấu ?)
                // Ví dụ: jdbc:mysql://localhost:3306/library_db?... -> jdbc:mysql://localhost:3306/?...
                String dbUrl = props.getProperty("db.url", "");
                if (!dbUrl.isEmpty()) {
                    // Giữ nguyên phần query string, chỉ thay thế /<dbName> bằng /
                    serverUrl = dbUrl.replaceFirst("//([^/]+)/[^?]*", "//$1/");
                }
            }
            Class.forName(driver);
        } catch (Exception e) {
            System.err.println("[DatabaseConnection] Warning loading db.properties: " + e.getMessage());
        }
    }

    private DatabaseConnection() {
        // Private constructor to prevent instantiation
    }

    // =========================================================================
    // Kết nối tới database library_db
    // =========================================================================

    /**
     * Mở kết nối tới database library_db đã được cấu hình.
     *
     * @return Connection object
     * @throws SQLException nếu không thể kết nối
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Kiểm tra kết nối tới database library_db.
     *
     * @return {@code Connection} nếu kết nối thành công, {@code null} nếu database chưa tồn tại
     *         hoặc không thể truy cập.
     */
    public static Connection testDatabaseConnection() {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            if (conn != null && !conn.isClosed()) {
                return conn;
            }
        } catch (SQLException e) {
            // Database chưa tồn tại hoặc không thể kết nối -> trả về null
            System.err.println("[DatabaseConnection] Database connection failed: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // Kết nối tới MySQL Server (kiểm tra tài khoản)
    // =========================================================================

    /**
     * Mở kết nối tới MySQL Server mà không chỉ định database cụ thể.
     * Dùng để xác minh thông tin tài khoản (username/password) trước khi truy cập database.
     *
     * @return Connection object tới server
     * @throws SQLException nếu không thể xác thực tài khoản
     */
    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(serverUrl, username, password);
    }

    /**
     * Kiểm tra kết nối tới MySQL Server (chỉ xác thực tài khoản, không cần database tồn tại).
     *
     * @return {@code true} nếu đăng nhập tài khoản thành công, {@code false} nếu thất bại
     */
    public static boolean testServerConnection() {
        try (Connection conn = DriverManager.getConnection(serverUrl, username, password)) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Server connection test failed: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Getters / Setters
    // =========================================================================

    public static String getUrl() {
        return url;
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    public static void setCredentials(String newUrl, String newUsername, String newPassword) {
        url = newUrl;
        username = newUsername;
        password = newPassword;
        // Cập nhật lại serverUrl khi credentials thay đổi
        serverUrl = newUrl.replaceFirst("//([^/]+)/[^?]*", "//$1/");
    }
}
