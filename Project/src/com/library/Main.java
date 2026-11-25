package com.library;

import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Main application entry point.
 * <p>
 * Thực hiện hai kiểm tra kết nối:
 * <ol>
 *   <li>Kết nối tài khoản MySQL Server (xác thực username/password).</li>
 *   <li>Kết nối tới database {@code library_db} trong tài khoản đó.</li>
 * </ol>
 * Business logic được xử lý qua {@link com.library.api.ApiServer}
 * kết hợp với Stored Procedures và Views trên MySQL.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("  DIGITAL LIBRARY MANAGEMENT SYSTEM & OPEN REPOSITORY - BACKEND               ");
        System.out.println("===============================================================================");

        // 1. Kiểm tra kết nối đăng nhập tài khoản MySQL Server
        try {
            System.out.print("[1] Kiểm tra kết nối tài khoản MySQL: ");

            try (Connection serverConn = DatabaseConnection.getServerConnection()) {
                if (serverConn == null || serverConn.isClosed()) {
                    throw new SQLException("Không thể kết nối đến tài khoản MySQL Server.");
                }

                System.out.println("THÀNH CÔNG!");
            }

        } catch (Exception e) {
            System.err.println("\n[LỖI] Không thể kết nối tài khoản MySQL: " + e.getMessage());
            return;
        }

        // 2. Kiểm tra kết nối tới database library_db
        System.out.print("[2] Kiểm tra kết nối tới database library_db: ");

        Connection dbConn = DatabaseConnection.testDatabaseConnection();

        if (dbConn == null) {
            System.err.println("THẤT BẠI!");
            System.err.println("    -> Database chưa tồn tại hoặc không thể kết nối.");
            System.err.println("    -> Vui lòng khởi tạo CSDL thủ công và chạy lại ứng dụng!");
            return;
        }

        System.out.println("THÀNH CÔNG! (URL: " + DatabaseConnection.getUrl() + ")");
        dbConn.close();

        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("  Tất cả kết nối sẵn sàng. Khởi động API Server bằng: com.library.api.ApiServer");
        System.out.println("===============================================================================");
    }
}
