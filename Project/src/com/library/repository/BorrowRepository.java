package com.library.repository;

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
     * Inserts a new Borrow record into MySQL and updates the object's borrowId.
     */
    public void insertBorrow(Borrow borrow) {
        String sql = "INSERT INTO BORROW (member_person_id, copy_id, borrow_date, due_date, return_date) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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
            System.err.println("[BorrowRepository] Error inserting borrow: " + e.getMessage());
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
            System.err.println("[BorrowRepository] Error finding borrow by id " + borrowId + ": " + e.getMessage());
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
            System.err.println("[BorrowRepository] Error finding overdue borrows: " + e.getMessage());
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
            System.err.println("[BorrowRepository] Error finding borrows for member " + memberPersonId + ": " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an existing Borrow record in MySQL.
     */
    public void updateBorrow(Borrow borrow) {
        String sql = "UPDATE BORROW SET member_person_id = ?, copy_id = ?, borrow_date = ?, " +
                     "due_date = ?, return_date = ? WHERE borrow_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
            System.err.println("[BorrowRepository] Error updating borrow " + borrow.getBorrowId() + ": " + e.getMessage());
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
            System.err.println("[BorrowRepository] Error deleting borrow " + borrowId + ": " + e.getMessage());
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
            System.err.println("[BorrowRepository] Error fetching all borrows: " + e.getMessage());
        }
        return list;
    }

    /**
     * Checks if a book copy is currently available for borrowing.
     */
    public boolean isCopyAvailable(int copyId) {
        String sql = "SELECT availability_status FROM BOOK_COPY WHERE copy_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Available".equalsIgnoreCase(rs.getString("availability_status"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BorrowRepository] Error checking copy availability: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates the availability status of a book copy in BOOK_COPY.
     */
    public void updateCopyAvailability(int copyId, String status) {
        String sql = "UPDATE BOOK_COPY SET availability_status = ? WHERE copy_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, copyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BorrowRepository] Error updating copy status: " + e.getMessage());
        }
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
