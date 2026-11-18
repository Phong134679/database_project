package com.library.exception;

/**
 * Thrown when a member exceeds their borrowing quota (BR05: Student <= 3, Faculty <= 10).
 */
public class BorrowQuotaExceededException extends BusinessException {

    public static final String DEFAULT_CODE = "BR05_QUOTA_EXCEEDED";

    public BorrowQuotaExceededException(String message) {
        super(DEFAULT_CODE, message);
    }
}
