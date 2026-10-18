package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an installment payment transaction for a fine.
 */
public class PaymentTransaction {
    private int transactionId;
    private int fineId;
    private BigDecimal amountPaid;
    private LocalDateTime paymentDate;
    private String paymentMethod;

    public PaymentTransaction() {
    }

    public PaymentTransaction(int transactionId, int fineId, BigDecimal amountPaid,
                              LocalDateTime paymentDate, String paymentMethod) {
        this.transactionId = transactionId;
        this.fineId = fineId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
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

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "transactionId=" + transactionId +
                ", fineId=" + fineId +
                ", amountPaid=" + amountPaid +
                ", paymentDate=" + paymentDate +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}
