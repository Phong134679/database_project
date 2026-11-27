package com.library.validation;

import com.library.exception.ResourceNotFoundException;
import com.library.model.Borrow;

/**
 * Validator for borrow entity existence checks.
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
