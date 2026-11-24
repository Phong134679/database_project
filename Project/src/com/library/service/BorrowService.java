package com.library.service;

import com.library.exception.DatabaseException;
import com.library.exception.InvalidDateRangeException;
import com.library.exception.LibraryException;
import com.library.model.Borrow;
import com.library.model.Fine;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.util.DatabaseConnection;
import com.library.validation.BorrowValidator;
import com.library.validation.DataValidator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Service class implementing business logic operations for borrow transactions.
 * Interacts with MySQL via BorrowRepository, FineRepository, and MemberService.
 * Enforces Test 01 (BR01), BR05, BR10, and Test 03 (FR05).
 */
public class BorrowService {

    public static final double OVERDUE_PENALTY_PER_BORROW = 50000.0; // 50.000 VND cho mỗi lượt mượn quá hạn (x 50)
    private static final double DAILY_PENALTY_RATE = 50000.0;

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
     * Processes a new book borrowing transaction within a single atomic Database Transaction.
     * Enforces Test 01 (BR01: external user rejection), BR05 (role-based quotas and durations),
     * and BR10 (physical book copy tracking).
     * Throws specific BusinessException/ValidationException on failure, returns true on success.
     */
    public boolean processNewBorrow(int memberId, String copyId) {
        // 1. Kiểm tra tính hợp lệ của tham số ID
        DataValidator.validatePositiveId(memberId, "Member Person ID");
        DataValidator.validateNonEmptyString(copyId, "Copy ID");

        // 2. Kiểm tra tính hợp lệ của người mượn (Test 01, BR01, BR05)
        memberService.isEligibleToBorrow(memberId);

        // 3. Phân định thời hạn mượn theo vai trò thành viên (BR05)
        int loanDays = memberService.getLoanDurationDays(memberId);
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(loanDays);

        Borrow borrow = new Borrow(0, memberId, copyId, today, dueDate, null);

        // 4. Thực thi kiểm tra trạng thái và tạo lượt mượn trong cùng 1 Transaction
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Kiểm tra trạng thái bản sao sách trong transaction (BR10)
            boolean isAvailable = borrowRepository.isCopyAvailable(conn, copyId);
            BorrowValidator.validateCopyAvailable(isAvailable, copyId);

            borrowRepository.insertBorrow(conn, borrow);
            borrowRepository.updateCopyAvailability(conn, copyId, "Borrowed");

            conn.commit();

            System.out.println("[BorrowService] SUCCESS: Borrow ID " + borrow.getBorrowId() +
                    " created for Member " + memberId + ", Copy " + copyId +
                    " (Loan Duration: " + loanDays + " days, Due Date: " + dueDate + ")");
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[BorrowService] Rollback failed: " + rollbackEx.getMessage());
                }
            }
            if (e instanceof LibraryException) {
                throw (LibraryException) e;
            }
            throw new DatabaseException("Lỗi giao dịch khi mượn sách: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // ignore
                }
            }
        }
    }

    public boolean processNewBorrow(int memberId, int copyId) {
        return processNewBorrow(memberId, String.valueOf(copyId));
    }

    /**
     * Processes book return using the current date as returnDate.
     */
    public boolean returnBook(int borrowId) {
        return returnBook(borrowId, LocalDate.now());
    }

    /**
     * Processes book return with a specified return date within a single atomic Database Transaction.
     * Automatically generates a FINE record if returned past due_date (Validation Case Test 03 & FR05).
     * Throws specific BusinessException/ValidationException on failure, returns true on success.
     */
    public boolean returnBook(int borrowId, LocalDate returnDate) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");

        Borrow borrow = borrowRepository.findById(borrowId);
        BorrowValidator.validateReturnEligibility(borrow, borrowId);

        LocalDate actualReturnDate = returnDate != null ? returnDate : LocalDate.now();
        if (borrow.getBorrowDate() != null && actualReturnDate.isBefore(borrow.getBorrowDate())) {
            throw new InvalidDateRangeException("Ngày trả (" + actualReturnDate + ") không thể trước ngày mượn (" + borrow.getBorrowDate() + ").");
        }

        borrow.setReturnDate(actualReturnDate);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            borrowRepository.updateBorrow(conn, borrow);
            borrowRepository.updateCopyAvailability(conn, borrow.getCopyId(), "Available");

            // Test 03 & FR05: Tự động phát sinh phiếu phạt khi trả sách quá hạn (Số lượng borrow quá hạn x 50 = 50.000 VND / borrow)
            if (borrow.getDueDate() != null && actualReturnDate.isAfter(borrow.getDueDate())) {
                BigDecimal penaltyAmount = BigDecimal.valueOf(OVERDUE_PENALTY_PER_BORROW);

                // Kiểm tra xem đã có phiếu phạt cho lượt mượn này chưa (tránh trùng lặp)
                Fine existingFine = fineRepository.findByBorrowId(conn, borrowId);
                if (existingFine == null) {
                    Fine newFine = new Fine(0, borrowId, penaltyAmount, "Unpaid");
                    fineRepository.insertFine(conn, newFine);
                    System.out.println("[BorrowService] OVERDUE DETECTED. " +
                            "Generated Fine ID: " + newFine.getFineId() + " (Amount: " +
                            String.format("%,.0f", penaltyAmount.doubleValue()) + " VND)");
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[BorrowService] Rollback failed: " + rollbackEx.getMessage());
                }
            }
            if (e instanceof LibraryException) {
                throw (LibraryException) e;
            }
            throw new DatabaseException("Lỗi giao dịch khi trả sách: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // ignore
                }
            }
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
