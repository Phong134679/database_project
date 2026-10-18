package com.library.controller;

import com.library.model.Borrow;
import com.library.service.BorrowService;

import java.util.List;

/**
 * Controller class handling presentation and user request routing to service layer.
 */
public class LibraryController {

    private BorrowService borrowService;

    public LibraryController() {
        this.borrowService = new BorrowService();
    }

    public LibraryController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    /**
     * Handles borrow requests from members.
     */
    public boolean handleBorrowRequest(int memberId, int copyId) {
        System.out.println("\n[LibraryController] Processing borrow request: Member " + memberId + ", Copy " + copyId);
        boolean success = borrowService.processNewBorrow(memberId, copyId);
        if (success) {
            System.out.println("[LibraryController] SUCCESS: Book borrowed successfully.");
        } else {
            System.out.println("[LibraryController] FAILED: Unable to process borrow request.");
        }
        return success;
    }

    /**
     * Handles return requests and displays overdue penalty if incurred.
     */
    public boolean handleReturnRequest(int borrowId) {
        System.out.println("\n[LibraryController] Processing return request for Borrow ID: " + borrowId);
        double penalty = borrowService.calculateLatePenalty(borrowId);
        boolean success = borrowService.returnBook(borrowId);
        if (success) {
            System.out.println("[LibraryController] SUCCESS: Book returned.");
            if (penalty > 0) {
                System.out.println("[LibraryController] NOTICE: Late penalty incurred: " + String.format("%,.0f", penalty) + " VND");
            }
        } else {
            System.out.println("[LibraryController] FAILED: Could not process return.");
        }
        return success;
    }

    /**
     * Displays all overdue borrow transactions in the system.
     */
    public void displayOverdueMembers() {
        System.out.println("\n[LibraryController] === OVERDUE BORROW RECORDS ===");
        List<Borrow> overdueList = borrowService.getOverdueBorrows();
        if (overdueList == null || overdueList.isEmpty()) {
            System.out.println("No overdue records found.");
            return;
        }

        for (Borrow b : overdueList) {
            double penalty = borrowService.calculateLatePenalty(b.getBorrowId());
            System.out.println(String.format("Borrow ID: %d | Member ID: %d | Copy ID: %d | Due Date: %s | Estimated Penalty: %,.0f VND",
                    b.getBorrowId(), b.getMemberPersonId(), b.getCopyId(), b.getDueDate(), penalty));
        }
    }

    /**
     * Displays details of a specific borrow record.
     */
    public void displayBorrowHistory(int borrowId) {
        Borrow b = borrowService.getBorrowDetails(borrowId);
        if (b != null) {
            System.out.println("[LibraryController] Details: " + b);
        } else {
            System.out.println("[LibraryController] No record found for ID: " + borrowId);
        }
    }

    public BorrowService getBorrowService() {
        return borrowService;
    }

    public void setBorrowService(BorrowService borrowService) {
        this.borrowService = borrowService;
    }
}
