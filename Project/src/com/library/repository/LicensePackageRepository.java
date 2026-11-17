package com.library.repository;

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

    /**
     * Inserts a new LicensePackage into MySQL.
     */
    public void insertPackage(LicensePackage lp) {
        String sql = "INSERT INTO LICENSE_PACKAGE (license_name, provider, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, lp.getLicenseName());
            ps.setString(2, lp.getProvider());
            ps.setString(3, lp.getDescription());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        lp.setLicenseId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[LicensePackageRepository] Error inserting package: " + e.getMessage());
        }
    }

    /**
     * Finds a LicensePackage by licenseId.
     */
    public LicensePackage findById(int licenseId) {
        String sql = "SELECT license_id, license_name, provider, description FROM LICENSE_PACKAGE WHERE license_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, licenseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPackage(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[LicensePackageRepository] Error finding package " + licenseId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds all license packages that contain a specific digital journal.
     */
    public List<LicensePackage> findPackagesForJournal(int journalResourceId) {
        List<LicensePackage> list = new ArrayList<>();
        String sql = "SELECT lp.license_id, lp.license_name, lp.provider, lp.description " +
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
            System.err.println("[LicensePackageRepository] Error finding packages for journal " + journalResourceId + ": " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all license packages.
     */
    public List<LicensePackage> findAll() {
        List<LicensePackage> list = new ArrayList<>();
        String sql = "SELECT license_id, license_name, provider, description FROM LICENSE_PACKAGE ORDER BY license_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToPackage(rs));
            }
        } catch (SQLException e) {
            System.err.println("[LicensePackageRepository] Error fetching all packages: " + e.getMessage());
        }
        return list;
    }

    private LicensePackage mapRowToPackage(ResultSet rs) throws SQLException {
        return new LicensePackage(
                rs.getInt("license_id"),
                rs.getString("license_name"),
                rs.getString("provider"),
                rs.getString("description")
        );
    }
}
