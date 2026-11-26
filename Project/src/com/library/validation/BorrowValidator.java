package com.library.validation;

import com.library.exception.ResourceNotFoundException;
import com.library.model.Borrow;

/**
 * Validator for borrow entity existence checks.
 * Availability and return eligibility are enforced by MySQL
 * (sp_borrow_book and sp_return_book).
 */
public final class BorrowValidator {

    private BorrowValidator() {
    }

    public static void validateBorrowExists(Borrow borrow, int borrowId) {
        if (borrow == null) {
            throw new ResourceNotFoundException("Phiếu mượn", borrowId);
        }
    }
}
