package com.library.exception;

/**
 * Thrown when a requested book copy is not in 'Available' status (BR10: e.g. 'Borrowed', 'Damaged', 'Lost').
 */
public class BookNotAvailableException extends BusinessException {

    public static final String DEFAULT_CODE = "BR10_BOOK_NOT_AVAILABLE";

    public BookNotAvailableException(String message) {
        super(DEFAULT_CODE, message);
    }
}
