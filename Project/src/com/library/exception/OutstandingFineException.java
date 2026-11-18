package com.library.exception;

/**
 * Thrown when a member attempts to borrow while having unsettled overdue fines (BR05).
 */
public class OutstandingFineException extends BusinessException {

    public static final String DEFAULT_CODE = "BR05_OUTSTANDING_FINE";

    public OutstandingFineException(String message) {
        super(DEFAULT_CODE, message);
    }
}
