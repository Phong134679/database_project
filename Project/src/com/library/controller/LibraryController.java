package com.library.controller;

import com.library.exception.LibraryException;
import com.library.model.AccessLog;
import com.library.model.Borrow;
import com.library.model.DigitalJournal;
import com.library.model.Fine;
import com.library.model.PaymentTransaction;
import com.library.model.UniversityMember;
import com.library.service.BorrowService;
import com.library.service.DigitalJournalService;
import com.library.service.FineService;
import com.library.service.MemberService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller class handling presentation and user request routing to service layer.
 * Coordinates borrowing operations, fine settlements, digital journal access, and member management.
 */
public class LibraryController {

    private BorrowService borrowService;
    private FineService fineService;
    private DigitalJournalService journalService;
    private MemberService memberService;

    public LibraryController() {
        this.borrowService = new BorrowService();
        this.fineService = new FineService();
        this.journalService = new DigitalJournalService();
        this.memberService = new MemberService();
    }

    public LibraryController(BorrowService borrowService) {
        this.borrowService = borrowService;
        this.fineService = new FineService();
        this.journalService = new DigitalJournalService();
        this.memberService = new MemberService();
    }

    public LibraryController(BorrowService borrowService, FineService fineService) {
        this.borrowService = borrowService;
        this.fineService = fineService;
        this.journalService = new DigitalJournalService();
        this.memberService = new MemberService();
    }

    public LibraryController(BorrowService borrowService, FineService fineService,
                             DigitalJournalService journalService, MemberService memberService) {
        this.borrowService = borrowService;
        this.fineService = fineService;
        this.journalService = journalService;
        this.memberService = memberService;
    }

    /**
     * Handles borrow requests from members.
     * Enforces Test 01 (BR01), BR05, and BR10.
     */
    public boolean handleBorrowRequest(int memberId, int copyId) {
        System.out.println("\n[LibraryController] Processing borrow request: Member " + memberId + ", Copy " + copyId);
        try {
            boolean success = borrowService.processNewBorrow(memberId, copyId);
            if (success) {
                System.out.println("[LibraryController] SUCCESS: Book borrowed successfully.");
            }
            return success;
        } catch (LibraryException e) {
            System.err.println("[LibraryController] REJECTED: " + e);
            return false;
        }
    }

    /**
     * Handles return requests and displays overdue penalty or generated fine if incurred.
     */
    public boolean handleReturnRequest(int borrowId) {
        return handleReturnRequest(borrowId, LocalDate.now());
    }

    /**
     * Handles return requests with a specific return date (useful for testing Test 03).
     */
    public boolean handleReturnRequest(int borrowId, LocalDate returnDate) {
        System.out.println("\n[LibraryController] Processing return request for Borrow ID: " + borrowId + " on date: " + returnDate);
        try {
            boolean success = borrowService.returnBook(borrowId, returnDate);
            if (success) {
                System.out.println("[LibraryController] SUCCESS: Book returned successfully.");
                Fine fine = fineService.getFineByBorrowId(borrowId);
                if (fine != null) {
                    System.out.println("[LibraryController] NOTICE: Overdue fine created: Fine ID " + fine.getFineId() +
                            " | Amount: " + String.format("%,.0f", fine.getTotalFineAmount().doubleValue()) + " VND | Status: " + fine.getPaymentStatus());
                }
            }
            return success;
        } catch (LibraryException e) {
            System.err.println("[LibraryController] FAILED: " + e);
            return false;
        }
    }

    /**
     * Handles installment fine payment (Validation Case Test 04 & BR11).
     */
    public boolean handleFinePayment(int fineId, BigDecimal amount, String paymentMethod) {
        System.out.println("\n[LibraryController] Processing fine payment for Fine ID: " + fineId +
                ", Amount: " + (amount != null ? String.format("%,.0f", amount.doubleValue()) : 0) + " VND, Method: " + paymentMethod);
        try {
            boolean success = fineService.payFine(fineId, amount, paymentMethod);
            if (success) {
                BigDecimal remaining = fineService.getRemainingBalance(fineId);
                System.out.println("[LibraryController] SUCCESS: Payment recorded. Remaining balance: " +
                        String.format("%,.0f", remaining.doubleValue()) + " VND");
            }
            return success;
        } catch (LibraryException e) {
            System.err.println("[LibraryController] FAILED: " + e);
            return false;
        }
    }

    /**
     * Handles digital journal access requests (Validation Case Test 02, BR04, BR12, BR13, FR08).
     */
    public boolean handleAccessDigitalJournal(int personId, int journalResourceId, String ipAddress, LocalDate accessDate) {
        System.out.println("\n[LibraryController] Processing digital journal access request: Person " +
                personId + ", Journal Resource " + journalResourceId + " on date: " + accessDate);
        try {
            boolean granted = journalService.accessDigitalJournal(personId, journalResourceId, ipAddress, accessDate);
            if (granted) {
                System.out.println("[LibraryController] RESULT: Access granted. User authorized to read/download.");
            }
            return granted;
        } catch (LibraryException e) {
            System.err.println("[LibraryController] RESULT: Access denied. " + e);
            return false;
        }
    }

    /**
     * Displays all digital journals available in the catalog.
     */
    public void displayAllJournals() {
        System.out.println("\n[LibraryController] === DIGITAL JOURNALS CATALOG ===");
        List<DigitalJournal> list = journalService.getAllJournals();
        if (list == null || list.isEmpty()) {
            System.out.println("No digital journals found.");
            return;
        }

        for (DigitalJournal dj : list) {
            System.out.println(String.format("ID: %d | Title: %s | Topic: %s | DOI: %s | Format: %s (%d KB)",
                    dj.getResourceId(), dj.getTitle(), dj.getTopic(), dj.getDoi(), dj.getFormat(), dj.getFileSize()));
        }
    }

    /**
     * Displays all university members.
     */
    public void displayAllMembers() {
        System.out.println("\n[LibraryController] === UNIVERSITY MEMBERS ===");
        List<UniversityMember> list = memberService.getAllMembers();
        if (list == null || list.isEmpty()) {
            System.out.println("No university members found.");
            return;
        }

        for (UniversityMember m : list) {
            String roleInfo = "STUDENT".equalsIgnoreCase(m.getMemberType())
                    ? ("Major: " + m.getMajor())
                    : ("Rank: " + m.getAcademicRank());
            System.out.println(String.format("ID: %d | Code: %s | Name: %s | Role: %s | Dept: %s | %s",
                    m.getPersonId(), m.getMemberId(), m.getName(), m.getMemberType(), m.getDepartment(), roleInfo));
        }
    }

    /**
     * Displays recent digital access logs (BR13 & FR08).
     */
    public void displayAccessLogs() {
        System.out.println("\n[LibraryController] === DIGITAL ACCESS LOGS (ACCESS_LOG) ===");
        List<AccessLog> logs = journalService.getAllAccessLogs();
        if (logs == null || logs.isEmpty()) {
            System.out.println("No access logs found.");
            return;
        }

        for (AccessLog log : logs) {
            String status = log.isDownloadSuccess() ? "SUCCESS" : "BLOCKED";
            System.out.println(String.format("Log #%d | Person: %d | Journal: %d | Time: %s | IP: %s | Status: %s",
                    log.getLogId(), log.getPersonId(), log.getJournalResourceId(),
                    log.getAccessTimestamp(), log.getIpAddress(), status));
        }
    }

    /**
     * Displays all fines in the system.
     */
    public void displayAllFines() {
        System.out.println("\n[LibraryController] === ALL FINE RECORDS ===");
        List<Fine> fines = fineService.getAllFines();
        if (fines == null || fines.isEmpty()) {
            System.out.println("No fine records found.");
            return;
        }

        for (Fine f : fines) {
            BigDecimal remaining = fineService.getRemainingBalance(f.getFineId());
            System.out.println(String.format("Fine ID: %d | Borrow ID: %d | Total: %,.0f VND | Remaining: %,.0f VND | Status: %s | Due: %s",
                    f.getFineId(), f.getBorrowId(), f.getTotalFineAmount().doubleValue(),
                    remaining.doubleValue(), f.getPaymentStatus(), f.getDueDate()));
        }
    }

    /**
     * Displays detailed payment history and remaining balance for a fine.
     */
    public void displayFineDetails(int fineId) {
        System.out.println("\n[LibraryController] === FINE DETAILS & PAYMENT HISTORY (ID: " + fineId + ") ===");
        Fine fine = fineService.getFineDetails(fineId);
        if (fine == null) {
            System.out.println("Fine ID " + fineId + " not found.");
            return;
        }

        BigDecimal remaining = fineService.getRemainingBalance(fineId);
        System.out.println(String.format("Fine ID: %d | Borrow ID: %d | Total: %,.0f VND | Remaining: %,.0f VND | Status: %s",
                fine.getFineId(), fine.getBorrowId(), fine.getTotalFineAmount().doubleValue(),
                remaining.doubleValue(), fine.getPaymentStatus()));

        List<PaymentTransaction> history = fineService.getPaymentHistory(fineId);
        if (history == null || history.isEmpty()) {
            System.out.println("    (No payment transactions recorded yet.)");
        } else {
            System.out.println("    Transactions:");
            for (PaymentTransaction tx : history) {
                System.out.println(String.format("    -> TX #%d | Paid: %,.0f VND | Date: %s | Method: %s",
                        tx.getTransactionId(), tx.getAmountPaid().doubleValue(), tx.getPaymentDate(), tx.getPaymentMethod()));
            }
        }
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

    public FineService getFineService() {
        return fineService;
    }

    public void setFineService(FineService fineService) {
        this.fineService = fineService;
    }

    public DigitalJournalService getJournalService() {
        return journalService;
    }

    public void setJournalService(DigitalJournalService journalService) {
        this.journalService = journalService;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }
}
