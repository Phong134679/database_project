package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing a fine penalty associated with a borrow transaction.
 */
public class Fine {
    private int fineId;
    private int borrowId;
    private BigDecimal totalFineAmount;
    private String paymentStatus;
    private LocalDate dueDate;

    public Fine() {
    }

    public Fine(int fineId, int borrowId, BigDecimal totalFineAmount, String paymentStatus, LocalDate dueDate) {
        this.fineId = fineId;
        this.borrowId = borrowId;
        this.totalFineAmount = totalFineAmount;
        this.paymentStatus = paymentStatus;
        this.dueDate = dueDate;
    }

    public int getFineId() {
        return fineId;
    }

    public void setFineId(int fineId) {
        this.fineId = fineId;
    }

    public int getBorrowId() {
        return borrowId;
    }

    public void setBorrowId(int borrowId) {
        this.borrowId = borrowId;
    }

    public BigDecimal getTotalFineAmount() {
        return totalFineAmount;
    }

    public void setTotalFineAmount(BigDecimal totalFineAmount) {
        this.totalFineAmount = totalFineAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public String toString() {
        return "Fine{" +
                "fineId=" + fineId +
                ", borrowId=" + borrowId +
                ", totalFineAmount=" + totalFineAmount +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", dueDate=" + dueDate +
                '}';
    }
}
