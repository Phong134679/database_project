package com.library.exception;

/**
 * Thrown when an input argument is null, empty, non-positive ID, or malformed.
 */
public class InvalidInputException extends ValidationException {

    public static final String DEFAULT_CODE = "INVALID_INPUT";

    public InvalidInputException(String message) {
        super(DEFAULT_CODE, message);
    }

    public InvalidInputException(String errorCode, String message) {
        super(errorCode, message);
    }
}
