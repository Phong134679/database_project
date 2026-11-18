package com.library.exception;

/**
 * Thrown when an expected entity (Member, BookCopy, Borrow, Fine, Journal, License)
 * does not exist in the database.
 */
public class ResourceNotFoundException extends BusinessException {

    public static final String DEFAULT_CODE = "NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(DEFAULT_CODE, resourceType + " with identifier [" + identifier + "] was not found.");
    }
}
