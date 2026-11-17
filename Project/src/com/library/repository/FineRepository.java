package com.library.repository;

import com.library.model.Fine;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data persistence for Fine entity.
 */
public class FineRepository {

    /**
     * Inserts a new Fine record into MySQL and updates the fine's fineId.
     */
    public void insertFine(Fine fine) {
        String sql = "INSERT INTO FINE (borrow_id, total_fine_amount, payment_status, due_date) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, fine.getBorrowId());
            ps.setBigDecimal(2, fine.getTotalFineAmount());
            ps.setString(3, fine.getPaymentStatus() != null ? fine.getPaymentStatus() : "Unpaid");
            ps.setDate(4, fine.getDueDate() != null ? Date.valueOf(fine.getDueDate()) : null);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        fine.setFineId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error inserting fine: " + e.getMessage());
        }
    }

    /**
     * Finds a Fine record by primary key fineId.
     */
    public Fine findById(int fineId) {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status, due_date " +
                     "FROM FINE WHERE fine_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error finding fine by id " + fineId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a Fine record by its associated borrowId.
     */
    public Fine findByBorrowId(int borrowId) {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status, due_date " +
                     "FROM FINE WHERE borrow_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error finding fine by borrowId " + borrowId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds all Fine records in the system.
     */
    public List<Fine> findAll() {
        List<Fine> list = new ArrayList<>();
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status, due_date " +
                     "FROM FINE ORDER BY fine_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToFine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error fetching all fines: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds all outstanding fines (status is 'Unpaid' or 'Partially Paid').
     */
    public List<Fine> findOutstandingFines() {
        List<Fine> list = new ArrayList<>();
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status, due_date " +
                     "FROM FINE WHERE payment_status IN ('Unpaid', 'Partially Paid') " +
                     "ORDER BY due_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToFine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error fetching outstanding fines: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an existing Fine record.
     */
    public void updateFine(Fine fine) {
        String sql = "UPDATE FINE SET borrow_id = ?, total_fine_amount = ?, " +
                     "payment_status = ?, due_date = ? WHERE fine_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fine.getBorrowId());
            ps.setBigDecimal(2, fine.getTotalFineAmount());
            ps.setString(3, fine.getPaymentStatus());
            ps.setDate(4, fine.getDueDate() != null ? Date.valueOf(fine.getDueDate()) : null);
            ps.setInt(5, fine.getFineId());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error updating fine " + fine.getFineId() + ": " + e.getMessage());
        }
    }

    /**
     * Updates only the payment status of a Fine.
     */
    public void updatePaymentStatus(int fineId, String paymentStatus) {
        String sql = "UPDATE FINE SET payment_status = ? WHERE fine_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, paymentStatus);
            ps.setInt(2, fineId);

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error updating payment status for fine " + fineId + ": " + e.getMessage());
        }
    }

    /**
     * Deletes a Fine record by fineId.
     */
    public void deleteFine(int fineId) {
        String sql = "DELETE FROM FINE WHERE fine_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fineId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error deleting fine " + fineId + ": " + e.getMessage());
        }
    }

    /**
     * Checks if a member has any unpaid or partially paid fines.
     */
    public boolean hasUnpaidFinesByMember(int memberPersonId) {
        String sql = "SELECT COUNT(*) AS unpaid_count FROM FINE f " +
                     "JOIN BORROW b ON f.borrow_id = b.borrow_id " +
                     "WHERE b.member_person_id = ? AND f.payment_status IN ('Unpaid', 'Partially Paid')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberPersonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("unpaid_count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[FineRepository] Error checking unpaid fines for member " + memberPersonId + ": " + e.getMessage());
        }
        return false;
    }

    private Fine mapRowToFine(ResultSet rs) throws SQLException {
        Date dueDateSql = rs.getDate("due_date");
        return new Fine(
                rs.getInt("fine_id"),
                rs.getInt("borrow_id"),
                rs.getBigDecimal("total_fine_amount"),
                rs.getString("payment_status"),
                dueDateSql != null ? dueDateSql.toLocalDate() : null
        );
    }
}
