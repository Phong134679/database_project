package com.library.exception;

/**
 * Root unchecked exception for all custom library system exceptions.
 * Carries an optional error code and message.
 */
public abstract class LibraryException extends RuntimeException {

    private final String errorCode;

    public LibraryException(String message) {
        super(message);
        this.errorCode = "UNKNOWN_ERROR";
    }

    public LibraryException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN_ERROR";
    }

    public LibraryException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
