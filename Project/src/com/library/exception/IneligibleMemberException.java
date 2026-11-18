package com.library.exception;

/**
 * Thrown when an external guest or author attempts to borrow books home (BR01 / Test 01).
 */
public class IneligibleMemberException extends BusinessException {

    public static final String DEFAULT_CODE = "BR01_INELIGIBLE_MEMBER";

    public IneligibleMemberException(String message) {
        super(DEFAULT_CODE, message);
    }
}
