package com.library.exception;

/**
 * Thrown when access to a digital journal is blocked due to missing or expired license (BR04, BR12).
 */
public class LicenseExpiredException extends BusinessException {

    public static final String DEFAULT_CODE = "BR04_LICENSE_EXPIRED";

    public LicenseExpiredException(String message) {
        super(DEFAULT_CODE, message);
    }
}
