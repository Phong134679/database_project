package com.library.validation;

import com.library.exception.ResourceNotFoundException;
import com.library.model.Person;

/**
 * Validator for user identity existence checks.
 * Membership eligibility, quota, overdue, and fine checks are enforced by MySQL
 * (sp_borrow_book and VIEW_MEMBERS_DETAILED).
 */
public final class MemberValidator {

    private MemberValidator() {
    }

    public static void validatePersonExists(Person person, int personId) {
        if (person == null) {
            throw new ResourceNotFoundException("Độc giả/Người dùng", personId);
        }
    }
}
