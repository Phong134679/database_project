package com.library.service;

import com.library.model.Borrow;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.util.DatabaseConnection;
import com.library.util.SqlExceptionMapper;
import com.library.validation.BorrowValidator;
import com.library.validation.DataValidator;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class coordinating borrow/return operations.
 * All business rules (eligibility, quota, overdue, fine, copy status) are enforced
 * by MySQL Stored Procedures sp_borrow_book and sp_return_book.
 */
public class BorrowService {

    private BorrowRepository borrowRepository;
    private FineRepository fineRepository;
    private MemberService memberService;

    public BorrowService() {
        this.borrowRepository = new BorrowRepository();
        this.fineRepository = new FineRepository();
        this.memberService = new MemberService();
    }

    public BorrowService(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = new FineRepository();
        this.memberService = new MemberService();
    }

    public BorrowService(BorrowRepository borrowRepository, FineRepository fineRepository) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = fineRepository;
        this.memberService = new MemberService();
    }

    public BorrowService(BorrowRepository borrowRepository, FineRepository fineRepository, MemberService memberService) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = fineRepository;
        this.memberService = memberService;
    }

    /**
     * Processes a new book borrowing transaction by calling sp_borrow_book(member_id, copy_id, OUT borrow_id, OUT due_date).
     */
    public Map<String, Object> borrowBook(String memberId, String copyId) {
        DataValidator.validateNonEmptyString(memberId, "Member ID");
        DataValidator.validateNonEmptyString(copyId, "Copy ID");

        String sql = "{CALL sp_borrow_book(?, ?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, memberId.trim());
            cs.setString(2, copyId.trim());
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.DATE);
            cs.execute();

            int borrowId = cs.getInt(3);
            Date dueDate = cs.getDate(4);
            System.out.println("[BorrowService] SUCCESS: Borrow ID " + borrowId +
                    " created for Member " + memberId.trim() + ", Copy " + copyId.trim() +
                    " (Due Date: " + dueDate + ")");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("borrowId", borrowId);
            result.put("memberId", memberId.trim());
            result.put("copyId", copyId.trim());
            result.put("dueDate", dueDate != null ? dueDate.toString() : null);
            return result;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return null;
        }
    }

    public boolean processNewBorrow(String memberId, String copyId) {
        borrowBook(memberId, copyId);
        return true;
    }

    public boolean returnBook(int borrowId) {
        return returnBook(borrowId, null);
    }

    /**
     * Processes book return by calling sp_return_book(borrow_id, return_date, OUT fine_id, OUT fine_amount).
     */
    public Map<String, Object> returnBookWithResult(int borrowId, LocalDate returnDate) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");

        String sql = "{CALL sp_return_book(?, ?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, borrowId);
            if (returnDate != null) {
                cs.setDate(2, Date.valueOf(returnDate));
            } else {
                cs.setNull(2, Types.DATE);
            }
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.DECIMAL);
            cs.execute();

            int fineId = cs.getInt(3);
            BigDecimal fineAmount = cs.getBigDecimal(4);
            if (fineId > 0) {
                System.out.println("[BorrowService] OVERDUE DETECTED. Fine ID: " + fineId +
                        " (Amount: " + String.format("%,.0f", fineAmount != null ? fineAmount.doubleValue() : 0.0) + " VND)");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("borrowId", borrowId);
            result.put("fineId", fineId);
            result.put("fineAmount", fineAmount);
            return result;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return null;
        }
    }

    public boolean returnBook(int borrowId, LocalDate returnDate) {
        returnBookWithResult(borrowId, returnDate);
        return true;
    }

    public List<Map<String, Object>> getAllBorrowsDetailed() {
        return borrowRepository.findAllBorrowsDetailed();
    }

    public List<Map<String, Object>> getAllCopiesDetailed() {
        return borrowRepository.findAllCopiesDetailed();
    }

    public List<Borrow> getOverdueBorrows() {
        return borrowRepository.findOverdueBorrows();
    }

    public Borrow getBorrowDetails(int borrowId) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");
        Borrow borrow = borrowRepository.findById(borrowId);
        BorrowValidator.validateBorrowExists(borrow, borrowId);
        return borrow;
    }

    public BorrowRepository getBorrowRepository() {
        return borrowRepository;
    }

    public void setBorrowRepository(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }

    public FineRepository getFineRepository() {
        return fineRepository;
    }

    public void setFineRepository(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }
}
