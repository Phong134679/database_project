package com.library.test;

import com.library.exception.*;
import com.library.model.Borrow;
import com.library.model.Fine;
import com.library.model.Person;
import com.library.validation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Unit and integration test suite verifying all Validation and Exception rules.
 */
public class ValidationAndExceptionTest {

    private static int totalPassed = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("  KIỂM THỬ TỔNG HỢP: MODUN VALIDATION & EXCEPTION (UNIT TESTS)               ");
        System.out.println("===============================================================================");

        testDataValidator();
        testMemberValidator();
        testBorrowValidator();
        testFineValidator();
        testLicenseValidator();

        System.out.println("\n===============================================================================");
        System.out.println("  KẾT QUẢ: " + totalPassed + "/" + totalTests + " TEST CASES PASSED HOÀN HẢO!  ");
        System.out.println("===============================================================================");
    }

    private static void testDataValidator() {
        System.out.println("\n--- [1] Kiểm thử DataValidator & ValidationException ---");

        // 1.1 ID âm hoặc bằng 0
        runTest("DataValidator: Chặn ID <= 0", () -> {
            try {
                DataValidator.validatePositiveId(0, "Member ID");
                return false;
            } catch (InvalidInputException e) {
                System.out.println("    -> Bắt đúng InvalidInputException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "INVALID_INPUT".equals(e.getErrorCode());
            }
        });

        // 1.2 Chuỗi rỗng hoặc null
        runTest("DataValidator: Chặn chuỗi rỗng / null", () -> {
            try {
                DataValidator.validateNonEmptyString("   ", "User Name");
                return false;
            } catch (InvalidInputException e) {
                System.out.println("    -> Bắt đúng InvalidInputException: [" + e.getErrorCode() + "] " + e.getMessage());
                return true;
            }
        });

        // 1.3 Số tiền âm hoặc bằng 0
        runTest("DataValidator: Chặn số tiền <= 0", () -> {
            try {
                DataValidator.validatePositiveAmount(BigDecimal.valueOf(-5000), "Số tiền phạt");
                return false;
            } catch (InvalidAmountException e) {
                System.out.println("    -> Bắt đúng InvalidAmountException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "INVALID_AMOUNT".equals(e.getErrorCode());
            }
        });

        // 1.4 Dải ngày không hợp lệ (fromDate > toDate)
        runTest("DataValidator: Chặn dải ngày đảo ngược", () -> {
            try {
                DataValidator.validateDateRange(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), "Thời hạn");
                return false;
            } catch (InvalidDateRangeException e) {
                System.out.println("    -> Bắt đúng InvalidDateRangeException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "INVALID_DATE_RANGE".equals(e.getErrorCode());
            }
        });
    }

    private static void testMemberValidator() {
        System.out.println("\n--- [2] Kiểm thử MemberValidator & BusinessException (BR01, BR05) ---");

        Person guest = new Person(4, "SSN004", "Lê Văn Cường", "Hà Nội", "cuong@mail.com", "0904", "GUEST");
        Person student = new Person(1, "SSN001", "Nguyễn Văn An", "Hà Nội", "an@mail.com", "0901", "STUDENT");

        // 2.1 Person không tồn tại
        runTest("MemberValidator: Chặn khi Person không tồn tại", () -> {
            try {
                MemberValidator.validatePersonExists(null, 999);
                return false;
            } catch (ResourceNotFoundException e) {
                System.out.println("    -> Bắt đúng ResourceNotFoundException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "NOT_FOUND".equals(e.getErrorCode());
            }
        });

        // 2.2 BR01: Khách ngoài mượn sách về nhà
        runTest("MemberValidator (BR01): Chặn đối tượng ngoài (GUEST) mượn sách", () -> {
            try {
                MemberValidator.validateBorrowEligibility(guest, false, false, 0);
                return false;
            } catch (IneligibleMemberException e) {
                System.out.println("    -> Bắt đúng IneligibleMemberException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR01_INELIGIBLE_MEMBER".equals(e.getErrorCode());
            }
        });

        // 2.3 BR05: Chặn khi đang giữ sách quá hạn
        runTest("MemberValidator (BR05): Chặn mượn khi đang có sách quá hạn", () -> {
            try {
                MemberValidator.validateBorrowEligibility(student, true, false, 0);
                return false;
            } catch (OverdueBooksPendingException e) {
                System.out.println("    -> Bắt đúng OverdueBooksPendingException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR05_OVERDUE_PENDING".equals(e.getErrorCode());
            }
        });

        // 2.4 BR05: Chặn khi đang nợ phạt
        runTest("MemberValidator (BR05): Chặn mượn khi đang nợ tiền phạt", () -> {
            try {
                MemberValidator.validateBorrowEligibility(student, false, true, 0);
                return false;
            } catch (OutstandingFineException e) {
                System.out.println("    -> Bắt đúng OutstandingFineException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR05_OUTSTANDING_FINE".equals(e.getErrorCode());
            }
        });

        // 2.5 BR05: Chặn khi vượt quá quota (Student mượn 3/3 cuốn)
        runTest("MemberValidator (BR05): Chặn mượn khi đạt hạn mức quota", () -> {
            try {
                MemberValidator.validateBorrowEligibility(student, false, false, 3);
                return false;
            } catch (BorrowQuotaExceededException e) {
                System.out.println("    -> Bắt đúng BorrowQuotaExceededException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR05_QUOTA_EXCEEDED".equals(e.getErrorCode());
            }
        });
    }

    private static void testBorrowValidator() {
        System.out.println("\n--- [3] Kiểm thử BorrowValidator (BR10, BR02) ---");

        // 3.1 BR10: Bản sao sách không sẵn sàng
        runTest("BorrowValidator (BR10): Chặn mượn bản sao không Available", () -> {
            try {
                BorrowValidator.validateCopyAvailable(false, 101);
                return false;
            } catch (BookNotAvailableException e) {
                System.out.println("    -> Bắt đúng BookNotAvailableException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR10_BOOK_NOT_AVAILABLE".equals(e.getErrorCode());
            }
        });

        // 3.2 BR02: Chặn trả sách trùng lặp
        Borrow alreadyReturned = new Borrow(1, 1, 2, LocalDate.now().minusDays(10), LocalDate.now().plusDays(4), LocalDate.now().minusDays(2));
        runTest("BorrowValidator (BR02): Chặn trả phiếu mượn đã hoàn tất", () -> {
            try {
                BorrowValidator.validateReturnEligibility(alreadyReturned, 1);
                return false;
            } catch (BookAlreadyReturnedException e) {
                System.out.println("    -> Bắt đúng BookAlreadyReturnedException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR02_ALREADY_RETURNED".equals(e.getErrorCode());
            }
        });
    }

    private static void testFineValidator() {
        System.out.println("\n--- [4] Kiểm thử FineValidator (BR11) ---");

        Fine settledFine = new Fine(1, 1, BigDecimal.valueOf(50000), "Completed", LocalDate.now().plusDays(10));

        // 4.1 BR11: Chặn nộp tiền cho phiếu phạt đã tất toán
        runTest("FineValidator (BR11): Chặn thanh toán cho phiếu phạt Completed", () -> {
            try {
                FineValidator.validatePaymentEligibility(settledFine, BigDecimal.valueOf(20000), 1);
                return false;
            } catch (FineAlreadySettledException e) {
                System.out.println("    -> Bắt đúng FineAlreadySettledException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR11_FINE_ALREADY_SETTLED".equals(e.getErrorCode());
            }
        });
    }

    private static void testLicenseValidator() {
        System.out.println("\n--- [5] Kiểm thử LicenseValidator (BR04, BR12) ---");

        // 5.1 BR04: Chặn truy cập khi bản quyền hết hạn
        runTest("LicenseValidator (BR04): Chặn truy cập khi gói bản quyền hết hạn", () -> {
            try {
                LicenseValidator.validateActiveLicense(false, "Lê Văn Cường", "AI Journal 2026", LocalDate.now());
                return false;
            } catch (LicenseExpiredException e) {
                System.out.println("    -> Bắt đúng LicenseExpiredException: [" + e.getErrorCode() + "] " + e.getMessage());
                return "BR04_LICENSE_EXPIRED".equals(e.getErrorCode());
            }
        });
    }

    private interface TestCase {
        boolean run();
    }

    private static void runTest(String name, TestCase testCase) {
        totalTests++;
        System.out.print("[TEST " + totalTests + "] " + name + ": ");
        try {
            boolean passed = testCase.run();
            if (passed) {
                System.out.println("  => [PASS]\n");
                totalPassed++;
            } else {
                System.out.println("  => [FAIL: Điều kiện kiểm thử không thỏa mãn]\n");
            }
        } catch (Exception e) {
            System.out.println("  => [FAIL: Ngoại lệ không mong muốn: " + e.getMessage() + "]\n");
        }
    }
}
