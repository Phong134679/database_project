package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.LicensePackage;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data persistence for LICENSE_PACKAGE entity.
 */
public class LicensePackageRepository {

    public void insertPackage(LicensePackage lp) {
        String sql = "INSERT INTO LICENSE_PACKAGE (license_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, lp.getLicenseName());
            ps.setString(2, lp.getDescription());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        lp.setLicenseId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thêm gói bản quyền (insertPackage): " + e.getMessage(), e);
        }
    }

    public LicensePackage findById(int licenseId) {
        String sql = "SELECT license_id, license_name, description FROM LICENSE_PACKAGE WHERE license_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, licenseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPackage(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu gói bản quyền ID " + licenseId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public List<LicensePackage> findPackagesForJournal(int journalResourceId) {
        List<LicensePackage> list = new ArrayList<>();
        String sql = "SELECT lp.license_id, lp.license_name, lp.description " +
                     "FROM LICENSE_PACKAGE lp " +
                     "JOIN RESOURCE_LICENSE rl ON lp.license_id = rl.license_id " +
                     "WHERE rl.resource_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, journalResourceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToPackage(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu gói bản quyền cho tài liệu " + journalResourceId + ": " + e.getMessage(), e);
        }
        return list;
    }

    public List<LicensePackage> findAll() {
        List<LicensePackage> list = new ArrayList<>();
        String sql = "SELECT license_id, license_name, description FROM LICENSE_PACKAGE ORDER BY license_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToPackage(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ danh mục gói bản quyền: " + e.getMessage(), e);
        }
        return list;
    }

    private LicensePackage mapRowToPackage(ResultSet rs) throws SQLException {
        return new LicensePackage(
                rs.getInt("license_id"),
                rs.getString("license_name"),
                rs.getString("description")
        );
    }
}
