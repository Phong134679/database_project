package com.library.validation;

import com.library.exception.BorrowQuotaExceededException;
import com.library.exception.IneligibleMemberException;
import com.library.exception.OutstandingFineException;
import com.library.exception.OverdueBooksPendingException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Person;

/**
 * Validator for user identity, role privileges (BR01), quota limits, and outstanding obligations (BR05).
 */
public final class MemberValidator {

    public static final int STUDENT_MAX_QUOTA = 3;
    public static final int FACULTY_MAX_QUOTA = 10;

    private MemberValidator() {
        // Prevent instantiation
    }

    /**
     * Validates that the Person entity exists.
     */
    public static void validatePersonExists(Person person, int personId) {
        if (person == null) {
            throw new ResourceNotFoundException("Độc giả/Người dùng", personId);
        }
    }

    /**
     * Validates member borrowing eligibility according to Test 01 (BR01) and BR05.
     * Throws specific BusinessException when an eligibility rule is violated.
     */
    public static void validateBorrowEligibility(Person person, boolean hasOverdue, boolean hasFines, int activeBorrows) {
        validatePersonExists(person, person.getPersonId());

        String pType = person.getPersonType();

        // BR01 / Test 01: External users (GUEST, EXTERNAL_AUTHOR) cannot borrow physical books home
        if (!"STUDENT".equalsIgnoreCase(pType) && !"FACULTY".equalsIgnoreCase(pType)) {
            throw new IneligibleMemberException(
                    "Độc giả [" + person.getName() + "] mang vai trò '" + pType +
                    "' là đối tượng bên ngoài, chỉ được phép đọc tài liệu tại chỗ theo quy chế BR01."
            );
        }

        // BR05: Blocking members with overdue unreturned books
        if (hasOverdue) {
            throw new OverdueBooksPendingException(
                    "Độc giả [" + person.getName() + "] hiện đang giữ sách trễ hạn chưa trả. " +
                    "Yêu cầu hoàn trả sách quá hạn trước khi tiếp tục mượn mới (BR05)."
            );
        }

        // BR05: Blocking members with outstanding unpaid fines
        if (hasFines) {
            throw new OutstandingFineException(
                    "Độc giả [" + person.getName() + "] có phiếu phạt quá hạn chưa thanh toán. " +
                    "Yêu cầu tất toán công nợ phạt trước khi tiếp tục mượn mới (BR05)."
            );
        }

        // BR05: Borrowing quota enforcement
        boolean isFaculty = "FACULTY".equalsIgnoreCase(pType);
        int maxQuota = isFaculty ? FACULTY_MAX_QUOTA : STUDENT_MAX_QUOTA;
        if (activeBorrows >= maxQuota) {
            throw new BorrowQuotaExceededException(
                    "Độc giả [" + person.getName() + "] đã đạt hạn mức mượn tối đa (" +
                    activeBorrows + "/" + maxQuota + " cuốn) theo quy chế BR05."
            );
        }
    }
}
