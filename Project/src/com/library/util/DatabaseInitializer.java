package com.library.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 * Utility class to initialize library_db and execute schema.sql / sample_data.sql
 * directly through JDBC.
 */
public class DatabaseInitializer {

    public static boolean initializeDatabase(String schemaPath, String dataPath) {
        String baseUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";
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

        System.out.println("[DatabaseInitializer] Connecting to MySQL server to initialize database...");
        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement stmt = conn.createStatement()) {

            // 1. Tạo Database library_db
            System.out.println("[DatabaseInitializer] Creating database library_db if not exists...");
            stmt.execute("CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            stmt.execute("USE library_db;");

            // 2. Chạy schema.sql
            if (schemaPath != null && new File(schemaPath).exists()) {
                System.out.println("[DatabaseInitializer] Executing schema.sql (" + schemaPath + ")...");
                executeSqlScript(stmt, schemaPath);
                System.out.println("[DatabaseInitializer] Schema created successfully.");
            }

            // 3. Chạy sample_data.sql
            if (dataPath != null && new File(dataPath).exists()) {
                System.out.println("[DatabaseInitializer] Executing sample_data.sql (" + dataPath + ")...");
                executeSqlScript(stmt, dataPath);
                System.out.println("[DatabaseInitializer] Sample data loaded successfully.");
            }

            return true;
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Initialization failed: " + e.getMessage());
            return false;
        }
    }

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
