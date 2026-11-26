package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.PaymentTransaction;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling read-only MySQL data queries for PAYMENT_TRANSACTION.
 * Payment insertion is delegated to MySQL Stored Procedure sp_confirm_fine_payment.
 */
public class PaymentTransactionRepository {

    public PaymentTransaction findById(int transactionId) {
        String sql = "SELECT transaction_id, fine_id, payment_date FROM PAYMENT_TRANSACTION WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTransaction(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu giao dịch ID " + transactionId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public List<PaymentTransaction> findByFineId(int fineId) {
        List<PaymentTransaction> list = new ArrayList<>();
        String sql = "SELECT transaction_id, fine_id, payment_date FROM PAYMENT_TRANSACTION WHERE fine_id = ? ORDER BY payment_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu giao dịch cho phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
        }
        return list;
    }

    public boolean hasPaymentTransaction(int fineId) {
        String sql = "SELECT COUNT(*) AS tx_count FROM PAYMENT_TRANSACTION WHERE fine_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("tx_count") > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi kiểm tra giao dịch của phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
        }
        return false;
    }

    public List<PaymentTransaction> findAll() {
        List<PaymentTransaction> list = new ArrayList<>();
        String sql = "SELECT transaction_id, fine_id, payment_date FROM PAYMENT_TRANSACTION ORDER BY transaction_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi lấy danh sách toàn bộ giao dịch thanh toán: " + e.getMessage(), e);
        }
        return list;
    }

    private PaymentTransaction mapRowToTransaction(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("payment_date");
        return new PaymentTransaction(
                rs.getInt("transaction_id"),
                rs.getInt("fine_id"),
                timestamp != null ? timestamp.toLocalDateTime() : null
        );
    }
}
