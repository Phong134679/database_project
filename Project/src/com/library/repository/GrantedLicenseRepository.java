package com.library.repository;

import com.library.model.GrantedLicense;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data persistence for GRANTED_LICENSE entity.
 * Implements verification queries for BR04 and BR12.
 */
public class GrantedLicenseRepository {

    /**
     * Inserts a new GrantedLicense into MySQL.
     */
    public void insertGrant(GrantedLicense grant) {
        String sql = "INSERT INTO GRANTED_LICENSE (person_id, license_id, valid_from, valid_to) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, grant.getPersonId());
            ps.setInt(2, grant.getLicenseId());
            ps.setDate(3, grant.getValidFrom() != null ? Date.valueOf(grant.getValidFrom()) : Date.valueOf(LocalDate.now()));
            ps.setDate(4, grant.getValidTo() != null ? Date.valueOf(grant.getValidTo()) : null);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        grant.setGrantId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[GrantedLicenseRepository] Error inserting grant: " + e.getMessage());
        }
    }

    /**
     * Finds a GrantedLicense by grantId.
     */
    public GrantedLicense findById(int grantId) {
        String sql = "SELECT grant_id, person_id, license_id, valid_from, valid_to " +
                     "FROM GRANTED_LICENSE WHERE grant_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, grantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToGrant(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GrantedLicenseRepository] Error finding grant " + grantId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds all license grants for a specific person.
     */
    public List<GrantedLicense> findByPersonId(int personId) {
        List<GrantedLicense> list = new ArrayList<>();
        String sql = "SELECT grant_id, person_id, license_id, valid_from, valid_to " +
                     "FROM GRANTED_LICENSE WHERE person_id = ? ORDER BY valid_from DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToGrant(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[GrantedLicenseRepository] Error finding grants for person " + personId + ": " + e.getMessage());
        }
        return list;
    }

    /**
     * Checks if a person holds an active, unexpired license grant covering a specific digital journal.
     * Core validation query for Test 02, BR04, and BR12.
     */
    public boolean hasActiveLicenseForJournal(int personId, int journalResourceId, LocalDate accessDate) {
        String sql = "SELECT COUNT(*) AS active_count " +
                     "FROM GRANTED_LICENSE gl " +
                     "JOIN RESOURCE_LICENSE rl ON gl.license_id = rl.license_id " +
                     "WHERE gl.person_id = ? " +
                     "  AND rl.resource_id = ? " +
                     "  AND gl.valid_from <= ? " +
                     "  AND gl.valid_to >= ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            LocalDate checkDate = accessDate != null ? accessDate : LocalDate.now();
            ps.setInt(1, personId);
            ps.setInt(2, journalResourceId);
            ps.setDate(3, Date.valueOf(checkDate));
            ps.setDate(4, Date.valueOf(checkDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("active_count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[GrantedLicenseRepository] Error checking active license: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves all GrantedLicense records.
     */
    public List<GrantedLicense> findAll() {
        List<GrantedLicense> list = new ArrayList<>();
        String sql = "SELECT grant_id, person_id, license_id, valid_from, valid_to FROM GRANTED_LICENSE ORDER BY grant_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToGrant(rs));
            }
        } catch (SQLException e) {
            System.err.println("[GrantedLicenseRepository] Error fetching all grants: " + e.getMessage());
        }
        return list;
    }

    private GrantedLicense mapRowToGrant(ResultSet rs) throws SQLException {
        Date vf = rs.getDate("valid_from");
        Date vt = rs.getDate("valid_to");
        return new GrantedLicense(
                rs.getInt("grant_id"),
                rs.getInt("person_id"),
                rs.getInt("license_id"),
                vf != null ? vf.toLocalDate() : null,
                vt != null ? vt.toLocalDate() : null
        );
    }
}
