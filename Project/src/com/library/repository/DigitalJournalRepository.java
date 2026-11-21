package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.DigitalJournal;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data persistence for DIGITAL_JOURNAL subclass.
 */
public class DigitalJournalRepository {

    /**
     * Finds a DigitalJournal by its resourceId joining with RESOURCE superclass table.
     */
    public DigitalJournal findById(int resourceId) {
        String sql = "SELECT r.resource_id, r.title, r.publish_date, r.topic, r.resource_type, " +
                     "       dj.doi, dj.url, dj.file_size, dj.format, dj.is_public " +
                     "FROM DIGITAL_JOURNAL dj " +
                     "JOIN RESOURCE r ON dj.resource_id = r.resource_id " +
                     "WHERE dj.resource_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToJournal(rs);
                }
            }
        } catch (SQLException e) {
            // Fallback query if is_public column not yet migrated
            return findByIdLegacy(resourceId);
        }
        return null;
    }

    private DigitalJournal findByIdLegacy(int resourceId) {
        String sql = "SELECT r.resource_id, r.title, r.publish_date, r.topic, r.resource_type, " +
                     "       dj.doi, dj.url, dj.file_size, dj.format " +
                     "FROM DIGITAL_JOURNAL dj " +
                     "JOIN RESOURCE r ON dj.resource_id = r.resource_id " +
                     "WHERE dj.resource_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToJournal(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu tài liệu số ID " + resourceId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds a DigitalJournal by its unique DOI identifier.
     */
    public DigitalJournal findByDoi(String doi) {
        String sql = "SELECT r.resource_id, r.title, r.publish_date, r.topic, r.resource_type, " +
                     "       dj.doi, dj.url, dj.file_size, dj.format, dj.is_public " +
                     "FROM DIGITAL_JOURNAL dj " +
                     "JOIN RESOURCE r ON dj.resource_id = r.resource_id " +
                     "WHERE dj.doi = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToJournal(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu tài liệu số theo DOI " + doi + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieves all DigitalJournal records.
     */
    public List<DigitalJournal> findAll() {
        List<DigitalJournal> list = new ArrayList<>();
        String sql = "SELECT r.resource_id, r.title, r.publish_date, r.topic, r.resource_type, " +
                     "       dj.doi, dj.url, dj.file_size, dj.format, dj.is_public " +
                     "FROM DIGITAL_JOURNAL dj " +
                     "JOIN RESOURCE r ON dj.resource_id = r.resource_id " +
                     "ORDER BY r.resource_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToJournal(rs));
            }
        } catch (SQLException e) {
            // Fallback if is_public column not present
            return findAllLegacy();
        }
        return list;
    }

    private List<DigitalJournal> findAllLegacy() {
        List<DigitalJournal> list = new ArrayList<>();
        String sql = "SELECT r.resource_id, r.title, r.publish_date, r.topic, r.resource_type, " +
                     "       dj.doi, dj.url, dj.file_size, dj.format " +
                     "FROM DIGITAL_JOURNAL dj " +
                     "JOIN RESOURCE r ON dj.resource_id = r.resource_id " +
                     "ORDER BY r.resource_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToJournal(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ danh mục tài liệu số: " + e.getMessage(), e);
        }
        return list;
    }

    private DigitalJournal mapRowToJournal(ResultSet rs) throws SQLException {
        Date pDate = rs.getDate("publish_date");
        boolean isPublic = false;
        try {
            isPublic = rs.getBoolean("is_public");
        } catch (SQLException ignored) {
            // fallback if column not in result set
        }

        return new DigitalJournal(
                rs.getInt("resource_id"),
                rs.getString("title"),
                pDate != null ? pDate.toLocalDate() : null,
                rs.getString("topic"),
                rs.getString("resource_type"),
                rs.getString("doi"),
                rs.getString("url"),
                rs.getInt("file_size"),
                rs.getString("format"),
                isPublic
        );
    }
}
