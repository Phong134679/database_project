package com.library.validation;

import com.library.exception.BorrowQuotaExceededException;
import com.library.exception.IneligibleMemberException;
import com.library.exception.OutstandingFineException;
import com.library.exception.OverdueBooksPendingException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Person;
import com.library.model.UniversityMember;

/**
 * Validator for user identity, membership privileges (BR01), quota limits, and outstanding obligations (BR05).
 */
public final class MemberValidator {

    public static final int STUDENT_MAX_QUOTA = 3;
    public static final int FACULTY_MAX_QUOTA = 10;

    private MemberValidator() {
    }

    public static void validatePersonExists(Person person, int personId) {
        if (person == null) {
            throw new ResourceNotFoundException("Độc giả/Người dùng", personId);
        }
    }

    /**
     * Validates university member borrowing eligibility according to BR01 and BR05.
     */
    public static void validateBorrowEligibility(UniversityMember member, boolean hasOverdue, boolean hasFines, int activeBorrows) {
        if (member == null) {
            throw new IneligibleMemberException("Chỉ UNIVERSITY_MEMBER mới được phép mượn sách theo quy chế BR01.");
        }

        String mType = member.getMemberType();
        if (!"STUDENT".equalsIgnoreCase(mType) && !"FACULTY".equalsIgnoreCase(mType)) {
            throw new IneligibleMemberException(
                    "Thành viên [" + member.getName() + "] có loại '" + mType +
                    "' không hợp lệ để mượn sách theo quy chế BR01."
            );
        }

        if (hasOverdue) {
            throw new OverdueBooksPendingException(
                    "Thành viên [" + member.getName() + "] hiện đang giữ sách trễ hạn chưa trả. " +
                    "Yêu cầu hoàn trả sách quá hạn trước khi tiếp tục mượn mới (BR05)."
            );
        }

        if (hasFines) {
            throw new OutstandingFineException(
                    "Thành viên [" + member.getName() + "] còn phiếu phạt ở trạng thái Unpaid. " +
                    "Yêu cầu thanh toán trước khi tiếp tục mượn mới (BR05)."
            );
        }

        boolean isFaculty = "FACULTY".equalsIgnoreCase(mType);
        int maxQuota = isFaculty ? FACULTY_MAX_QUOTA : STUDENT_MAX_QUOTA;
        if (activeBorrows >= maxQuota) {
            throw new BorrowQuotaExceededException(
                    "Thành viên [" + member.getName() + "] đã đạt hạn mức mượn tối đa (" +
                    activeBorrows + "/" + maxQuota + " cuốn) theo quy chế BR05."
            );
        }
    }
}
