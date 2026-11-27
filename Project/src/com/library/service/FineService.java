package com.library.service;

import com.library.exception.FineAlreadySettledException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Fine;
import com.library.model.PaymentTransaction;
import com.library.repository.FineRepository;
import com.library.repository.PaymentTransactionRepository;
import com.library.util.TransactionManager;
import com.library.validation.DataValidator;
import com.library.validation.FineValidator;

import java.util.List;
import java.util.Map;

/**
 * Service class coordinating Fine queries and payment confirmation via JDBC transactions.
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
     * Confirms that a fine has been fully settled within an atomic JDBC transaction:
     * locks the FINE row (FOR UPDATE), validates status != Completed, inserts PAYMENT_TRANSACTION,
     * and updates FINE.payment_status to 'Completed'.
     */
    public boolean confirmPayment(int fineId) {
        DataValidator.validatePositiveId(fineId, "Fine ID");

        return TransactionManager.executeInTransaction(conn -> {
            Fine fine = fineRepository.findByIdForUpdate(conn, fineId);
            if (fine == null) {
                throw new ResourceNotFoundException("Không tìm thấy phiếu phạt với ID đã cho.");
            }
            if ("Completed".equalsIgnoreCase(fine.getPaymentStatus())) {
                throw new FineAlreadySettledException("Phiếu phạt này đã được thanh toán hoàn tất.");
            }

            int transactionId = paymentTransactionRepository.insertTransaction(conn, fineId);
            fineRepository.updatePaymentStatus(conn, fineId, "Completed");

            System.out.println("[FineService] SUCCESS: Fine #" + fineId +
                    " confirmed as PAID (Completed) by librarian (Transaction ID: " + transactionId + ").");
            return true;
        });
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
