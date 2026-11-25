package com.library.test;

import com.library.exception.BookAlreadyReturnedException;
import com.library.exception.BookNotAvailableException;
import com.library.exception.BorrowQuotaExceededException;
import com.library.exception.DatabaseException;
import com.library.exception.FineAlreadySettledException;
import com.library.exception.IneligibleMemberException;
import com.library.exception.InvalidAmountException;
import com.library.exception.InvalidDateRangeException;
import com.library.exception.InvalidInputException;
import com.library.exception.LibraryException;
import com.library.exception.LicenseExpiredException;
import com.library.exception.OutstandingFineException;
import com.library.exception.OverdueBooksPendingException;
import com.library.exception.ResourceNotFoundException;
import com.library.util.DatabaseConnection;
import com.library.util.SqlExceptionMapper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Tests SqlExceptionMapper against every error prefix, fallback behavior,
 * and live MySQL Connector/J SQLException format from SIGNAL SQLSTATE '45000'.
 */
public class SqlExceptionMapperTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== SqlExceptionMapperTest (TASK 06-A Gate) ===");

        // 1. Unit tests for all error prefixes (SQLSTATE 45000, vendor code 1644)
        assertMapped("NOT_FOUND", "[NOT_FOUND] Không tìm thấy entity.", "45000", 1644, ResourceNotFoundException.class, "Không tìm thấy entity.");
        assertMapped("BR01_INELIGIBLE_MEMBER", "[BR01_INELIGIBLE_MEMBER] Chỉ STUDENT/FACULTY.", "45000", 1644, IneligibleMemberException.class, "Chỉ STUDENT/FACULTY.");
        assertMapped("BR02_ALREADY_RETURNED", "[BR02_ALREADY_RETURNED] Đã trả rồi.", "45000", 1644, BookAlreadyReturnedException.class, "Đã trả rồi.");
        assertMapped("BR04_LICENSE_EXPIRED", "[BR04_LICENSE_EXPIRED] Thiếu license.", "45000", 1644, LicenseExpiredException.class, "Thiếu license.");
        assertMapped("BR05_OVERDUE_PENDING", "[BR05_OVERDUE_PENDING] Có sách quá hạn.", "45000", 1644, OverdueBooksPendingException.class, "Có sách quá hạn.");
        assertMapped("BR05_OUTSTANDING_FINE", "[BR05_OUTSTANDING_FINE] Có phiếu phạt.", "45000", 1644, OutstandingFineException.class, "Có phiếu phạt.");
        assertMapped("BR05_QUOTA_EXCEEDED", "[BR05_QUOTA_EXCEEDED] Vượt quota.", "45000", 1644, BorrowQuotaExceededException.class, "Vượt quota.");
        assertMapped("BR10_BOOK_NOT_AVAILABLE", "[BR10_BOOK_NOT_AVAILABLE] Sách không sẵn sàng.", "45000", 1644, BookNotAvailableException.class, "Sách không sẵn sàng.");
        assertMapped("BR11_FINE_ALREADY_SETTLED", "[BR11_FINE_ALREADY_SETTLED] Đã thanh toán.", "45000", 1644, FineAlreadySettledException.class, "Đã thanh toán.");
        assertMapped("INVALID_INPUT", "[INVALID_INPUT] Dữ liệu không hợp lệ.", "45000", 1644, InvalidInputException.class, "Dữ liệu không hợp lệ.");
        assertMapped("INVALID_DATE_RANGE", "[INVALID_DATE_RANGE] Ngày trả không hợp lệ.", "45000", 1644, InvalidDateRangeException.class, "Ngày trả không hợp lệ.");
        assertMapped("INVALID_AMOUNT", "[INVALID_AMOUNT] Số tiền không hợp lệ.", "45000", 1644, InvalidAmountException.class, "Số tiền không hợp lệ.");

        // 2. Non-direct prefix with SQLSTATE 45000 (embedded wrapper message)
        assertMapped("Embedded wrapper", "SQLError: [BR01_INELIGIBLE_MEMBER] Wrapped error", "45000", 1644, IneligibleMemberException.class, "Wrapped error");

        // 3. Fallbacks: unknown code & standard SQL error must produce DatabaseException with cause preserved
        SQLException unknownCode = new SQLException("[UNKNOWN_CODE] Some message", "45000", 1644);
        LibraryException mappedUnknown = SqlExceptionMapper.map(unknownCode);
        check("Fallback unknown code -> DatabaseException",
                mappedUnknown instanceof DatabaseException && mappedUnknown.getCause() == unknownCode);

        SQLException dbSyntaxErr = new SQLException("Unknown column 'x' in 'field list'", "42S22", 1054);
        LibraryException mappedDb = SqlExceptionMapper.map(dbSyntaxErr);
        check("Fallback SQL error -> DatabaseException with cause",
                mappedDb instanceof DatabaseException && mappedDb.getCause() == dbSyntaxErr);

        // 4. Live MySQL Connector/J SIGNAL format verification
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_borrow_book(?, ?, ?, ?)}")) {
            cs.setInt(1, 999999);
            cs.setInt(2, 1);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.DATE);
            cs.execute();
            check("Live Connector/J SIGNAL test", false);
        } catch (SQLException sqlEx) {
            System.out.println("[Live Connector/J] SQLState=" + sqlEx.getSQLState()
                    + ", ErrorCode=" + sqlEx.getErrorCode()
                    + ", Message=\"" + sqlEx.getMessage() + "\"");
            LibraryException mappedLive = SqlExceptionMapper.map(sqlEx);
            check("Live Connector/J SIGNAL [NOT_FOUND] -> ResourceNotFoundException",
                    mappedLive instanceof ResourceNotFoundException);
        }

        System.out.println("Result: PASSED=" + passed + ", FAILED=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertMapped(String name, String rawMsg, String sqlState, int vendorCode,
                                     Class<? extends LibraryException> expectedClass, String expectedCleanMsg) {
        SQLException sqlEx = new SQLException(rawMsg, sqlState, vendorCode);
        LibraryException mapped = SqlExceptionMapper.map(sqlEx);
        boolean ok = expectedClass.isInstance(mapped) && expectedCleanMsg.equals(mapped.getMessage());
        check("Prefix " + name, ok);
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + label);
        } else {
            failed++;
            System.out.println("[FAIL] " + label);
        }
    }
}
