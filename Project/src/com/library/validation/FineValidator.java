package com.library.validation;

import com.library.exception.FineAlreadySettledException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Fine;

import java.math.BigDecimal;

/**
 * Validator for fines and payment confirmations (BR11).
 * Không có bước tính toán trả góp, chỉ kiểm tra điều kiện xác nhận thanh toán.
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
    public static void validatePaymentEligibility(Fine fine, int fineId) {
        validateFineExists(fine, fineId);

        if ("Completed".equalsIgnoreCase(fine.getPaymentStatus())) {
            throw new FineAlreadySettledException(
                    "Phiếu phạt ID " + fineId + " đã được tất toán toàn bộ công nợ (Trạng thái: Completed)."
            );
        }
    }

    /**
     * Backward-compatible validator accepting an amount parameter.
     */
    public static void validatePaymentEligibility(Fine fine, BigDecimal amountPaid, int fineId) {
        validatePaymentEligibility(fine, fineId);
    }
}
