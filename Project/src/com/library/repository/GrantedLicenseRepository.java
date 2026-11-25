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
 * Repository class handling MySQL data persistence for GRANTED_LICENSE entity.
 * Đã bỏ valid_from / valid_to; kiểm tra theo sự tồn tại bản ghi và quy tắc AND cho nhiều License.
 */
public class GrantedLicenseRepository {

    /**
     * Checks if a person already holds a specific license package (prevents duplicate grant).
     */
    public boolean existsGrant(int personId, int licenseId) {
        String sql = "SELECT COUNT(*) FROM GRANTED_LICENSE WHERE person_id = ? AND license_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, licenseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi kiểm tra trùng lặp License: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Inserts a new GrantedLicense into MySQL (MANUAL grant).
     */
    public void insertGrant(GrantedLicense grant) {
        String sql = "INSERT INTO GRANTED_LICENSE (person_id, license_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, grant.getPersonId());
            ps.setInt(2, grant.getLicenseId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        grant.setGrantId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi cấp phép bản quyền (insertGrant): " + e.getMessage(), e);
        }
    }

    /**
     * Revokes a license from a person by (person_id, license_id).
     */
    public int revokeGrant(int personId, int licenseId) {
        String sql = "DELETE FROM GRANTED_LICENSE WHERE person_id = ? AND license_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            ps.setInt(2, licenseId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thu hồi License: " + e.getMessage(), e);
        }
    }

    /**
     * Revokes a license grant by primary key grant_id.
     */
    public int revokeByGrantId(int grantId) {
        String sql = "DELETE FROM GRANTED_LICENSE WHERE grant_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, grantId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thu hồi License ID " + grantId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Finds a GrantedLicense by grantId.
     */
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

    /**
     * Finds all license grants for a specific person.
     */
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

    /**
     * Checks if a private Resource declares at least one required license in RESOURCE_LICENSE
     * AND the person owns ALL required licenses (Method B: AND inside).
     * Explicitly checks requiredCount > 0 to avoid the double NOT EXISTS empty-set trap.
     */
    public boolean hasAllRequiredLicenses(int personId, int resourceId) {
        String countReqSql = "SELECT COUNT(*) AS req_count FROM RESOURCE_LICENSE WHERE resource_id = ?";
        String countOwnedSql = "SELECT COUNT(DISTINCT rl.license_id) AS owned_count " +
                               "FROM RESOURCE_LICENSE rl " +
                               "INNER JOIN GRANTED_LICENSE gl ON rl.license_id = gl.license_id " +
                               "WHERE rl.resource_id = ? AND gl.person_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            int reqCount = 0;
            try (PreparedStatement psReq = conn.prepareStatement(countReqSql)) {
                psReq.setInt(1, resourceId);
                try (ResultSet rsReq = psReq.executeQuery()) {
                    if (rsReq.next()) {
                        reqCount = rsReq.getInt("req_count");
                    }
                }
            }

            // Resource private không khai báo License -> từ chối (không mặc định cho phép)
            if (reqCount == 0) {
                return false;
            }

            int ownedCount = 0;
            try (PreparedStatement psOwned = conn.prepareStatement(countOwnedSql)) {
                psOwned.setInt(1, resourceId);
                psOwned.setInt(2, personId);
                try (ResultSet rsOwned = psOwned.executeQuery()) {
                    if (rsOwned.next()) {
                        ownedCount = rsOwned.getInt("owned_count");
                    }
                }
            }

            return ownedCount == reqCount;
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi kiểm tra bộ License cho tài nguyên " + resourceId + " và người dùng " + personId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all GrantedLicense records.
     */
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
     * Retrieves all granted licenses joined with Person and LicensePackage details for Librarian UI.
     */
    public List<Map<String, Object>> findAllDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT gl.grant_id, gl.person_id, p.name AS person_name, p.person_type, " +
                     "       gl.license_id, lp.license_name, lp.provider " +
                     "FROM GRANTED_LICENSE gl " +
                     "JOIN PERSON p ON gl.person_id = p.person_id " +
                     "JOIN LICENSE_PACKAGE lp ON gl.license_id = lp.license_id " +
                     "ORDER BY gl.grant_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("grantId", rs.getInt("grant_id"));
                map.put("personId", rs.getInt("person_id"));
                map.put("personName", rs.getString("person_name"));
                map.put("personType", rs.getString("person_type"));
                map.put("licenseId", rs.getInt("license_id"));
                map.put("licenseName", rs.getString("license_name"));
                map.put("provider", rs.getString("provider"));
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
