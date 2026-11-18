package com.library.exception;

/**
 * Thrown when attempting to pay against a fine that is already fully settled (Completed status).
 */
public class FineAlreadySettledException extends BusinessException {

    public static final String DEFAULT_CODE = "BR11_FINE_ALREADY_SETTLED";

    public FineAlreadySettledException(String message) {
        super(DEFAULT_CODE, message);
    }
}
