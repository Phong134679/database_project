package com.library.util;

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

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility mapping MySQL Stored Procedure SQLException (SQLSTATE '45000',
 * vendor code 1644, MESSAGE_TEXT '[CODE] message') to domain LibraryException subclasses.
 */
public final class SqlExceptionMapper {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^\\s*\\[([A-Z0-9_]+)\\]\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern EMBEDDED_PREFIX_PATTERN = Pattern.compile("\\[([A-Z0-9_]+)\\]\\s*(.*)$", Pattern.DOTALL);

    private SqlExceptionMapper() {
    }

    /**
     * Converts a SQLException into a specific BusinessException/ValidationException
     * when a known error prefix is present, or DatabaseException otherwise.
     */
    public static LibraryException map(SQLException e) {
        if (e == null) {
            return new DatabaseException("Unknown SQL error (null SQLException).");
        }

        String rawMessage = e.getMessage() != null ? e.getMessage().trim() : "";
        boolean isUserDefinedSignal = "45000".equals(e.getSQLState()) || e.getErrorCode() == 1644;

        String code = null;
        String cleanMessage = rawMessage;

        Matcher direct = PREFIX_PATTERN.matcher(rawMessage);
        if (direct.matches()) {
            code = direct.group(1);
            cleanMessage = direct.group(2).trim();
        } else if (isUserDefinedSignal) {
            Matcher embedded = EMBEDDED_PREFIX_PATTERN.matcher(rawMessage);
            if (embedded.find()) {
                code = embedded.group(1);
                cleanMessage = embedded.group(2).trim();
            }
        }

        if (cleanMessage.isEmpty()) {
            cleanMessage = rawMessage;
        }

        if (code != null) {
            LibraryException mapped = mapByCode(code, cleanMessage);
            if (mapped != null) {
                return mapped;
            }
        }

        return new DatabaseException("Lỗi database: " + rawMessage, e);
    }

    /**
     * Maps a reason string (e.g. OUT p_reason from sp_access_digital_journal)
     * carrying a '[ERROR_CODE] message' prefix into its corresponding LibraryException.
     */
    public static LibraryException mapFromReason(String reason) {
        String raw = reason != null ? reason.trim() : "";
        Matcher m = PREFIX_PATTERN.matcher(raw);
        if (m.matches()) {
            String code = m.group(1);
            String cleanMessage = m.group(2).trim();
            LibraryException mapped = mapByCode(code, cleanMessage.isEmpty() ? raw : cleanMessage);
            if (mapped != null) {
                return mapped;
            }
        }
        return null;
    }

    /**
     * Translates a SQLException and immediately throws the mapped LibraryException.
     */
    public static void throwMapped(SQLException e) {
        throw map(e);
    }

    private static LibraryException mapByCode(String code, String message) {
        switch (code) {
            case "NOT_FOUND":
                return new ResourceNotFoundException(message);
            case "BR01_INELIGIBLE_MEMBER":
                return new IneligibleMemberException(message);
            case "BR02_ALREADY_RETURNED":
                return new BookAlreadyReturnedException(message);
            case "BR04_LICENSE_EXPIRED":
                return new LicenseExpiredException(message);
            case "BR05_OVERDUE_PENDING":
                return new OverdueBooksPendingException(message);
            case "BR05_OUTSTANDING_FINE":
                return new OutstandingFineException(message);
            case "BR05_QUOTA_EXCEEDED":
                return new BorrowQuotaExceededException(message);
            case "BR10_BOOK_NOT_AVAILABLE":
                return new BookNotAvailableException(message);
            case "BR11_FINE_ALREADY_SETTLED":
                return new FineAlreadySettledException(message);
            case "INVALID_INPUT":
                return new InvalidInputException(message);
            case "INVALID_DATE_RANGE":
                return new InvalidDateRangeException(message);
            case "INVALID_AMOUNT":
                return new InvalidAmountException(message);
            default:
                return null;
        }
    }
}
