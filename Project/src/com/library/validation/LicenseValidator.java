package com.library.validation;

import com.library.exception.LicenseExpiredException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.DigitalJournal;

import java.time.LocalDate;

/**
 * Validator for digital resource entitlement, license coverage, and expiration (BR04, BR12).
 */
public final class LicenseValidator {

    private LicenseValidator() {
        // Prevent instantiation
    }

    /**
     * Validates that a digital journal resource exists.
     */
    public static void validateJournalExists(DigitalJournal journal, int journalResourceId) {
        if (journal == null) {
            throw new ResourceNotFoundException("Tạp chí / Tài liệu số (Digital Journal)", journalResourceId);
        }
    }

    /**
     * Validates that the user holds an active, unexpired license package for the digital resource.
     */
    public static void validateActiveLicense(boolean hasActiveLicense, String userName, String journalTitle, LocalDate checkDate) {
        if (!hasActiveLicense) {
            throw new LicenseExpiredException(
                    "Độc giả [" + userName + "] không có gói bản quyền còn hiệu lực cho tài liệu: \"" +
                    journalTitle + "\" tại thời điểm: " + checkDate + " (BR04, BR12)."
            );
        }
    }

    /**
     * Validates validity date range when granting a license package to a user.
     */
    public static void validateLicenseGrant(LocalDate validFrom, LocalDate validTo) {
        DataValidator.validateDateRange(validFrom, validTo, "Thời hạn cấp gói bản quyền");
    }
}
