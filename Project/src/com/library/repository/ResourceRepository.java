package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.Resource;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling data queries for general RESOURCE catalog
 * (joining PHYSICAL_BOOK and DIGITAL_JOURNAL subtypes, with is_public on RESOURCE).
 */
public class ResourceRepository {

    /**
     * Finds a Resource by its primary key resource_id.
     */
    public Resource findById(int resourceId) {
        String sql = "SELECT resource_id, title, publish_date, topic, resource_type, is_public " +
                     "FROM RESOURCE WHERE resource_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date pDate = rs.getDate("publish_date");
                    return new Resource(
                            rs.getInt("resource_id"),
                            rs.getString("title"),
                            pDate != null ? pDate.toLocalDate() : null,
                            rs.getString("topic"),
                            rs.getString("resource_type"),
                            rs.getBoolean("is_public")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu tài nguyên ID " + resourceId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Resolves the parent resource_id for a physical BOOK_COPY copy_id.
     */
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

    /**
     * Retrieves all resources with specific subclass attributes, isPublic flag, and copy statistics.
     */
    public List<Map<String, Object>> findAllResourcesDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT r.resource_id, r.title, r.publish_date, r.topic, r.resource_type, r.is_public, " +
                     "       pb.shelf_location, " +
                     "       dj.url, dj.file_size, dj.format, " +
                     "       COUNT(bc.copy_id) AS total_copies, " +
                     "       SUM(CASE WHEN bc.availability_status = 'Available' THEN 1 ELSE 0 END) AS available_copies " +
                     "FROM RESOURCE r " +
                     "LEFT JOIN PHYSICAL_BOOK pb ON r.resource_id = pb.resource_id " +
                     "LEFT JOIN DIGITAL_JOURNAL dj ON r.resource_id = dj.resource_id " +
                     "LEFT JOIN BOOK_COPY bc ON r.resource_id = bc.resource_id " +
                     "GROUP BY r.resource_id, r.title, r.publish_date, r.topic, r.resource_type, r.is_public, " +
                     "         pb.shelf_location, dj.url, dj.file_size, dj.format " +
                     "ORDER BY r.resource_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                int resourceId = rs.getInt("resource_id");
                map.put("resourceId", resourceId);
                map.put("title", rs.getString("title"));
                Date pDate = rs.getDate("publish_date");
                map.put("publishDate", pDate != null ? pDate.toString() : null);
                map.put("topic", rs.getString("topic"));
                String type = rs.getString("resource_type");
                map.put("resourceType", type);
                map.put("isPublic", rs.getBoolean("is_public"));

                if ("PHYSICAL_BOOK".equalsIgnoreCase(type)) {
                    map.put("shelfLocation", rs.getString("shelf_location"));
                    map.put("totalCopies", rs.getInt("total_copies"));
                    map.put("availableCopies", rs.getInt("available_copies"));
                } else if ("DIGITAL_JOURNAL".equalsIgnoreCase(type)) {
                    map.put("url", rs.getString("url"));
                    map.put("fileSize", rs.getInt("file_size"));
                    map.put("format", rs.getString("format"));
                }
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất danh mục tài nguyên: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all copies belonging to a specific physical book.
     */
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
