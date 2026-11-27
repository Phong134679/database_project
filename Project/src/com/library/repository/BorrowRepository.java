package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.Borrow;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling MySQL data persistence and queries for BORROW and BOOK_COPY via direct JDBC.
 */
public class BorrowRepository {

    public Borrow findById(Connection conn, int borrowId) throws SQLException {
        String sql = "SELECT borrow_id, member_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW WHERE borrow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBorrow(rs);
                }
            }
        }
        return null;
    }

    public Borrow findById(int borrowId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, borrowId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu mượn ID " + borrowId + ": " + e.getMessage(), e);
        }
    }

    public Borrow findByIdForUpdate(Connection conn, int borrowId) throws SQLException {
        String sql = "SELECT borrow_id, member_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW WHERE borrow_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBorrow(rs);
                }
            }
        }
        return null;
    }

    public int countOverdueByMemberId(Connection conn, String memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BORROW WHERE member_id = ? AND return_date IS NULL AND due_date < CURDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countUnpaidFinesByMemberId(Connection conn, String memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BORROW b " +
                     "INNER JOIN FINE f ON f.borrow_id = b.borrow_id " +
                     "WHERE b.member_id = ? AND f.payment_status = 'Unpaid'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countActiveByMemberId(Connection conn, String memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BORROW WHERE member_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countActiveByCopyId(Connection conn, String copyId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BORROW WHERE copy_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Map<String, Object> findPhysicalCopyForUpdate(Connection conn, String copyId) throws SQLException {
        String sql = "SELECT bc.copy_id, bc.resource_id, bc.availability_status, r.is_public " +
                     "FROM BOOK_COPY bc " +
                     "INNER JOIN PHYSICAL_BOOK pb ON pb.resource_id = bc.resource_id " +
                     "INNER JOIN RESOURCE r ON r.resource_id = pb.resource_id " +
                     "WHERE bc.copy_id = ? " +
                     "FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("copyId", rs.getString("copy_id"));
                    map.put("resourceId", rs.getInt("resource_id"));
                    map.put("availabilityStatus", rs.getString("availability_status"));
                    map.put("isPublic", rs.getBoolean("is_public"));
                    return map;
                }
            }
        }
        return null;
    }

    public boolean lockCopyForUpdate(Connection conn, String copyId) throws SQLException {
        String sql = "SELECT copy_id FROM BOOK_COPY WHERE copy_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertBorrow(Connection conn, String memberId, String copyId, LocalDate borrowDate, LocalDate dueDate) throws SQLException {
        String sql = "INSERT INTO BORROW (member_id, copy_id, borrow_date, due_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, memberId);
            ps.setString(2, copyId);
            ps.setDate(3, Date.valueOf(borrowDate));
            ps.setDate(4, Date.valueOf(dueDate));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Không thể lấy borrow_id sau khi INSERT BORROW.");
    }

    public void updateReturnDate(Connection conn, int borrowId, LocalDate returnDate) throws SQLException {
        String sql = "UPDATE BORROW SET return_date = ? WHERE borrow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            ps.setInt(2, borrowId);
            ps.executeUpdate();
        }
    }

    public void updateCopyAvailability(Connection conn, String copyId, String availabilityStatus) throws SQLException {
        String sql = "UPDATE BOOK_COPY SET availability_status = ? WHERE copy_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, availabilityStatus);
            ps.setString(2, copyId);
            ps.executeUpdate();
        }
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
     * Retrieves all borrows with member, book, and latest fine details directly from base tables
     * without using VIEW_BORROWS_DETAILED.
     */
    public List<Map<String, Object>> findAllBorrowsDetailedRaw() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.member_id, um.person_id, p.name AS member_name, um.member_type, " +
                     "       b.copy_id, r.title AS book_title, b.borrow_date, b.due_date, b.return_date, " +
                     "       f_latest.fine_id, f_latest.total_fine_amount AS fine_amount, f_latest.payment_status AS fine_status " +
                     "FROM BORROW b " +
                     "INNER JOIN UNIVERSITY_MEMBER um ON um.member_id = b.member_id " +
                     "INNER JOIN PERSON p ON p.person_id = um.person_id " +
                     "INNER JOIN BOOK_COPY bc ON bc.copy_id = b.copy_id " +
                     "INNER JOIN PHYSICAL_BOOK pb ON pb.resource_id = bc.resource_id " +
                     "INNER JOIN RESOURCE r ON r.resource_id = pb.resource_id " +
                     "LEFT JOIN ( " +
                     "    SELECT fine_id, borrow_id, total_fine_amount, payment_status " +
                     "    FROM ( " +
                     "        SELECT f.fine_id, f.borrow_id, f.total_fine_amount, f.payment_status, " +
                     "               ROW_NUMBER() OVER (PARTITION BY f.borrow_id ORDER BY f.fine_id DESC) AS rn " +
                     "        FROM FINE f " +
                     "    ) ranked " +
                     "    WHERE rn = 1 " +
                     ") f_latest ON f_latest.borrow_id = b.borrow_id " +
                     "ORDER BY b.borrow_id DESC";
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
     * Retrieves all book copies directly from BOOK_COPY, PHYSICAL_BOOK, RESOURCE, and RESOURCE_LICENSE
     * without using VIEW_BOOKS_DETAILED.
     */
    public List<Map<String, Object>> findAllCopiesDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT bc.copy_id, r.resource_id, r.title AS book_title, bc.availability_status, " +
                     "       pb.shelf_location, r.is_public, COALESCE(rl_agg.required_license_count, 0) AS required_license_count " +
                     "FROM BOOK_COPY bc " +
                     "INNER JOIN PHYSICAL_BOOK pb ON pb.resource_id = bc.resource_id " +
                     "INNER JOIN RESOURCE r ON r.resource_id = pb.resource_id " +
                     "LEFT JOIN ( " +
                     "    SELECT resource_id, COUNT(*) AS required_license_count " +
                     "    FROM RESOURCE_LICENSE " +
                     "    GROUP BY resource_id " +
                     ") rl_agg ON rl_agg.resource_id = r.resource_id " +
                     "ORDER BY bc.copy_id ASC";
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
