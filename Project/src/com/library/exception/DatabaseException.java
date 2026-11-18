package com.library.exception;

/**
 * Exception thrown when a database or JDBC access error occurs in repository layer.
 */
public class DatabaseException extends LibraryException {

    public static final String DEFAULT_CODE = "DB_ERROR";

    public DatabaseException(String message) {
        super(DEFAULT_CODE, message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(DEFAULT_CODE, message, cause);
    }

    public DatabaseException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
