package com.library.validation;

import com.library.exception.LicenseExpiredException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.DigitalJournal;
import com.library.model.LicensePackage;
import com.library.model.Resource;

import java.time.LocalDate;

/**
 * Validator for resource access authorization and license entitlement (BR04, BR12).
 * Đã bỏ kiểm tra thời hạn (valid_from / valid_to): chỉ kiểm tra sự tồn tại đầy đủ License yêu cầu.
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
     * Validates that a general resource exists.
     */
    public static void validateResourceExists(Resource resource, int resourceId) {
        if (resource == null) {
            throw new ResourceNotFoundException("Tài nguyên (Resource)", resourceId);
        }
    }

    /**
     * Validates that a license package exists.
     */
    public static void validateLicensePackageExists(LicensePackage lp, int licenseId) {
        if (lp == null) {
            throw new ResourceNotFoundException("Gói bản quyền (LicensePackage)", licenseId);
        }
    }

    /**
     * Validates that the user holds all required licenses for the private resource.
     */
    public static void validateActiveLicense(boolean hasAllRequiredLicenses, String userName, String resourceTitle) {
        if (!hasAllRequiredLicenses) {
            throw new LicenseExpiredException(
                    "Độc giả [" + userName + "] chưa sở hữu đầy đủ tất cả License bắt buộc cho tài nguyên: \"" +
                    resourceTitle + "\" (BR04, BR12)."
            );
        }
    }

    /**
     * Compatibility overload kept for existing callers/tests.
     */
    public static void validateActiveLicense(boolean hasAllRequiredLicenses, String userName, String resourceTitle, LocalDate ignoredDate) {
        validateActiveLicense(hasAllRequiredLicenses, userName, resourceTitle);
    }
}
