package com.library.test;

import com.library.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  KIỂM TRA DỮ LIỆU THỰC TẾ TRONG MYSQL DATABASE  ");
        System.out.println("=================================================");
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Kết nối MySQL 'library_db' THÀNH CÔNG!");
            String[] tables = {"RESOURCE", "PERSON", "UNIVERSITY_MEMBER", "BOOK_COPY", "BORROW", "FINE", "DIGITAL_JOURNAL", "ACCESS_LOG"};
            for (String t : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    if (rs.next()) {
                        System.out.printf("  - Bảng %-18s: %d bản ghi%n", t, rs.getInt(1));
                    }
                } catch (Exception ex) {
                    System.out.printf("  - Bảng %-18s: Lỗi: %s%n", t, ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối MySQL: " + e.getMessage());
        }
    }
}
