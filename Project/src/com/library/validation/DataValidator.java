package com.library.validation;

import com.library.exception.InvalidAmountException;
import com.library.exception.InvalidDateRangeException;
import com.library.exception.InvalidInputException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Utility validator for basic input data formats, positive numbers, non-empty strings, and date boundaries.
 */
public final class DataValidator {

    private DataValidator() {
        // Prevent instantiation
    }

    /**
     * Validates that an integer identifier is strictly positive (> 0).
     */
    public static void validatePositiveId(int id, String fieldName) {
        if (id <= 0) {
            throw new InvalidInputException(fieldName + " phải là số nguyên dương lớn hơn 0 (nhận được: " + id + ").");
        }
    }

    /**
     * Validates that a string is neither null nor blank.
     */
    public static void validateNonEmptyString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " không được để trống hoặc mang giá trị null.");
        }
    }

    /**
     * Validates that a monetary amount is not null and strictly greater than zero.
     */
    public static void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new InvalidAmountException(fieldName + " không được là null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(fieldName + " phải lớn hơn 0 (nhận được: " + amount + ").");
        }
    }

    /**
     * Validates that a date range is valid (validFrom <= validTo).
     */
    public static void validateDateRange(LocalDate from, LocalDate to, String fieldName) {
        if (from == null || to == null) {
            throw new InvalidDateRangeException(fieldName + ": Cả ngày bắt đầu và ngày kết thúc đều không được null.");
        }
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException(fieldName + ": Ngày bắt đầu (" + from + ") không thể sau ngày kết thúc (" + to + ").");
        }
    }
}
