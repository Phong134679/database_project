package com.library.model;

import java.time.LocalDate;

/**
 * Entity representing a borrow/return transaction.
 */
public class Borrow {
    private int borrowId;
    private int memberPersonId;
    private int copyId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    public Borrow() {
    }

    public Borrow(int borrowId, int memberPersonId, int copyId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate) {
        this.borrowId = borrowId;
        this.memberPersonId = memberPersonId;
        this.copyId = copyId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public int getBorrowId() {
        return borrowId;
    }

    public void setBorrowId(int borrowId) {
        this.borrowId = borrowId;
    }

    public int getMemberPersonId() {
        return memberPersonId;
    }

    public void setMemberPersonId(int memberPersonId) {
        this.memberPersonId = memberPersonId;
    }

    public int getCopyId() {
        return copyId;
    }

    public void setCopyId(int copyId) {
        this.copyId = copyId;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    @Override
    public String toString() {
        return "Borrow{" +
                "borrowId=" + borrowId +
                ", memberPersonId=" + memberPersonId +
                ", copyId=" + copyId +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                '}';
    }
}
