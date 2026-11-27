package com.library.service;

import com.library.exception.BookAlreadyReturnedException;
import com.library.exception.BookNotAvailableException;
import com.library.exception.BorrowQuotaExceededException;
import com.library.exception.IneligibleMemberException;
import com.library.exception.InvalidDateRangeException;
import com.library.exception.LicenseExpiredException;
import com.library.exception.OutstandingFineException;
import com.library.exception.OverdueBooksPendingException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Borrow;
import com.library.model.Fine;
import com.library.model.UniversityMember;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.repository.GrantedLicenseRepository;
import com.library.repository.ResourceRepository;
import com.library.repository.UniversityMemberRepository;
import com.library.util.TransactionManager;
import com.library.validation.BorrowValidator;
import com.library.validation.DataValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class coordinating borrow/return business rules and JDBC transactions.
 */
public class BorrowService {

    public static final BigDecimal LATE_RETURN_FINE_AMOUNT = new BigDecimal("50000.00");

    private BorrowRepository borrowRepository;
    private FineRepository fineRepository;
    private UniversityMemberRepository memberRepository;
    private ResourceRepository resourceRepository;
    private GrantedLicenseRepository grantedLicenseRepository;
    private MemberService memberService;
    private ResourceService resourceService;

    public BorrowService() {
        this.borrowRepository = new BorrowRepository();
        this.fineRepository = new FineRepository();
        this.memberRepository = new UniversityMemberRepository();
        this.resourceRepository = new ResourceRepository();
        this.grantedLicenseRepository = new GrantedLicenseRepository();
        this.memberService = new MemberService();
        this.resourceService = new ResourceService(this.resourceRepository, this.grantedLicenseRepository);
    }

    public BorrowService(BorrowRepository borrowRepository) {
        this(borrowRepository, new FineRepository(), new MemberService());
    }

    public BorrowService(BorrowRepository borrowRepository, FineRepository fineRepository) {
        this(borrowRepository, fineRepository, new MemberService());
    }

    public BorrowService(BorrowRepository borrowRepository, FineRepository fineRepository, MemberService memberService) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = fineRepository;
        this.memberService = memberService;
        this.memberRepository = memberService != null && memberService.getMemberRepository() != null
                ? memberService.getMemberRepository()
                : new UniversityMemberRepository();
        this.resourceRepository = new ResourceRepository();
        this.grantedLicenseRepository = new GrantedLicenseRepository();
        this.resourceService = new ResourceService(this.resourceRepository, this.grantedLicenseRepository);
    }

    public BorrowService(BorrowRepository borrowRepository,
                         FineRepository fineRepository,
                         UniversityMemberRepository memberRepository,
                         ResourceRepository resourceRepository,
                         GrantedLicenseRepository grantedLicenseRepository,
                         MemberService memberService) {
        this(borrowRepository, fineRepository, memberRepository, resourceRepository,
                grantedLicenseRepository, memberService, new ResourceService(resourceRepository, grantedLicenseRepository));
    }

    public BorrowService(BorrowRepository borrowRepository,
                         FineRepository fineRepository,
                         UniversityMemberRepository memberRepository,
                         ResourceRepository resourceRepository,
                         GrantedLicenseRepository grantedLicenseRepository,
                         MemberService memberService,
                         ResourceService resourceService) {
        this.borrowRepository = borrowRepository;
        this.fineRepository = fineRepository;
        this.memberRepository = memberRepository;
        this.resourceRepository = resourceRepository;
        this.grantedLicenseRepository = grantedLicenseRepository;
        this.memberService = memberService;
        this.resourceService = resourceService;
    }

    /**
     * Processes a new book borrowing transaction in Java + JDBC:
     * locks member & copy rows (FOR UPDATE), checks eligibility, overdue, unpaid fines,
     * quota, availability, and license requirements, then inserts BORROW and updates BOOK_COPY.
     */
    public Map<String, Object> borrowBook(String memberId, String copyId) {
        DataValidator.validateNonEmptyString(memberId, "Member ID");
        DataValidator.validateNonEmptyString(copyId, "Copy ID");

        String cleanMemberId = memberId.trim();
        String cleanCopyId = copyId.trim();

        return TransactionManager.executeInTransaction(conn -> {
            UniversityMember member = memberRepository.findByMemberIdForUpdate(conn, cleanMemberId);
            if (member == null) {
                throw new IneligibleMemberException("Chỉ UNIVERSITY_MEMBER mới được mượn sách.");
            }

            int quota = memberService.getBorrowQuota(member.getMemberType());
            int loanDays = memberService.getLoanDurationDays(member.getMemberType());

            if (borrowRepository.countOverdueByMemberId(conn, cleanMemberId) > 0) {
                throw new OverdueBooksPendingException("Thành viên đang có sách mượn quá hạn chưa trả.");
            }
            if (borrowRepository.countUnpaidFinesByMemberId(conn, cleanMemberId) > 0) {
                throw new OutstandingFineException("Thành viên có phiếu phạt chưa thanh toán.");
            }
            if (borrowRepository.countActiveByMemberId(conn, cleanMemberId) >= quota) {
                throw new BorrowQuotaExceededException("Thành viên đã đạt giới hạn số lượng sách mượn.");
            }

            Map<String, Object> copyInfo = borrowRepository.findPhysicalCopyForUpdate(conn, cleanCopyId);
            if (copyInfo == null) {
                throw new BookNotAvailableException("Bản sao sách không tồn tại hoặc không phải sách vật lý.");
            }
            String copyStatus = (String) copyInfo.get("availabilityStatus");
            if (!"Available".equals(copyStatus)) {
                throw new BookNotAvailableException("Bản sao sách hiện không sẵn sàng để mượn.");
            }
            if (borrowRepository.countActiveByCopyId(conn, cleanCopyId) > 0) {
                throw new BookNotAvailableException("Bản sao đang có giao dịch mượn chưa trả.");
            }

            int resourceId = ((Number) copyInfo.get("resourceId")).intValue();
            boolean isPublic = Boolean.TRUE.equals(copyInfo.get("isPublic"));
            boolean isAuthor = resourceRepository.isAuthorOfResource(conn, member.getPersonId(), resourceId);

            if (!isPublic && !isAuthor) {
                if (!resourceService.hasRequiredLicensesConfigured(conn, resourceId)) {
                    throw new LicenseExpiredException("Sách private chưa khai báo License yêu cầu.");
                }
                if (!resourceService.hasAllRequiredLicenses(conn, member.getPersonId(), resourceId)) {
                    throw new LicenseExpiredException("Thành viên chưa sở hữu đầy đủ License bắt buộc cho sách.");
                }
            }

            LocalDate borrowDate = LocalDate.now();
            LocalDate dueDate = borrowDate.plusDays(loanDays);

            int borrowId = borrowRepository.insertBorrow(conn, cleanMemberId, cleanCopyId, borrowDate, dueDate);
            borrowRepository.updateCopyAvailability(conn, cleanCopyId, "Borrowed");

            System.out.println("[BorrowService] SUCCESS: Borrow ID " + borrowId +
                    " created for Member " + cleanMemberId + ", Copy " + cleanCopyId +
                    " (Due Date: " + dueDate + ")");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("borrowId", borrowId);
            result.put("memberId", cleanMemberId);
            result.put("copyId", cleanCopyId);
            result.put("dueDate", dueDate.toString());
            return result;
        });
    }

    public boolean processNewBorrow(String memberId, String copyId) {
        borrowBook(memberId, copyId);
        return true;
    }

    public boolean returnBook(int borrowId) {
        return returnBook(borrowId, null);
    }

    /**
     * Processes book return in Java + JDBC:
     * locks BORROW & BOOK_COPY rows (FOR UPDATE), validates return date,
     * updates BORROW.return_date & BOOK_COPY.availability_status, and generates/accumulates late fine.
     */
    public Map<String, Object> returnBookWithResult(int borrowId, LocalDate returnDate) {
        DataValidator.validatePositiveId(borrowId, "Borrow ID");

        return TransactionManager.executeInTransaction(conn -> {
            Borrow borrow = borrowRepository.findByIdForUpdate(conn, borrowId);
            if (borrow == null) {
                throw new ResourceNotFoundException("Không tìm thấy phiếu mượn với ID đã cho.");
            }
            if (borrow.getReturnDate() != null) {
                throw new BookAlreadyReturnedException("Sách thuộc lượt mượn này đã được trả trước đó.");
            }

            LocalDate actualReturnDate = (returnDate != null) ? returnDate : LocalDate.now();
            if (actualReturnDate.isBefore(borrow.getBorrowDate())) {
                throw new InvalidDateRangeException("Ngày trả không thể trước ngày mượn.");
            }

            if (!borrowRepository.lockCopyForUpdate(conn, borrow.getCopyId())) {
                throw new ResourceNotFoundException("Không tìm thấy bản sao sách của giao dịch mượn.");
            }

            borrowRepository.updateReturnDate(conn, borrowId, actualReturnDate);
            borrowRepository.updateCopyAvailability(conn, borrow.getCopyId(), "Available");

            int fineId = 0;
            BigDecimal fineAmount = BigDecimal.ZERO;

            if (actualReturnDate.isAfter(borrow.getDueDate())) {
                Fine existingUnpaid = fineRepository.findLatestUnpaidByBorrowIdForUpdate(conn, borrowId);
                if (existingUnpaid != null) {
                    fineId = existingUnpaid.getFineId();
                    fineAmount = existingUnpaid.getTotalFineAmount().add(LATE_RETURN_FINE_AMOUNT);
                    fineRepository.updateTotalFineAmount(conn, fineId, fineAmount);
                } else {
                    fineAmount = LATE_RETURN_FINE_AMOUNT;
                    fineId = fineRepository.insertFine(conn, borrowId, fineAmount, "Unpaid");
                }
                System.out.println("[BorrowService] OVERDUE DETECTED. Fine ID: " + fineId +
                        " (Amount: " + String.format("%,.0f", fineAmount.doubleValue()) + " VND)");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("borrowId", borrowId);
            result.put("fineId", fineId);
            result.put("fineAmount", fineAmount);
            return result;
        });
    }

    public boolean returnBook(int borrowId, LocalDate returnDate) {
        returnBookWithResult(borrowId, returnDate);
        return true;
    }

    /**
     * Retrieves all borrows and computes isOverdue and status in Java.
     */
    public List<Map<String, Object>> getAllBorrowsDetailed() {
        List<Map<String, Object>> rawList = borrowRepository.findAllBorrowsDetailedRaw();
        List<Map<String, Object>> result = new ArrayList<>(rawList.size());
        LocalDate today = LocalDate.now();

        for (Map<String, Object> raw : rawList) {
            String dueDateStr = (String) raw.get("dueDate");
            String returnDateStr = (String) raw.get("returnDate");
            LocalDate dueDate = dueDateStr != null ? LocalDate.parse(dueDateStr) : null;

            boolean isReturned = returnDateStr != null;
            boolean isOverdue = !isReturned && dueDate != null && dueDate.isBefore(today);
            String status = isReturned ? "Returned" : (isOverdue ? "Overdue" : "Active");

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("borrowId", raw.get("borrowId"));
            map.put("memberId", raw.get("memberId"));
            map.put("personId", raw.get("personId"));
            map.put("memberName", raw.get("memberName"));
            map.put("memberType", raw.get("memberType"));
            map.put("copyId", raw.get("copyId"));
            map.put("bookTitle", raw.get("bookTitle"));
            map.put("borrowDate", raw.get("borrowDate"));
            map.put("dueDate", dueDateStr);
            map.put("returnDate", returnDateStr);
            map.put("isOverdue", isOverdue);
            map.put("status", status);
            map.put("fineId", raw.get("fineId"));
            map.put("fineAmount", raw.get("fineAmount"));
            map.put("fineStatus", raw.get("fineStatus"));
            result.add(map);
        }
        return result;
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
