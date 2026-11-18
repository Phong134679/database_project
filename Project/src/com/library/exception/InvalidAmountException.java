package com.library.exception;

/**
 * Thrown when a financial amount (fine, payment) is negative, zero, or exceeds boundary.
 */
public class InvalidAmountException extends ValidationException {

    public static final String DEFAULT_CODE = "INVALID_AMOUNT";

    public InvalidAmountException(String message) {
        super(DEFAULT_CODE, message);
    }
}
