package com.library.service;

import com.library.model.Fine;
import com.library.model.PaymentTransaction;
import com.library.repository.FineRepository;
import com.library.repository.PaymentTransactionRepository;
import com.library.util.DatabaseConnection;
import com.library.util.SqlExceptionMapper;
import com.library.validation.DataValidator;
import com.library.validation.FineValidator;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Service class implementing business logic operations for Fine management
 * and payment confirmations by librarian (Test 03, Test 04, BR02, BR11).
 * Không có bước tính toán trả góp, thủ thư trực tiếp xác nhận đã thanh toán.
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
     * Creates and records a new fine in the system (triggered by overdue returns).
     * Bỏ due_date, mặc định trạng thái 'Unpaid'.
     */
    public Fine createFine(int borrowId, BigDecimal fineAmount) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");
        DataValidator.validatePositiveAmount(fineAmount, "Số tiền phạt (Fine Amount)");

        Fine fine = new Fine(0, borrowId, fineAmount, "Unpaid");
        fineRepository.insertFine(fine);
        return fine;
    }

    /**
     * Backward-compatible createFine ignoring daysToPay.
     */
    public Fine createFine(int borrowId, BigDecimal fineAmount, int daysToPay) {
        return createFine(borrowId, fineAmount);
    }

    /**
     * Confirms that a fine has been fully settled by delegating validation and atomic
     * transaction recording + status update to MySQL Stored Procedure sp_confirm_fine_payment.
     */
    public boolean confirmPayment(int fineId) {
        DataValidator.validatePositiveId(fineId, "Fine ID");

        String sql = "{CALL sp_confirm_fine_payment(?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, fineId);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.execute();

            int transactionId = cs.getInt(2);
            System.out.println("[FineService] SUCCESS: Fine #" + fineId +
                    " confirmed as PAID (Completed) by librarian (Transaction ID: " + transactionId + ").");
            return true;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return false;
        }
    }

    /**
     * Executes a payment for a fine by confirming payment.
     */
    public boolean payFine(int fineId) {
        return confirmPayment(fineId);
    }

    /**
     * Backward-compatible payFine ignoring partial amount and payment method.
     */
    public boolean payFine(int fineId, BigDecimal amountPaid, String paymentMethod) {
        return confirmPayment(fineId);
    }

    /**
     * Retrieves all fines with detailed member and borrow information for UI display.
     */
    public List<Map<String, Object>> getAllFinesDetailed() {
        return fineRepository.findAllFinesDetailed();
    }

    /**
     * Calculates the remaining unpaid balance of a fine (0 if Completed, full amount if Unpaid).
     */
    public BigDecimal getRemainingBalance(int fineId) {
        DataValidator.validatePositiveId(fineId, "Fine ID");
        Fine fine = fineRepository.findById(fineId);
        FineValidator.validateFineExists(fine, fineId);

        if ("Completed".equalsIgnoreCase(fine.getPaymentStatus())) {
            return BigDecimal.ZERO;
        }
        return fine.getTotalFineAmount();
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
     * Retrieves all outstanding fines ('Unpaid').
     */
    public List<Fine> getOutstandingFines() {
        return fineRepository.findOutstandingFines();
    }

    /**
     * Retrieves all payment transactions for a fine.
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
