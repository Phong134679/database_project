package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.DigitalJournal;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data queries for DIGITAL_JOURNAL via VIEW_JOURNALS_DETAILED.
 */
public class DigitalJournalRepository {

    public DigitalJournal findById(int resourceId) {
        String sql = "SELECT resource_id, title, topic, is_public, url, file_size, format " +
                     "FROM VIEW_JOURNALS_DETAILED WHERE resource_id = ?";
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
     * Checks if a person is an author of the specified resource via AUTHOR_OF table.
     */
    public boolean isAuthorOfResource(int personId, int resourceId) {
        String sql = "SELECT COUNT(*) FROM AUTHOR_OF WHERE person_id = ? AND resource_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi kiểm tra tư cách tác giả của người dùng " + personId + " cho tài nguyên " + resourceId + ": " + e.getMessage(), e);
        }
        return false;
    }

    public List<DigitalJournal> findAll() {
        List<DigitalJournal> list = new ArrayList<>();
        String sql = "SELECT resource_id, title, topic, is_public, url, file_size, format " +
                     "FROM VIEW_JOURNALS_DETAILED ORDER BY resource_id ASC";
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
        return new DigitalJournal(
                rs.getInt("resource_id"),
                rs.getString("title"),
                rs.getString("topic"),
                rs.getBoolean("is_public"),
                rs.getString("url"),
                rs.getInt("file_size"),
                rs.getString("format")
        );
    }
}
