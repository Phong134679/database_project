package com.library.service;

import com.library.model.Fine;
import com.library.model.PaymentTransaction;
import com.library.repository.FineRepository;
import com.library.repository.PaymentTransactionRepository;
import com.library.validation.DataValidator;
import com.library.validation.FineValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class implementing business logic operations for Fine management
 * and installment payment transactions (Test 03, Test 04, BR02, BR11).
 */
public class FineService {

    private FineRepository fineRepository;
    private PaymentTransactionRepository paymentTransactionRepository;

    public FineService() {
        this.fineRepository = new FineRepository();
        this.paymentTransactionRepository = new PaymentTransactionRepository();
    }

    public FineService(FineRepository fineRepository, PaymentTransactionRepository paymentTransactionRepository) {
        this.fineRepository = fineRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Creates and records a new fine in the system (e.g. triggered by overdue returns or damage).
     */
    public Fine createFine(int borrowId, BigDecimal fineAmount, int daysToPay) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");
        DataValidator.validatePositiveAmount(fineAmount, "Số tiền phạt (Fine Amount)");

        LocalDate dueDate = LocalDate.now().plusDays(daysToPay > 0 ? daysToPay : 14);
        Fine fine = new Fine(0, borrowId, fineAmount, "Unpaid", dueDate);
        fineRepository.insertFine(fine);
        return fine;
    }

    /**
     * Executes a payment for an outstanding fine (Validation Case Test 04 & BR11).
     * Supports installment payments and automatically transitions status
     * from 'Unpaid' -> 'Partially Paid' -> 'Completed'.
     * Throws specific BusinessException/ValidationException on failure, returns true on success.
     */
    public boolean payFine(int fineId, BigDecimal amountPaid, String paymentMethod) {
        DataValidator.validatePositiveId(fineId, "Fine ID");
        DataValidator.validatePositiveAmount(amountPaid, "Số tiền thanh toán");

        Fine fine = fineRepository.findById(fineId);
        FineValidator.validatePaymentEligibility(fine, amountPaid, fineId);

        BigDecimal currentPaid = paymentTransactionRepository.getTotalPaidForFine(fineId);
        BigDecimal remaining = fine.getTotalFineAmount().subtract(currentPaid);

        if (amountPaid.compareTo(remaining) > 0) {
            System.out.println("[FineService] Notice: Payment amount (" + amountPaid +
                    " VND) exceeds remaining balance (" + remaining + " VND). Adjusting to exact balance.");
            amountPaid = remaining;
        }

        // 1. Ghi nhận giao dịch thanh toán
        PaymentTransaction tx = new PaymentTransaction(
                0,
                fineId,
                amountPaid,
                LocalDateTime.now(),
                paymentMethod != null && !paymentMethod.trim().isEmpty() ? paymentMethod : "Cash"
        );
        paymentTransactionRepository.insertTransaction(tx);

        if (tx.getTransactionId() <= 0) {
            System.err.println("[FineService] Failed to record payment transaction.");
            return false;
        }

        // 2. Tính lại tổng đã trả sau giao dịch này
        BigDecimal newTotalPaid = currentPaid.add(amountPaid);

        // 3. Cập nhật trạng thái phiếu phạt
        String newStatus;
        if (newTotalPaid.compareTo(fine.getTotalFineAmount()) >= 0) {
            newStatus = "Completed";
        } else {
            newStatus = "Partially Paid";
        }

        fine.setPaymentStatus(newStatus);
        fineRepository.updatePaymentStatus(fineId, newStatus);

        System.out.println("[FineService] SUCCESS: Payment of " + String.format("%,.0f", amountPaid.doubleValue()) +
                " VND recorded (Transaction ID: " + tx.getTransactionId() + "). New status: " + newStatus);
        return true;
    }

    /**
     * Calculates the remaining unpaid balance of a fine.
     */
    public BigDecimal getRemainingBalance(int fineId) {
        DataValidator.validatePositiveId(fineId, "Fine ID");
        Fine fine = fineRepository.findById(fineId);
        FineValidator.validateFineExists(fine, fineId);

        BigDecimal totalPaid = paymentTransactionRepository.getTotalPaidForFine(fineId);
        BigDecimal remaining = fine.getTotalFineAmount().subtract(totalPaid);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    /**
     * Retrieves fine details by ID.
     */
    public Fine getFineDetails(int fineId) {
        DataValidator.validatePositiveId(fineId, "Fine ID");
        Fine fine = fineRepository.findById(fineId);
        FineValidator.validateFineExists(fine, fineId);
        return fine;
    }

    /**
     * Retrieves fine details associated with a borrowId.
     */
    public Fine getFineByBorrowId(int borrowId) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");
        return fineRepository.findByBorrowId(borrowId);
    }

    /**
     * Retrieves all fines.
     */
    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }

    /**
     * Retrieves all outstanding fines ('Unpaid' or 'Partially Paid').
     */
    public List<Fine> getOutstandingFines() {
        return fineRepository.findOutstandingFines();
    }

    /**
     * Retrieves all installment payment transactions for a fine.
     */
    public List<PaymentTransaction> getPaymentHistory(int fineId) {
        return paymentTransactionRepository.findByFineId(fineId);
    }

    public FineRepository getFineRepository() {
        return fineRepository;
    }

    public void setFineRepository(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    public PaymentTransactionRepository getPaymentTransactionRepository() {
        return paymentTransactionRepository;
    }

    public void setPaymentTransactionRepository(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }
}
