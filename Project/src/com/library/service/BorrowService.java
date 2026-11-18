package com.library.service;

import com.library.exception.InvalidDateRangeException;
import com.library.model.Borrow;
import com.library.model.Fine;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.validation.BorrowValidator;
import com.library.validation.DataValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service class implementing business logic operations for borrow transactions.
 * Interacts with MySQL via BorrowRepository, FineRepository, and MemberService.
 * Enforces Test 01 (BR01), BR05, BR10, and Test 03 (FR05).
 */
public class BorrowService {

    private static final double DAILY_PENALTY_RATE = 10000.0; // 10,000 VND per day overdue

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
     * Processes a new book borrowing transaction.
     * Enforces Test 01 (BR01: external user rejection), BR05 (role-based quotas and durations),
     * and BR10 (physical book copy tracking).
     * Throws specific BusinessException/ValidationException on failure, returns true on success.
     */
    public boolean processNewBorrow(int memberId, int copyId) {
        // 1. Kiểm tra tính hợp lệ của tham số ID
        DataValidator.validatePositiveId(memberId, "Member Person ID");
        DataValidator.validatePositiveId(copyId, "Copy ID");

        // 2. Kiểm tra tính hợp lệ của người mượn (Test 01, BR01, BR05)
        memberService.isEligibleToBorrow(memberId);

        // 3. Kiểm tra tình trạng bản sao sách (BR10)
        boolean isAvailable = borrowRepository.isCopyAvailable(copyId);
        BorrowValidator.validateCopyAvailable(isAvailable, copyId);

        // 4. Phân định thời hạn mượn theo vai trò thành viên (BR05)
        int loanDays = memberService.getLoanDurationDays(memberId);
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(loanDays);

        Borrow borrow = new Borrow(0, memberId, copyId, today, dueDate, null);
        borrowRepository.insertBorrow(borrow);

        if (borrow.getBorrowId() > 0) {
            borrowRepository.updateCopyAvailability(copyId, "Borrowed");
            System.out.println("[BorrowService] SUCCESS: Borrow ID " + borrow.getBorrowId() +
                    " created for Member " + memberId + ", Copy " + copyId +
                    " (Loan Duration: " + loanDays + " days, Due Date: " + dueDate + ")");
            return true;
        }

        return false;
    }

    /**
     * Processes book return using the current date as returnDate.
     */
    public boolean returnBook(int borrowId) {
        return returnBook(borrowId, LocalDate.now());
    }

    /**
     * Processes book return with a specified return date.
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
        borrowRepository.updateBorrow(borrow);
        borrowRepository.updateCopyAvailability(borrow.getCopyId(), "Available");

        // Test 03 & FR05: Tự động phát sinh phiếu phạt khi trả sách quá hạn
        if (borrow.getDueDate() != null && actualReturnDate.isAfter(borrow.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(borrow.getDueDate(), actualReturnDate);
            BigDecimal penaltyAmount = BigDecimal.valueOf(daysLate * DAILY_PENALTY_RATE);

            // Kiểm tra xem đã có phiếu phạt cho lượt mượn này chưa (tránh trùng lặp)
            Fine existingFine = fineRepository.findByBorrowId(borrowId);
            if (existingFine == null) {
                LocalDate fineDueDate = actualReturnDate.plusDays(14); // Hạn thanh toán phạt 14 ngày
                Fine newFine = new Fine(0, borrowId, penaltyAmount, "Unpaid", fineDueDate);
                fineRepository.insertFine(newFine);
                System.out.println("[BorrowService] OVERDUE DETECTED (" + daysLate + " days late). " +
                        "Generated Fine ID: " + newFine.getFineId() + " (Amount: " +
                        String.format("%,.0f", penaltyAmount.doubleValue()) + " VND, Due Date: " + fineDueDate + ")");
            }
        }

        return true;
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
            long daysLate = ChronoUnit.DAYS.between(borrow.getDueDate(), effectiveReturnDate);
            return daysLate * DAILY_PENALTY_RATE;
        }

        return 0.0;
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
