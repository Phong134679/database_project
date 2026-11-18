package com.library.exception;

/**
 * Base exception for data and primitive constraint validation failures.
 */
public class ValidationException extends LibraryException {

    public static final String DEFAULT_CODE = "VALIDATION_ERROR";

    public ValidationException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
