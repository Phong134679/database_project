package com.library.validation;

import com.library.exception.BookAlreadyReturnedException;
import com.library.exception.BookNotAvailableException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Borrow;

/**
 * Validator for physical book copy availability and borrow/return lifecycle rules (BR10, BR02).
 */
public final class BorrowValidator {

    private BorrowValidator() {
        // Prevent instantiation
    }

    /**
     * Validates that a physical book copy is available for new loan (BR10).
     */
    public static void validateCopyAvailable(boolean isAvailable, String copyId) {
        if (!isAvailable) {
            throw new BookNotAvailableException(
                    "Bản sao sách (Copy ID: " + copyId + ") hiện không ở trạng thái sẵn sàng để mượn (đang được mượn hoặc hỏng/mất)."
            );
        }
    }

    public static void validateCopyAvailable(boolean isAvailable, int copyId) {
        validateCopyAvailable(isAvailable, String.valueOf(copyId));
    }

    /**
     * Validates that a borrow transaction exists.
     */
    public static void validateBorrowExists(Borrow borrow, int borrowId) {
        if (borrow == null) {
            throw new ResourceNotFoundException("Phiếu mượn", borrowId);
        }
    }

    /**
     * Validates that a book can be returned (must exist and not already returned).
     */
    public static void validateReturnEligibility(Borrow borrow, int borrowId) {
        validateBorrowExists(borrow, borrowId);
        if (borrow.getReturnDate() != null) {
            throw new BookAlreadyReturnedException(
                    "Phiếu mượn ID " + borrowId + " đã được hoàn tất trả vào ngày: " + borrow.getReturnDate()
            );
        }
    }
}
