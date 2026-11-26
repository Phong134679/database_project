package com.library.service;

import com.library.model.Fine;
import com.library.model.PaymentTransaction;
import com.library.repository.FineRepository;
import com.library.repository.PaymentTransactionRepository;
import com.library.util.DatabaseConnection;
import com.library.util.SqlExceptionMapper;
import com.library.validation.DataValidator;
import com.library.validation.FineValidator;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Service class coordinating Fine queries and payment confirmation.
 * Fine creation is handled by sp_return_book; payment confirmation is handled
 * by sp_confirm_fine_payment.
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

    public List<Map<String, Object>> getAllFinesDetailed() {
        return fineRepository.findAllFinesDetailed();
    }

    public Fine getFineDetails(int fineId) {
        DataValidator.validatePositiveId(fineId, "Fine ID");
        Fine fine = fineRepository.findById(fineId);
        FineValidator.validateFineExists(fine, fineId);
        return fine;
    }

    public Fine getFineByBorrowId(int borrowId) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");
        return fineRepository.findByBorrowId(borrowId);
    }

    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }

    public List<Fine> getOutstandingFines() {
        return fineRepository.findOutstandingFines();
    }

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
