package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.Fine;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling read-only MySQL data queries for FINE entity.
 * Fine creation and payment confirmation are delegated to MySQL Stored Procedures
 * (sp_return_book, sp_confirm_fine_payment).
 */
public class FineRepository {

    public Fine findById(Connection conn, int fineId) {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE WHERE fine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public Fine findById(int fineId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, fineId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi tra cứu phiếu phạt: " + e.getMessage(), e);
        }
    }

    /**
     * Finds the latest Fine associated with a specific borrowId.
     */
    public Fine findByBorrowId(Connection conn, int borrowId) {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "FROM FINE WHERE borrow_id = ? ORDER BY fine_id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu phạt theo lượt mượn " + borrowId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public Fine findByBorrowId(int borrowId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByBorrowId(conn, borrowId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi tra cứu phiếu phạt: " + e.getMessage(), e);
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
     * Retrieves all fines with detailed member and borrow info from VIEW_FINES_DETAILED.
     */
    public List<Map<String, Object>> findAllFinesDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT fine_id, borrow_id, member_id, person_id, member_name, member_type, " +
                     "       copy_id, book_title, total_fine_amount, payment_status, " +
                     "       transaction_id, payment_date " +
                     "FROM VIEW_FINES_DETAILED " +
                     "ORDER BY fine_id DESC";
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
