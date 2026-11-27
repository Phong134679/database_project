package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.Resource;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling data queries for general RESOURCE catalog via direct JDBC.
 */
public class ResourceRepository {

    public Resource findById(int resourceId) {
        String sql = "SELECT resource_id, title, topic, is_public FROM RESOURCE WHERE resource_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Resource(
                            rs.getInt("resource_id"),
                            rs.getString("title"),
                            rs.getString("topic"),
                            rs.getBoolean("is_public")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu tài nguyên ID " + resourceId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public Integer findResourceIdByCopyId(String copyId) {
        String sql = "SELECT resource_id FROM BOOK_COPY WHERE copy_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("resource_id");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu resource_id của bản sao " + copyId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public boolean isAuthorOfResource(Connection conn, int personId, int resourceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AUTHOR_OF WHERE person_id = ? AND resource_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int countRequiredLicenses(Connection conn, int resourceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM RESOURCE_LICENSE WHERE resource_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves all resources directly from RESOURCE, PHYSICAL_BOOK, DIGITAL_JOURNAL,
     * RESOURCE_LICENSE, and BOOK_COPY without using VIEW_RESOURCES_DETAILED.
     */
    public List<Map<String, Object>> findAllResourcesDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT r.resource_id, r.title, r.topic, r.is_public, " +
                     "       COALESCE(rl_agg.required_license_count, 0) AS required_license_count, " +
                     "       pb.shelf_location, " +
                     "       COALESCE(bc_agg.total_copies, 0) AS total_copies, " +
                     "       COALESCE(bc_agg.available_copies, 0) AS available_copies, " +
                     "       dj.url AS journal_url, dj.file_size, dj.format AS journal_format, " +
                     "       (pb.resource_id IS NOT NULL) AS is_physical_book, " +
                     "       (dj.resource_id IS NOT NULL) AS is_digital_journal " +
                     "FROM RESOURCE r " +
                     "LEFT JOIN PHYSICAL_BOOK pb ON pb.resource_id = r.resource_id " +
                     "LEFT JOIN DIGITAL_JOURNAL dj ON dj.resource_id = r.resource_id " +
                     "LEFT JOIN ( " +
                     "    SELECT resource_id, COUNT(*) AS required_license_count " +
                     "    FROM RESOURCE_LICENSE " +
                     "    GROUP BY resource_id " +
                     ") rl_agg ON rl_agg.resource_id = r.resource_id " +
                     "LEFT JOIN ( " +
                     "    SELECT resource_id, COUNT(*) AS total_copies, " +
                     "           SUM(CASE WHEN availability_status = 'Available' THEN 1 ELSE 0 END) AS available_copies " +
                     "    FROM BOOK_COPY " +
                     "    GROUP BY resource_id " +
                     ") bc_agg ON bc_agg.resource_id = r.resource_id " +
                     "ORDER BY r.resource_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                boolean isPhysicalBook = rs.getBoolean("is_physical_book");
                boolean isDigitalJournal = rs.getBoolean("is_digital_journal");
                String type;
                if (isPhysicalBook && isDigitalJournal) {
                    type = "BOTH";
                } else if (isPhysicalBook) {
                    type = "PHYSICAL_BOOK";
                } else if (isDigitalJournal) {
                    type = "DIGITAL_JOURNAL";
                } else {
                    type = "UNKNOWN";
                }

                map.put("resourceId", rs.getInt("resource_id"));
                map.put("title", rs.getString("title"));
                map.put("topic", rs.getString("topic"));
                map.put("isPhysicalBook", isPhysicalBook);
                map.put("isDigitalJournal", isDigitalJournal);
                map.put("resourceType", type);
                map.put("isPublic", rs.getBoolean("is_public"));
                map.put("requiredLicenseCount", rs.getInt("required_license_count"));

                if (isPhysicalBook) {
                    map.put("shelfLocation", rs.getString("shelf_location"));
                    map.put("totalCopies", rs.getInt("total_copies"));
                    map.put("availableCopies", rs.getInt("available_copies"));
                }
                if (isDigitalJournal) {
                    map.put("url", rs.getString("journal_url"));
                    map.put("fileSize", rs.getInt("file_size"));
                    map.put("format", rs.getString("journal_format"));
                }
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất danh mục tài nguyên: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Map<String, Object>> findCopiesByResourceId(int resourceId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT copy_id, resource_id, availability_status " +
                     "FROM BOOK_COPY WHERE resource_id = ? ORDER BY copy_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("copyId", rs.getString("copy_id"));
                    map.put("resourceId", rs.getInt("resource_id"));
                    map.put("availabilityStatus", rs.getString("availability_status"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu danh sách bản sao cho sách " + resourceId + ": " + e.getMessage(), e);
        }
        return list;
    }
}
