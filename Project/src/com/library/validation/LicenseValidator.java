package com.library.validation;

import com.library.exception.ResourceNotFoundException;
import com.library.model.DigitalJournal;
import com.library.model.LicensePackage;
import com.library.model.Resource;

/**
 * Validator for resource and license package existence checks.
 * License entitlement rules are enforced by MySQL (fn_has_all_required_licenses,
 * sp_access_digital_journal, sp_borrow_book).
 */
public final class LicenseValidator {

    private LicenseValidator() {
    }

    public static void validateJournalExists(DigitalJournal journal, int journalResourceId) {
        if (journal == null) {
            throw new ResourceNotFoundException("Tạp chí / Tài liệu số (Digital Journal)", journalResourceId);
        }
    }

    public static void validateResourceExists(Resource resource, int resourceId) {
        if (resource == null) {
            throw new ResourceNotFoundException("Tài nguyên (Resource)", resourceId);
        }
    }

    public static void validateLicensePackageExists(LicensePackage lp, int licenseId) {
        if (lp == null) {
            throw new ResourceNotFoundException("Gói bản quyền (LicensePackage)", licenseId);
        }
    }
}
