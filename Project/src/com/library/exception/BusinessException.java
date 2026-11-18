package com.library.exception;

/**
 * Base exception for business logic rule violations (BR01, BR02, BR04, BR05, BR10, BR11, etc.).
 */
public class BusinessException extends LibraryException {

    public static final String DEFAULT_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessException(String message) {
        super(DEFAULT_CODE, message);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }
}
