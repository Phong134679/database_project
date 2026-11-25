package com.library.service;

import com.library.exception.BookNotAvailableException;
import com.library.model.Borrow;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.util.DatabaseConnection;
import com.library.util.SqlExceptionMapper;
import com.library.validation.BorrowValidator;
import com.library.validation.DataValidator;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service class implementing business logic operations for borrow transactions.
 * Interacts with MySQL via BorrowRepository, FineRepository, and MemberService.
 * Enforces Test 01 (BR01), BR05, BR10, and Test 03 (FR05).
 */
public class BorrowService {

    public static final double OVERDUE_PENALTY_PER_BORROW = 50000.0; // 50.000 VND cho mỗi lượt mượn quá hạn (x 50)

    private BorrowRepository borrowRepository;
    private FineRepository fineRepository;
    private MemberService memberService;

    public BorrowService() {
        this.borrowRepository = new BorrowRepository();
        this.fineRepository = new FineRepository();
        this.memberService = new MemberService();
    }

    public BorrowService(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = new FineRepository();
        this.memberService = new MemberService();
    }

    public BorrowService(BorrowRepository borrowRepository, FineRepository fineRepository) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = fineRepository;
        this.memberService = new MemberService();
    }

    public BorrowService(BorrowRepository borrowRepository, FineRepository fineRepository, MemberService memberService) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = fineRepository;
        this.memberService = memberService;
    }

    /**
     * Processes a new book borrowing transaction by delegating business checks
     * and atomic updates to MySQL Stored Procedure sp_borrow_book.
     */
    public boolean processNewBorrow(int memberId, String copyId) {
        DataValidator.validatePositiveId(memberId, "Member Person ID");
        DataValidator.validateNonEmptyString(copyId, "Copy ID");

        int numericCopyId;
        try {
            numericCopyId = Integer.parseInt(copyId.trim());
        } catch (NumberFormatException e) {
            throw new BookNotAvailableException("Bản sao sách [" + copyId + "] không tồn tại.");
        }
        DataValidator.validatePositiveId(numericCopyId, "Copy ID");

        String sql = "{CALL sp_borrow_book(?, ?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, memberId);
            cs.setInt(2, numericCopyId);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.DATE);
            cs.execute();

            int borrowId = cs.getInt(3);
            Date dueDate = cs.getDate(4);
            System.out.println("[BorrowService] SUCCESS: Borrow ID " + borrowId +
                    " created for Member " + memberId + ", Copy " + numericCopyId +
                    " (Due Date: " + dueDate + ")");
            return true;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return false;
        }
    }

    public boolean processNewBorrow(int memberId, int copyId) {
        return processNewBorrow(memberId, String.valueOf(copyId));
    }

    /**
     * Processes book return using MySQL server CURDATE() when returnDate is null.
     */
    public boolean returnBook(int borrowId) {
        return returnBook(borrowId, null);
    }

    /**
     * Processes book return by delegating business checks, copy release, and
     * automatic overdue fine creation to MySQL Stored Procedure sp_return_book.
     */
    public boolean returnBook(int borrowId, LocalDate returnDate) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");

        String sql = "{CALL sp_return_book(?, ?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, borrowId);
            if (returnDate != null) {
                cs.setDate(2, Date.valueOf(returnDate));
            } else {
                cs.setNull(2, Types.DATE);
            }
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.DECIMAL);
            cs.execute();

            int fineId = cs.getInt(3);
            BigDecimal fineAmount = cs.getBigDecimal(4);
            if (fineId > 0) {
                System.out.println("[BorrowService] OVERDUE DETECTED. Generated Fine ID: " + fineId +
                        " (Amount: " + String.format("%,.0f", fineAmount != null ? fineAmount.doubleValue() : 0.0) + " VND)");
            }
            return true;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return false;
        }
    }

    /**
     * Retrieves all borrows with detailed member name and book title for UI.
     */
    public List<Map<String, Object>> getAllBorrowsDetailed() {
        return borrowRepository.findAllBorrowsDetailed();
    }

    /**
     * Retrieves all book copies for UI.
     */
    public List<Map<String, Object>> getAllCopiesDetailed() {
        return borrowRepository.findAllCopiesDetailed();
    }

    /**
     * Calculates the overdue penalty amount for a borrow transaction.
     */
    public double calculateLatePenalty(int borrowId) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");
        Borrow borrow = borrowRepository.findById(borrowId);
        BorrowValidator.validateBorrowExists(borrow, borrowId);

        if (borrow.getDueDate() == null) {
            return 0.0;
        }

        LocalDate effectiveReturnDate = borrow.getReturnDate() != null ? borrow.getReturnDate() : LocalDate.now();
        if (effectiveReturnDate.isAfter(borrow.getDueDate())) {
            return OVERDUE_PENALTY_PER_BORROW;
        }

        return 0.0;
    }

    /**
     * Calculates total overdue penalty for a member based on overdue borrow count x 50.
     */
    public double calculateMemberOverduePenalty(int memberPersonId) {
        DataValidator.validatePositiveId(memberPersonId, "Member Person ID");
        int overdueCount = borrowRepository.countOverdueBorrowsByMember(memberPersonId);
        return overdueCount * OVERDUE_PENALTY_PER_BORROW;
    }

    /**
     * Retrieves all overdue borrow transactions.
     */
    public List<Borrow> getOverdueBorrows() {
        return borrowRepository.findOverdueBorrows();
    }

    /**
     * Retrieves detailed information of a borrow transaction.
     */
    public Borrow getBorrowDetails(int borrowId) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");
        Borrow borrow = borrowRepository.findById(borrowId);
        BorrowValidator.validateBorrowExists(borrow, borrowId);
        return borrow;
    }

    public BorrowRepository getBorrowRepository() {
        return borrowRepository;
    }

    public void setBorrowRepository(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }

    public FineRepository getFineRepository() {
        return fineRepository;
    }

    public void setFineRepository(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }
}
