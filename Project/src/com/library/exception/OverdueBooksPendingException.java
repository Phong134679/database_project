package com.library.exception;

/**
 * Thrown when a member attempts to borrow while holding overdue unreturned books (BR05).
 */
public class OverdueBooksPendingException extends BusinessException {

    public static final String DEFAULT_CODE = "BR05_OVERDUE_PENDING";

    public OverdueBooksPendingException(String message) {
        super(DEFAULT_CODE, message);
    }
}
