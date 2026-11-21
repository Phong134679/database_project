package com.library.repository;

import com.library.exception.DatabaseException;
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
     * Inserts a new Fine record using an active Connection (for transaction support).
     */
    public void insertFine(Connection conn, Fine fine) {
        String sql = "INSERT INTO FINE (borrow_id, total_fine_amount, payment_status, due_date) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            throw new DatabaseException("Lỗi khi thêm mới phiếu phạt (insertFine): " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a new Fine record into MySQL and updates the fine's fineId.
     */
    public void insertFine(Fine fine) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            insertFine(conn, fine);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi thêm mới phiếu phạt: " + e.getMessage(), e);
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
            throw new DatabaseException("Lỗi khi tra cứu phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds a Fine record by its associated borrowId using an active Connection.
     */
    public Fine findByBorrowId(Connection conn, int borrowId) {
        String sql = "SELECT fine_id, borrow_id, total_fine_amount, payment_status, due_date " +
                     "FROM FINE WHERE borrow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFine(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu phiếu phạt theo Borrow ID " + borrowId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds a Fine record by its associated borrowId.
     */
    public Fine findByBorrowId(int borrowId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByBorrowId(conn, borrowId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi tra cứu phiếu phạt: " + e.getMessage(), e);
        }
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
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ phiếu phạt: " + e.getMessage(), e);
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
            throw new DatabaseException("Lỗi khi truy xuất danh sách phạt chưa nộp: " + e.getMessage(), e);
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
            throw new DatabaseException("Lỗi khi cập nhật phiếu phạt ID " + fine.getFineId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates only the payment status of a Fine using an active Connection.
     */
    public void updatePaymentStatus(Connection conn, int fineId, String paymentStatus) {
        String sql = "UPDATE FINE SET payment_status = ? WHERE fine_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentStatus);
            ps.setInt(2, fineId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi cập nhật trạng thái phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates only the payment status of a Fine.
     */
    public void updatePaymentStatus(int fineId, String paymentStatus) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updatePaymentStatus(conn, fineId, paymentStatus);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối khi cập nhật trạng thái phiếu phạt: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all fines with detailed member, borrow, and payment calculation for UI display.
     */
    public List<java.util.Map<String, Object>> findAllFinesDetailed() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT f.fine_id, f.borrow_id, f.total_fine_amount, f.payment_status, f.due_date, " +
                     "       b.member_person_id, p.name AS member_name, r.title AS book_title, " +
                     "       COALESCE(SUM(pt.amount_paid), 0.00) AS total_paid " +
                     "FROM FINE f " +
                     "JOIN BORROW b ON f.borrow_id = b.borrow_id " +
                     "JOIN PERSON p ON b.member_person_id = p.person_id " +
                     "JOIN BOOK_COPY bc ON b.copy_id = bc.copy_id " +
                     "JOIN RESOURCE r ON bc.resource_id = r.resource_id " +
                     "LEFT JOIN PAYMENT_TRANSACTION pt ON f.fine_id = pt.fine_id " +
                     "GROUP BY f.fine_id, f.borrow_id, f.total_fine_amount, f.payment_status, f.due_date, " +
                     "         b.member_person_id, p.name, r.title " +
                     "ORDER BY f.fine_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                int fineId = rs.getInt("fine_id");
                map.put("fineId", fineId);
                map.put("borrowId", rs.getInt("borrow_id"));
                map.put("memberPersonId", rs.getInt("member_person_id"));
                map.put("memberName", rs.getString("member_name"));
                map.put("bookTitle", rs.getString("book_title"));
                BigDecimal totalAmount = rs.getBigDecimal("total_fine_amount");
                BigDecimal paidAmount = rs.getBigDecimal("total_paid");
                BigDecimal remaining = totalAmount.subtract(paidAmount);
                if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                    remaining = BigDecimal.ZERO;
                }
                map.put("totalFineAmount", totalAmount);
                map.put("paidAmount", paidAmount);
                map.put("remainingBalance", remaining);
                map.put("paymentStatus", rs.getString("payment_status"));
                Date dDate = rs.getDate("due_date");
                map.put("dueDate", dDate != null ? dDate.toString() : null);
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu danh sách phiếu phạt chi tiết: " + e.getMessage(), e);
        }
        return list;
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
            throw new DatabaseException("Lỗi khi xóa phiếu phạt ID " + fineId + ": " + e.getMessage(), e);
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
            throw new DatabaseException("Lỗi khi kiểm tra nợ phạt của thành viên " + memberPersonId + ": " + e.getMessage(), e);
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
