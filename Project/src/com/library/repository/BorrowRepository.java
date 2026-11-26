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
 * Repository class handling MySQL data persistence for BORROW entity.
 * In v2 schema, BORROW.member_id is VARCHAR(20) referencing UNIVERSITY_MEMBER(member_id)
 * and copy_id is VARCHAR(30) referencing BOOK_COPY(copy_id).
 */
public class BorrowRepository {

    /**
     * Inserts a new Borrow record using a provided active Connection (for transaction support).
     */
    public void insertBorrow(Connection conn, Borrow borrow) {
        String sql = "INSERT INTO BORROW (member_id, copy_id, borrow_date, due_date, return_date) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, borrow.getMemberId());
            ps.setString(2, borrow.getCopyId());
            ps.setDate(3, borrow.getBorrowDate() != null ? Date.valueOf(borrow.getBorrowDate()) : Date.valueOf(LocalDate.now()));
            ps.setDate(4, borrow.getDueDate() != null ? Date.valueOf(borrow.getDueDate()) : null);
            if (borrow.getReturnDate() != null) {
                ps.setDate(5, Date.valueOf(borrow.getReturnDate()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        borrow.setBorrowId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thêm mới phiếu mượn (insertBorrow): " + e.getMessage(), e);
        }
    }

    public void insertBorrow(Borrow borrow) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            insertBorrow(conn, borrow);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi thêm mới phiếu mượn: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a Borrow record by its primary key borrowId.
     */
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

    /**
     * Finds all borrows that are currently overdue (return_date IS NULL AND due_date < CURDATE()).
     */
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

    /**
     * Finds all borrow records for a specific member_id.
     */
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

    /**
     * Updates an existing Borrow record using an active Connection (for transaction support).
     */
    public void updateBorrow(Connection conn, Borrow borrow) {
        String sql = "UPDATE BORROW SET member_id = ?, copy_id = ?, borrow_date = ?, " +
                     "due_date = ?, return_date = ? WHERE borrow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, borrow.getMemberId());
            ps.setString(2, borrow.getCopyId());
            ps.setDate(3, borrow.getBorrowDate() != null ? Date.valueOf(borrow.getBorrowDate()) : null);
            ps.setDate(4, borrow.getDueDate() != null ? Date.valueOf(borrow.getDueDate()) : null);
            if (borrow.getReturnDate() != null) {
                ps.setDate(5, Date.valueOf(borrow.getReturnDate()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }
            ps.setInt(6, borrow.getBorrowId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi cập nhật phiếu mượn ID " + borrow.getBorrowId() + ": " + e.getMessage(), e);
        }
    }

    public void updateBorrow(Borrow borrow) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateBorrow(conn, borrow);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi cập nhật phiếu mượn: " + e.getMessage(), e);
        }
    }

    public void deleteBorrow(int borrowId) {
        String sql = "DELETE FROM BORROW WHERE borrow_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, borrowId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi xóa phiếu mượn ID " + borrowId + ": " + e.getMessage(), e);
        }
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

    public boolean isCopyAvailable(Connection conn, String copyId) {
        String sql = "SELECT availability_status FROM BOOK_COPY WHERE copy_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Available".equalsIgnoreCase(rs.getString("availability_status"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi kiểm tra trạng thái bản sao ID " + copyId + ": " + e.getMessage(), e);
        }
        return false;
    }

    public boolean isCopyAvailable(String copyId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return isCopyAvailable(conn, copyId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi kiểm tra trạng thái bản sao: " + e.getMessage(), e);
        }
    }

    public void updateCopyAvailability(Connection conn, String copyId, String status) {
        String sql = "UPDATE BOOK_COPY SET availability_status = ? WHERE copy_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, copyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi cập nhật trạng thái bản sao ID " + copyId + ": " + e.getMessage(), e);
        }
    }

    public void updateCopyAvailability(String copyId, String status) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateCopyAvailability(conn, copyId, status);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi cập nhật trạng thái bản sao: " + e.getMessage(), e);
        }
    }

    /**
     * Counts how many books a member is currently borrowing (return_date IS NULL).
     */
    public int countActiveBorrowsByMember(String memberId) {
        String sql = "SELECT COUNT(*) AS active_count FROM BORROW WHERE member_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("active_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi đếm số sách đang mượn của thành viên " + memberId + ": " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Checks if a member currently has any overdue borrows not yet returned.
     */
    public boolean hasOverdueBorrows(String memberId) {
        return countOverdueBorrowsByMember(memberId) > 0;
    }

    /**
     * Counts the number of overdue borrows for a member that are not yet returned.
     */
    public int countOverdueBorrowsByMember(String memberId) {
        String sql = "SELECT COUNT(*) AS overdue_count FROM BORROW " +
                     "WHERE member_id = ? AND return_date IS NULL AND due_date < CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("overdue_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi đếm số sách quá hạn của thành viên " + memberId + ": " + e.getMessage(), e);
        }
        return 0;
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
