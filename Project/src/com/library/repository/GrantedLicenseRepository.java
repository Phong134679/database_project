package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.GrantedLicense;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling MySQL data persistence and queries for GRANTED_LICENSE via direct JDBC.
 */
public class GrantedLicenseRepository {

    public GrantedLicense findById(Connection conn, int grantId) throws SQLException {
        String sql = "SELECT grant_id, person_id, license_id FROM GRANTED_LICENSE WHERE grant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, grantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToGrant(rs);
                }
            }
        }
        return null;
    }

    public GrantedLicense findById(int grantId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, grantId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu cấp phép bản quyền ID " + grantId + ": " + e.getMessage(), e);
        }
    }

    public boolean existsByPersonAndLicense(Connection conn, int personId, int licenseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM GRANTED_LICENSE WHERE person_id = ? AND license_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, licenseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int insertGrant(Connection conn, int personId, int licenseId) throws SQLException {
        String sql = "INSERT INTO GRANTED_LICENSE (person_id, license_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, personId);
            ps.setInt(2, licenseId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Không thể lấy grant_id sau khi INSERT GRANTED_LICENSE.");
    }

    public int deleteByPersonAndLicense(Connection conn, int personId, int licenseId) throws SQLException {
        String sql = "DELETE FROM GRANTED_LICENSE WHERE person_id = ? AND license_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, licenseId);
            return ps.executeUpdate();
        }
    }

    public int deleteByGrantId(Connection conn, int grantId) throws SQLException {
        String sql = "DELETE FROM GRANTED_LICENSE WHERE grant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, grantId);
            return ps.executeUpdate();
        }
    }

    /**
     * Counts distinct required licenses for the resource that are currently granted to the person.
     */
    public int countGrantedRequiredLicenses(Connection conn, int personId, int resourceId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT rl.license_id) " +
                     "FROM RESOURCE_LICENSE rl " +
                     "INNER JOIN GRANTED_LICENSE gl " +
                     "    ON gl.license_id = rl.license_id " +
                     "   AND gl.person_id = ? " +
                     "WHERE rl.resource_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<GrantedLicense> findByPersonId(int personId) {
        List<GrantedLicense> list = new ArrayList<>();
        String sql = "SELECT grant_id, person_id, license_id " +
                     "FROM GRANTED_LICENSE WHERE person_id = ? ORDER BY grant_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToGrant(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu cấp phép bản quyền của người dùng " + personId + ": " + e.getMessage(), e);
        }
        return list;
    }

    public List<GrantedLicense> findAll() {
        List<GrantedLicense> list = new ArrayList<>();
        String sql = "SELECT grant_id, person_id, license_id FROM GRANTED_LICENSE ORDER BY grant_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToGrant(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ danh mục cấp phép: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all granted licenses directly from GRANTED_LICENSE, PERSON, LICENSE_PACKAGE,
     * and UNIVERSITY_MEMBER without using VIEW_GRANTED_LICENSES_DETAILED.
     */
    public List<Map<String, Object>> findAllDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT gl.grant_id, gl.person_id, p.name AS person_name, um.member_id, um.member_type, " +
                     "       gl.license_id, lp.license_name, lp.description " +
                     "FROM GRANTED_LICENSE gl " +
                     "INNER JOIN PERSON p ON p.person_id = gl.person_id " +
                     "INNER JOIN LICENSE_PACKAGE lp ON lp.license_id = gl.license_id " +
                     "LEFT JOIN UNIVERSITY_MEMBER um ON um.person_id = gl.person_id " +
                     "ORDER BY gl.grant_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("grantId", rs.getInt("grant_id"));
                map.put("personId", rs.getInt("person_id"));
                map.put("personName", rs.getString("person_name"));
                map.put("memberId", rs.getString("member_id"));
                map.put("memberType", rs.getString("member_type"));
                map.put("licenseId", rs.getInt("license_id"));
                map.put("licenseName", rs.getString("license_name"));
                map.put("description", rs.getString("description"));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất danh sách cấp phép chi tiết: " + e.getMessage(), e);
        }
        return list;
    }

    private GrantedLicense mapRowToGrant(ResultSet rs) throws SQLException {
        return new GrantedLicense(
                rs.getInt("grant_id"),
                rs.getInt("person_id"),
                rs.getInt("license_id")
        );
    }
}
