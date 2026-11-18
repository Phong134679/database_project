package com.library.exception;

/**
 * Thrown when a date range is inverted (start date after end date) or invalid.
 */
public class InvalidDateRangeException extends ValidationException {

    public static final String DEFAULT_CODE = "INVALID_DATE_RANGE";

    public InvalidDateRangeException(String message) {
        super(DEFAULT_CODE, message);
    }
}
