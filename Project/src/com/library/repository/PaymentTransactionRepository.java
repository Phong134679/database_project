package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.PaymentTransaction;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data persistence for PaymentTransaction entity.
 */
public class PaymentTransactionRepository {

    /**
     * Inserts a new PaymentTransaction record into MySQL and updates the transactionId.
     */
    public void insertTransaction(PaymentTransaction transaction) {
        String sql = "INSERT INTO PAYMENT_TRANSACTION (fine_id, amount_paid, payment_date, payment_method) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, transaction.getFineId());
            ps.setBigDecimal(2, transaction.getAmountPaid());
            LocalDateTime pDate = transaction.getPaymentDate() != null ? transaction.getPaymentDate() : LocalDateTime.now();
            ps.setTimestamp(3, Timestamp.valueOf(pDate));
            ps.setString(4, transaction.getPaymentMethod());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setTransactionId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thêm giao dịch thanh toán (insertTransaction): " + e.getMessage(), e);
        }
    }

    /**
     * Finds a payment transaction by primary key transactionId.
     */
    public PaymentTransaction findById(int transactionId) {
        String sql = "SELECT transaction_id, fine_id, amount_paid, payment_date, payment_method " +
                     "FROM PAYMENT_TRANSACTION WHERE transaction_id = ?";
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

    /**
     * Retrieves all payment transactions recorded for a specific fine.
     */
    public List<PaymentTransaction> findByFineId(int fineId) {
        List<PaymentTransaction> list = new ArrayList<>();
        String sql = "SELECT transaction_id, fine_id, amount_paid, payment_date, payment_method " +
                     "FROM PAYMENT_TRANSACTION WHERE fine_id = ? ORDER BY payment_date ASC";
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

    /**
     * Calculates the total amount paid so far for a specific fine.
     */
    public BigDecimal getTotalPaidForFine(int fineId) {
        String sql = "SELECT COALESCE(SUM(amount_paid), 0.00) AS total_paid " +
                     "FROM PAYMENT_TRANSACTION WHERE fine_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total_paid");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tính tổng tiền đã trả cho phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Retrieves all payment transactions in the system.
     */
    public List<PaymentTransaction> findAll() {
        List<PaymentTransaction> list = new ArrayList<>();
        String sql = "SELECT transaction_id, fine_id, amount_paid, payment_date, payment_method " +
                     "FROM PAYMENT_TRANSACTION ORDER BY transaction_id DESC";
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
                rs.getBigDecimal("amount_paid"),
                timestamp != null ? timestamp.toLocalDateTime() : null,
                rs.getString("payment_method")
        );
    }
}
