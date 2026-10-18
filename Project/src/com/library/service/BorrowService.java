package com.library.service;

import com.library.model.Borrow;
import com.library.repository.BorrowRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service class implementing business logic operations for borrow transactions.
 * Interacts with MySQL via BorrowRepository.
 */
public class BorrowService {

    private static final double DAILY_PENALTY_RATE = 10000.0; // 10,000 VND per day overdue
    private static final int DEFAULT_LOAN_DAYS = 14;

    private BorrowRepository borrowRepository;

    public BorrowService() {
        this.borrowRepository = new BorrowRepository();
    }

    public BorrowService(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }

    /**
     * Processes a new book borrowing transaction.
     * Verifies physical book copy availability before recording borrow in MySQL.
     */
    public boolean processNewBorrow(int memberId, int copyId) {
        if (!borrowRepository.isCopyAvailable(copyId)) {
            System.err.println("[BorrowService] Copy ID " + copyId + " is not available for borrowing.");
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(DEFAULT_LOAN_DAYS);

        Borrow borrow = new Borrow(0, memberId, copyId, today, dueDate, null);
        borrowRepository.insertBorrow(borrow);

        if (borrow.getBorrowId() > 0) {
            borrowRepository.updateCopyAvailability(copyId, "Borrowed");
            return true;
        }

        return false;
    }

    /**
     * Processes book return, updates return date and resets copy availability.
     */
    public boolean returnBook(int borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId);
        if (borrow == null) {
            System.err.println("[BorrowService] Borrow record not found for ID: " + borrowId);
            return false;
        }

        if (borrow.getReturnDate() != null) {
            System.err.println("[BorrowService] Borrow ID " + borrowId + " was already returned on: " + borrow.getReturnDate());
            return false;
        }

        LocalDate returnDate = LocalDate.now();
        borrow.setReturnDate(returnDate);
        borrowRepository.updateBorrow(borrow);
        borrowRepository.updateCopyAvailability(borrow.getCopyId(), "Available");
        return true;
    }

    /**
     * Calculates the overdue penalty amount for a borrow transaction.
     */
    public double calculateLatePenalty(int borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId);
        if (borrow == null || borrow.getDueDate() == null) {
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
        return borrowRepository.findById(borrowId);
    }

    public BorrowRepository getBorrowRepository() {
        return borrowRepository;
    }

    public void setBorrowRepository(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }
}
