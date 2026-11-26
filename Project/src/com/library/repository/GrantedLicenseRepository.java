package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.GrantedLicense;
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
 * Repository class handling read-only MySQL data queries for GRANTED_LICENSE.
 * Grant and revoke operations are delegated to MySQL Stored Procedures
 * (sp_grant_license_manual, sp_revoke_license_manual).
 */
public class GrantedLicenseRepository {

    public GrantedLicense findById(int grantId) {
        String sql = "SELECT grant_id, person_id, license_id FROM GRANTED_LICENSE WHERE grant_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, grantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToGrant(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu cấp phép bản quyền ID " + grantId + ": " + e.getMessage(), e);
        }
        return null;
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

    public boolean hasAllRequiredLicenses(int personId, int resourceId) {
        String sql = "SELECT fn_has_all_required_licenses(?, ?) AS has_all";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("has_all");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi kiểm tra bộ License cho tài nguyên " + resourceId + " và người dùng " + personId + ": " + e.getMessage(), e);
        }
        return false;
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
     * Retrieves all granted licenses from VIEW_GRANTED_LICENSES_DETAILED.
     */
    public List<Map<String, Object>> findAllDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT grant_id, person_id, person_name, member_id, member_type, " +
                     "       license_id, license_name, description " +
                     "FROM VIEW_GRANTED_LICENSES_DETAILED " +
                     "ORDER BY grant_id DESC";
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
