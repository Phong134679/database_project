package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.Fine;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling MySQL data persistence and queries for FINE via direct JDBC.
 */
public class FineRepository {

    public Fine findById(Connection conn, int fineId) throws SQLException {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE WHERE fine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        }
        return null;
    }

    public Fine findById(int fineId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, fineId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
        }
    }

    public Fine findByIdForUpdate(Connection conn, int fineId) throws SQLException {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE WHERE fine_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        }
        return null;
    }

    public Fine findByBorrowId(Connection conn, int borrowId) throws SQLException {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE WHERE borrow_id = ? ORDER BY fine_id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        }
        return null;
    }

    public Fine findByBorrowId(int borrowId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByBorrowId(conn, borrowId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu phạt theo lượt mượn " + borrowId + ": " + e.getMessage(), e);
        }
    }

    public Fine findLatestUnpaidByBorrowIdForUpdate(Connection conn, int borrowId) throws SQLException {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE WHERE borrow_id = ? AND payment_status = 'Unpaid' " +
                     "ORDER BY fine_id DESC LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        }
        return null;
    }

    public int insertFine(Connection conn, int borrowId, BigDecimal amount, String paymentStatus) throws SQLException {
        String sql = "INSERT INTO FINE (borrow_id, total_fine_amount, payment_status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, borrowId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, paymentStatus);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Không thể lấy fine_id sau khi INSERT FINE.");
    }

    public void updateTotalFineAmount(Connection conn, int fineId, BigDecimal newAmount) throws SQLException {
        String sql = "UPDATE FINE SET total_fine_amount = ? WHERE fine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newAmount);
            ps.setInt(2, fineId);
            ps.executeUpdate();
        }
    }

    public void updatePaymentStatus(Connection conn, int fineId, String paymentStatus) throws SQLException {
        String sql = "UPDATE FINE SET payment_status = ? WHERE fine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentStatus);
            ps.setInt(2, fineId);
            ps.executeUpdate();
        }
    }

    public List<Fine> findAll() {
        List<Fine> list = new ArrayList<>();
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE ORDER BY fine_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToFine(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ phiếu phạt: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Fine> findOutstandingFines() {
        List<Fine> list = new ArrayList<>();
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE WHERE payment_status = 'Unpaid' " +
                     "ORDER BY fine_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToFine(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất danh sách phạt chưa nộp: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all fines with member, borrow, book, and latest payment transaction details
     * directly from base tables without using VIEW_FINES_DETAILED.
     */
    public List<Map<String, Object>> findAllFinesDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT f.fine_id, f.borrow_id, b.member_id, um.person_id, p.name AS member_name, " +
                     "       um.member_type, b.copy_id, r.title AS book_title, f.total_fine_amount, " +
                     "       f.payment_status, pt_latest.transaction_id, pt_latest.payment_date " +
                     "FROM FINE f " +
                     "INNER JOIN BORROW b ON b.borrow_id = f.borrow_id " +
                     "INNER JOIN UNIVERSITY_MEMBER um ON um.member_id = b.member_id " +
                     "INNER JOIN PERSON p ON p.person_id = um.person_id " +
                     "INNER JOIN BOOK_COPY bc ON bc.copy_id = b.copy_id " +
                     "INNER JOIN RESOURCE r ON r.resource_id = bc.resource_id " +
                     "LEFT JOIN ( " +
                     "    SELECT transaction_id, fine_id, payment_date " +
                     "    FROM ( " +
                     "        SELECT pt.transaction_id, pt.fine_id, pt.payment_date, " +
                     "               ROW_NUMBER() OVER (PARTITION BY pt.fine_id ORDER BY pt.payment_date DESC, pt.transaction_id DESC) AS rn " +
                     "        FROM PAYMENT_TRANSACTION pt " +
                     "    ) ranked " +
                     "    WHERE rn = 1 " +
                     ") pt_latest ON pt_latest.fine_id = f.fine_id " +
                     "ORDER BY f.fine_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("fineId", rs.getInt("fine_id"));
                map.put("borrowId", rs.getInt("borrow_id"));
                map.put("memberId", rs.getString("member_id"));
                map.put("personId", rs.getInt("person_id"));
                map.put("memberName", rs.getString("member_name"));
                map.put("memberType", rs.getString("member_type"));
                map.put("copyId", rs.getString("copy_id"));
                map.put("bookTitle", rs.getString("book_title"));
                map.put("totalFineAmount", rs.getBigDecimal("total_fine_amount"));
                map.put("paymentStatus", rs.getString("payment_status"));
                int txId = rs.getInt("transaction_id");
                map.put("transactionId", rs.wasNull() ? null : txId);
                Timestamp pDate = rs.getTimestamp("payment_date");
                map.put("paymentDate", pDate != null ? pDate.toLocalDateTime().toString() : null);
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu danh sách phiếu phạt chi tiết: " + e.getMessage(), e);
        }
        return list;
    }

    private Fine mapRowToFine(ResultSet rs) throws SQLException {
        return new Fine(
                rs.getInt("fine_id"),
                rs.getInt("borrow_id"),
                rs.getBigDecimal("total_fine_amount"),
                rs.getString("payment_status")
        );
    }
}
