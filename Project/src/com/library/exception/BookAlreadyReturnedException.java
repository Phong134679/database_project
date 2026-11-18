package com.library.exception;

/**
 * Thrown when attempting to return a book that has already been returned.
 */
public class BookAlreadyReturnedException extends BusinessException {

    public static final String DEFAULT_CODE = "BR02_ALREADY_RETURNED";

    public BookAlreadyReturnedException(String message) {
        super(DEFAULT_CODE, message);
    }
}
