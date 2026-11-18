package com.library.validation;

import com.library.exception.FineAlreadySettledException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Fine;

import java.math.BigDecimal;

/**
 * Validator for fines and installment payment transactions (BR02, BR11).
 */
public final class FineValidator {

    private FineValidator() {
        // Prevent instantiation
    }

    /**
     * Validates that a Fine entity exists.
     */
    public static void validateFineExists(Fine fine, int fineId) {
        if (fine == null) {
            throw new ResourceNotFoundException("Phiếu phạt (Fine)", fineId);
        }
    }

    /**
     * Validates payment eligibility against a fine (BR11).
     */
    public static void validatePaymentEligibility(Fine fine, BigDecimal amountPaid, int fineId) {
        DataValidator.validatePositiveAmount(amountPaid, "Số tiền thanh toán");
        validateFineExists(fine, fineId);

        if ("Completed".equalsIgnoreCase(fine.getPaymentStatus())) {
            throw new FineAlreadySettledException(
                    "Phiếu phạt ID " + fineId + " đã được tất toán toàn bộ công nợ (Trạng thái: Completed)."
            );
        }
    }
}
