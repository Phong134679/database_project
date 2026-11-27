package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.AccessLog;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling MySQL data persistence and queries for ACCESS_LOG via direct JDBC.
 */
public class AccessLogRepository {

    public long insertAccessLog(Connection conn, Integer personId, int journalResourceId, String ipAddress) throws SQLException {
        String sql = "INSERT INTO ACCESS_LOG (person_id, journal_resource_id, ip_address) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (personId != null) {
                ps.setInt(1, personId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, journalResourceId);
            ps.setString(3, ipAddress);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Không thể lấy log_id sau khi INSERT ACCESS_LOG.");
    }

    public AccessLog findById(long logId) {
        String sql = "SELECT log_id, person_id, journal_resource_id, ip_address FROM ACCESS_LOG WHERE log_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, logId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToLog(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu nhật ký truy cập ID " + logId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public List<AccessLog> findByPersonId(int personId) {
        List<AccessLog> list = new ArrayList<>();
        String sql = "SELECT log_id, person_id, journal_resource_id, ip_address " +
                     "FROM ACCESS_LOG WHERE person_id = ? ORDER BY log_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu nhật ký của người dùng " + personId + ": " + e.getMessage(), e);
        }
        return list;
    }

    public List<AccessLog> findByJournalId(int journalResourceId) {
        List<AccessLog> list = new ArrayList<>();
        String sql = "SELECT log_id, person_id, journal_resource_id, ip_address " +
                     "FROM ACCESS_LOG WHERE journal_resource_id = ? ORDER BY log_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, journalResourceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu nhật ký cho tài liệu " + journalResourceId + ": " + e.getMessage(), e);
        }
        return list;
    }

    public List<AccessLog> findAll() {
        List<AccessLog> list = new ArrayList<>();
        String sql = "SELECT log_id, person_id, journal_resource_id, ip_address FROM ACCESS_LOG ORDER BY log_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToLog(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ danh sách nhật ký: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all access logs with person and journal details directly from base tables
     * without using VIEW_ACCESS_LOGS_DETAILED.
     */
    public List<Map<String, Object>> findAllDetailedRaw() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT l.log_id, l.person_id, p.name AS person_name, " +
                     "       l.journal_resource_id, r.title AS journal_title, r.is_public, " +
                     "       dj.url AS journal_url, dj.format AS journal_format, l.ip_address " +
                     "FROM ACCESS_LOG l " +
                     "LEFT JOIN PERSON p ON p.person_id = l.person_id " +
                     "LEFT JOIN RESOURCE r ON r.resource_id = l.journal_resource_id " +
                     "LEFT JOIN DIGITAL_JOURNAL dj ON dj.resource_id = l.journal_resource_id " +
                     "ORDER BY l.log_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                Integer pId = rs.getObject("person_id", Integer.class);
                map.put("logId", rs.getLong("log_id"));
                map.put("personId", pId);
                map.put("personName", rs.getString("person_name"));
                map.put("journalResourceId", rs.getInt("journal_resource_id"));
                map.put("journalTitle", rs.getString("journal_title"));
                map.put("isPublic", rs.getBoolean("is_public"));
                map.put("journalUrl", rs.getString("journal_url"));
                map.put("journalFormat", rs.getString("journal_format"));
                map.put("ipAddress", rs.getString("ip_address"));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất nhật ký truy cập chi tiết: " + e.getMessage(), e);
        }
        return list;
    }

    private AccessLog mapRowToLog(ResultSet rs) throws SQLException {
        Integer personId = rs.getObject("person_id", Integer.class);
        return new AccessLog(
                rs.getLong("log_id"),
                personId,
                rs.getInt("journal_resource_id"),
                rs.getString("ip_address")
        );
    }
}
