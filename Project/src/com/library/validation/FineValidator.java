package com.library.validation;

import com.library.exception.ResourceNotFoundException;
import com.library.model.Fine;

/**
 * Validator for fine entity existence checks.
 */
public final class FineValidator {

    private FineValidator() {
    }

    public static void validateFineExists(Fine fine, int fineId) {
        if (fine == null) {
            throw new ResourceNotFoundException("Phiếu phạt (Fine)", fineId);
        }
    }
}
