package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.Borrow;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling read-only MySQL data queries for BORROW and BOOK_COPY.
 * All write/state-change operations are delegated to MySQL Stored Procedures
 * (sp_borrow_book, sp_return_book).
 */
public class BorrowRepository {

    public Borrow findById(int borrowId) {
        String sql = "SELECT borrow_id, member_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW WHERE borrow_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBorrow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu mượn ID " + borrowId + ": " + e.getMessage(), e);
        }
        return null;
    }

    public List<Borrow> findOverdueBorrows() {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT borrow_id, member_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW WHERE return_date IS NULL AND due_date < CURDATE() " +
                     "ORDER BY due_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToBorrow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu danh sách phiếu mượn quá hạn: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Borrow> findByMemberId(String memberId) {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT borrow_id, member_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW WHERE member_id = ? ORDER BY borrow_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBorrow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu mượn của thành viên " + memberId + ": " + e.getMessage(), e);
        }
        return list;
    }

    public List<Borrow> findAll() {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT borrow_id, member_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW ORDER BY borrow_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToBorrow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ danh sách phiếu mượn: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all borrows with Member and Book details from VIEW_BORROWS_DETAILED.
     */
    public List<Map<String, Object>> findAllBorrowsDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT borrow_id, member_id, person_id, member_name, member_type, " +
                     "       copy_id, book_title, borrow_date, due_date, return_date, " +
                     "       is_overdue, status, fine_id, fine_amount, fine_status " +
                     "FROM VIEW_BORROWS_DETAILED " +
                     "ORDER BY borrow_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("borrowId", rs.getInt("borrow_id"));
                map.put("memberId", rs.getString("member_id"));
                map.put("personId", rs.getInt("person_id"));
                map.put("memberName", rs.getString("member_name"));
                map.put("memberType", rs.getString("member_type"));
                map.put("copyId", rs.getString("copy_id"));
                map.put("bookTitle", rs.getString("book_title"));
                Date bDate = rs.getDate("borrow_date");
                Date dDate = rs.getDate("due_date");
                Date rDate = rs.getDate("return_date");
                map.put("borrowDate", bDate != null ? bDate.toString() : null);
                map.put("dueDate", dDate != null ? dDate.toString() : null);
                map.put("returnDate", rDate != null ? rDate.toString() : null);
                map.put("isOverdue", rs.getBoolean("is_overdue"));
                map.put("status", rs.getString("status"));
                int fineId = rs.getInt("fine_id");
                map.put("fineId", rs.wasNull() ? null : fineId);
                map.put("fineAmount", rs.getBigDecimal("fine_amount"));
                map.put("fineStatus", rs.getString("fine_status"));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu danh sách mượn chi tiết: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all book copies from VIEW_BOOKS_DETAILED for UI selection.
     */
    public List<Map<String, Object>> findAllCopiesDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT copy_id, resource_id, title AS book_title, availability_status, " +
                     "       shelf_location, is_public, required_license_count " +
                     "FROM VIEW_BOOKS_DETAILED " +
                     "ORDER BY copy_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("copyId", rs.getString("copy_id"));
                map.put("resourceId", rs.getInt("resource_id"));
                map.put("bookTitle", rs.getString("book_title"));
                map.put("availabilityStatus", rs.getString("availability_status"));
                map.put("shelfLocation", rs.getString("shelf_location"));
                map.put("isPublic", rs.getBoolean("is_public"));
                map.put("requiredLicenseCount", rs.getInt("required_license_count"));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu danh sách bản sao: " + e.getMessage(), e);
        }
        return list;
    }

    private Borrow mapRowToBorrow(ResultSet rs) throws SQLException {
        Date borrowDateSql = rs.getDate("borrow_date");
        Date dueDateSql = rs.getDate("due_date");
        Date returnDateSql = rs.getDate("return_date");

        return new Borrow(
                rs.getInt("borrow_id"),
                rs.getString("member_id"),
                rs.getString("copy_id"),
                borrowDateSql != null ? borrowDateSql.toLocalDate() : null,
                dueDateSql != null ? dueDateSql.toLocalDate() : null,
                returnDateSql != null ? returnDateSql.toLocalDate() : null
        );
    }
}
