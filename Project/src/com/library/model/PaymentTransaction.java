package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a payment confirmation transaction for a fine.
 * Thuộc tính 'amount_paid' và 'payment_method' đã được bãi bỏ.
 */
public class PaymentTransaction {
    private int transactionId;
    private int fineId;
    private LocalDateTime paymentDate;

    public PaymentTransaction() {
    }

    public PaymentTransaction(int transactionId, int fineId, LocalDateTime paymentDate) {
        this.transactionId = transactionId;
        this.fineId = fineId;
        this.paymentDate = paymentDate;
    }

    /**
     * Backward-compatible constructor ignoring amountPaid and paymentMethod.
     */
    public PaymentTransaction(int transactionId, int fineId, BigDecimal amountPaid,
                              LocalDateTime paymentDate, String paymentMethod) {
        this(transactionId, fineId, paymentDate);
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getFineId() {
        return fineId;
    }

    public void setFineId(int fineId) {
        this.fineId = fineId;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "transactionId=" + transactionId +
                ", fineId=" + fineId +
                ", paymentDate=" + paymentDate +
                '}';
    }
}
