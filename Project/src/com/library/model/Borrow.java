package com.library.model;

import java.time.LocalDate;

/**
 * Entity representing a borrow/return transaction.
 * memberId references UNIVERSITY_MEMBER.member_id (VARCHAR).
 * copyId references BOOK_COPY.copy_id (VARCHAR, e.g. '1_1').
 */
public class Borrow {
    private int borrowId;
    private String memberId;
    private String copyId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    public Borrow() {
    }

    public Borrow(int borrowId, String memberId, String copyId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate) {
        this.borrowId = borrowId;
        this.memberId = memberId;
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

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getCopyId() {
        return copyId;
    }

    public void setCopyId(String copyId) {
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
                ", memberId='" + memberId + '\'' +
                ", copyId='" + copyId + '\'' +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                '}';
    }
}
