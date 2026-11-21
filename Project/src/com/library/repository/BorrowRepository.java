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
import java.util.List;

/**
 * Repository class handling MySQL data persistence for Borrow entity.
 * Uses PreparedStatements and try-with-resources for reliable SQL execution.
 */
public class BorrowRepository {

    /**
     * Inserts a new Borrow record using a provided active Connection (for transaction support).
     */
    public void insertBorrow(Connection conn, Borrow borrow) {
        String sql = "INSERT INTO BORROW (member_person_id, copy_id, borrow_date, due_date, return_date) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, borrow.getMemberPersonId());
            ps.setInt(2, borrow.getCopyId());
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

    /**
     * Inserts a new Borrow record into MySQL and updates the object's borrowId.
     */
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
        String sql = "SELECT borrow_id, member_person_id, copy_id, borrow_date, due_date, return_date " +
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
     * Finds all borrows that are currently overdue (return_date IS NULL AND due_date < CURRENT_DATE).
     */
    public List<Borrow> findOverdueBorrows() {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT borrow_id, member_person_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW WHERE return_date IS NULL AND due_date < CURRENT_DATE() " +
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
     * Finds all borrow records for a specific member.
     */
    public List<Borrow> findByMemberId(int memberPersonId) {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT borrow_id, member_person_id, copy_id, borrow_date, due_date, return_date " +
                     "FROM BORROW WHERE member_person_id = ? ORDER BY borrow_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberPersonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBorrow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu mượn của thành viên " + memberPersonId + ": " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Updates an existing Borrow record using an active Connection (for transaction support).
     */
    public void updateBorrow(Connection conn, Borrow borrow) {
        String sql = "UPDATE BORROW SET member_person_id = ?, copy_id = ?, borrow_date = ?, " +
                     "due_date = ?, return_date = ? WHERE borrow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrow.getMemberPersonId());
            ps.setInt(2, borrow.getCopyId());
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

    /**
     * Updates an existing Borrow record in MySQL.
     */
    public void updateBorrow(Borrow borrow) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateBorrow(conn, borrow);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi cập nhật phiếu mượn: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a Borrow record by its primary key borrowId.
     */
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

    /**
     * Retrieves all Borrow records.
     */
    public List<Borrow> findAll() {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT borrow_id, member_person_id, copy_id, borrow_date, due_date, return_date " +
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
     * Checks if a book copy is currently available using an active Connection.
     */
    public boolean isCopyAvailable(Connection conn, int copyId) {
        String sql = "SELECT availability_status FROM BOOK_COPY WHERE copy_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, copyId);
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

    /**
     * Checks if a book copy is currently available for borrowing.
     */
    public boolean isCopyAvailable(int copyId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return isCopyAvailable(conn, copyId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi kiểm tra trạng thái bản sao: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the availability status of a book copy in BOOK_COPY using an active Connection.
     */
    public void updateCopyAvailability(Connection conn, int copyId, String status) {
        String sql = "UPDATE BOOK_COPY SET availability_status = ? WHERE copy_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, copyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi cập nhật trạng thái bản sao ID " + copyId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates the availability status of a book copy in BOOK_COPY.
     */
    public void updateCopyAvailability(int copyId, String status) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateCopyAvailability(conn, copyId, status);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi cập nhật trạng thái bản sao: " + e.getMessage(), e);
        }
    }

    /**
     * Counts how many books a member is currently borrowing (return_date IS NULL).
     * Used to enforce borrow quota limits (BR05).
     */
    public int countActiveBorrowsByMember(int memberPersonId) {
        String sql = "SELECT COUNT(*) AS active_count FROM BORROW WHERE member_person_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberPersonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("active_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi đếm số sách đang mượn của thành viên " + memberPersonId + ": " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Checks if a member currently has any overdue borrows not yet returned.
     */
    public boolean hasOverdueBorrows(int memberPersonId) {
        String sql = "SELECT COUNT(*) AS overdue_count FROM BORROW " +
                     "WHERE member_person_id = ? AND return_date IS NULL AND due_date < CURRENT_DATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberPersonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("overdue_count") > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi kiểm tra sách quá hạn của thành viên " + memberPersonId + ": " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Retrieves all borrows joined with Member and Book details for display on UI.
     */
    public List<java.util.Map<String, Object>> findAllBorrowsDetailed() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.member_person_id, p.name AS member_name, p.person_type, " +
                     "       b.copy_id, bc.barcode, r.title AS book_title, " +
                     "       b.borrow_date, b.due_date, b.return_date " +
                     "FROM BORROW b " +
                     "JOIN PERSON p ON b.member_person_id = p.person_id " +
                     "JOIN BOOK_COPY bc ON b.copy_id = bc.copy_id " +
                     "JOIN RESOURCE r ON bc.resource_id = r.resource_id " +
                     "ORDER BY b.borrow_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("borrowId", rs.getInt("borrow_id"));
                map.put("memberPersonId", rs.getInt("member_person_id"));
                map.put("memberName", rs.getString("member_name"));
                map.put("memberType", rs.getString("person_type"));
                map.put("copyId", rs.getInt("copy_id"));
                map.put("barcode", rs.getString("barcode"));
                map.put("bookTitle", rs.getString("book_title"));
                Date bDate = rs.getDate("borrow_date");
                Date dDate = rs.getDate("due_date");
                Date rDate = rs.getDate("return_date");
                map.put("borrowDate", bDate != null ? bDate.toString() : null);
                map.put("dueDate", dDate != null ? dDate.toString() : null);
                map.put("returnDate", rDate != null ? rDate.toString() : null);
                boolean isOverdue = rDate == null && dDate != null && dDate.toLocalDate().isBefore(LocalDate.now());
                map.put("isOverdue", isOverdue);
                map.put("status", rDate != null ? "Returned" : (isOverdue ? "Overdue" : "Active"));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu danh sách mượn chi tiết: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all book copies joined with book titles for UI selection.
     */
    public List<java.util.Map<String, Object>> findAllCopiesDetailed() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT bc.copy_id, bc.resource_id, r.title AS book_title, bc.barcode, " +
                     "       bc.condition_status, bc.availability_status " +
                     "FROM BOOK_COPY bc " +
                     "JOIN RESOURCE r ON bc.resource_id = r.resource_id " +
                     "ORDER BY bc.copy_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("copyId", rs.getInt("copy_id"));
                map.put("resourceId", rs.getInt("resource_id"));
                map.put("bookTitle", rs.getString("book_title"));
                map.put("barcode", rs.getString("barcode"));
                map.put("conditionStatus", rs.getString("condition_status"));
                map.put("availabilityStatus", rs.getString("availability_status"));
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
                rs.getInt("member_person_id"),
                rs.getInt("copy_id"),
                borrowDateSql != null ? borrowDateSql.toLocalDate() : null,
                dueDateSql != null ? dueDateSql.toLocalDate() : null,
                returnDateSql != null ? returnDateSql.toLocalDate() : null
        );
    }
}
