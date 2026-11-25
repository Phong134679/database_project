package com.library.service;

import com.library.exception.InvalidInputException;
import com.library.exception.LicenseExpiredException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.AccessLog;
import com.library.model.DigitalJournal;
import com.library.model.GrantedLicense;
import com.library.model.LicensePackage;
import com.library.model.Person;
import com.library.repository.AccessLogRepository;
import com.library.repository.DigitalJournalRepository;
import com.library.repository.GrantedLicenseRepository;
import com.library.repository.LicensePackageRepository;
import com.library.repository.PersonRepository;
import com.library.validation.DataValidator;
import com.library.validation.LicenseValidator;
import com.library.validation.MemberValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service class implementing digital resource access control, MANUAL license grant/revoke,
 * and access logging (BR04, BR12, BR13, FR07, FR08).
 *
 * Quy tắc đọc tài liệu số:
 *   ALLOW = IS_AUTHOR OR RESOURCE.is_public = TRUE OR HAS_ALL_REQUIRED_LICENSES
 */
public class DigitalJournalService {

    private DigitalJournalRepository journalRepository;
    private LicensePackageRepository licensePackageRepository;
    private GrantedLicenseRepository grantedLicenseRepository;
    private AccessLogRepository accessLogRepository;
    private PersonRepository personRepository;

    public DigitalJournalService() {
        this.journalRepository = new DigitalJournalRepository();
        this.licensePackageRepository = new LicensePackageRepository();
        this.grantedLicenseRepository = new GrantedLicenseRepository();
        this.accessLogRepository = new AccessLogRepository();
        this.personRepository = new PersonRepository();
    }

    public DigitalJournalService(DigitalJournalRepository journalRepository,
                                 LicensePackageRepository licensePackageRepository,
                                 GrantedLicenseRepository grantedLicenseRepository,
                                 AccessLogRepository accessLogRepository,
                                 PersonRepository personRepository) {
        this.journalRepository = journalRepository;
        this.licensePackageRepository = licensePackageRepository;
        this.grantedLicenseRepository = grantedLicenseRepository;
        this.accessLogRepository = accessLogRepository;
        this.personRepository = personRepository;
    }

    /**
     * Executes digital journal access verification following the official order:
     * 1. AUTHOR_OF (cho phép ngay, bỏ qua is_public và License)
     * 2. RESOURCE.is_public == TRUE (cho phép ngay)
     * 3. Private -> phải có khai báo License trong RESOURCE_LICENSE và sở hữu TẤT CẢ (AND)
     */
    public boolean accessDigitalJournal(Integer personId, int journalResourceId, String ipAddress, LocalDate ignoredDate) {
        DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");

        // Kiểm tra tài nguyên số có tồn tại không
        DigitalJournal journal = journalRepository.findById(journalResourceId);
        LicenseValidator.validateJournalExists(journal, journalResourceId);

        // Ghi nhận nhật ký truy cập vào ACCESS_LOG (BR13 & FR08)
        AccessLog log = new AccessLog(
                0,
                journalResourceId,
                ipAddress != null ? ipAddress : "127.0.0.1"
        );
        accessLogRepository.insertLog(log);

        // Bước 1: Kiểm tra người dùng có phải tác giả của tài liệu hay không (AUTHOR_OF)
        if (personId != null && personId > 0) {
            boolean isAuthor = journalRepository.isAuthorOfResource(personId, journalResourceId);
            if (isAuthor) {
                Person author = personRepository.findById(personId);
                String authorName = author != null ? author.getName() : ("Person ID " + personId);
                System.out.println("[DigitalJournalService] ACCESS GRANTED (Đặc quyền Tác giả AUTHOR_OF): User [" + authorName +
                        "] -> \"" + journal.getTitle() + "\"");
                return true;
            }
        }

        // Bước 2 & 3: Kiểm tra RESOURCE.is_public
        if (journal.isPublic()) {
            System.out.println("[DigitalJournalService] ACCESS GRANTED (Tài nguyên công khai is_public = TRUE): \"" +
                    journal.getTitle() + "\" from IP: " + ipAddress);
            return true;
        }

        // Bước 4-7: Tài nguyên private -> yêu cầu người dùng hợp lệ và sở hữu ĐẦY ĐỦ tất cả License bắt buộc
        if (personId == null || personId <= 0) {
            throw new LicenseExpiredException("Tài liệu số này ở chế độ Private (Cần License). Vui lòng chọn người dùng để thẩm định quyền đọc.");
        }

        Person person = personRepository.findById(personId);
        MemberValidator.validatePersonExists(person, personId);
        String userName = person.getName();

        boolean hasAllLicenses = grantedLicenseRepository.hasAllRequiredLicenses(personId, journalResourceId);
        if (hasAllLicenses) {
            System.out.println("[DigitalJournalService] ACCESS GRANTED (Đủ toàn bộ License bắt buộc): User [" + userName +
                    "] -> \"" + journal.getTitle() + "\"");
            return true;
        } else {
            System.out.println("[DigitalJournalService] ACCESS BLOCKED: User [" + userName +
                    "] lacks required license(s) for \"" + journal.getTitle() + "\"");
            LicenseValidator.validateActiveLicense(false, userName, journal.getTitle());
            return false;
        }
    }

    public boolean accessDigitalJournal(int personId, int journalResourceId, String ipAddress, LocalDate accessDate) {
        return accessDigitalJournal(Integer.valueOf(personId), journalResourceId, ipAddress, accessDate);
    }

    public boolean accessDigitalJournal(int personId, int journalResourceId, String ipAddress) {
        return accessDigitalJournal(Integer.valueOf(personId), journalResourceId, ipAddress, null);
    }

    /**
     * Grants a license package to a person (MANUAL only).
     * Prevents duplicate grant for the same (person_id, license_id).
     */
    public GrantedLicense grantLicenseManual(int personId, int licenseId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(licenseId, "License ID");

        Person person = personRepository.findById(personId);
        MemberValidator.validatePersonExists(person, personId);

        LicensePackage lp = licensePackageRepository.findById(licenseId);
        LicenseValidator.validateLicensePackageExists(lp, licenseId);

        if (grantedLicenseRepository.existsGrant(personId, licenseId)) {
            throw new InvalidInputException(
                    "Người dùng [" + person.getName() + "] đã sở hữu gói bản quyền [" + lp.getLicenseName() + "] (Không thể cấp trùng)."
            );
        }

        GrantedLicense grant = new GrantedLicense(0, personId, licenseId);
        grantedLicenseRepository.insertGrant(grant);
        return grant;
    }

    /**
     * Revokes a license package from a person by (person_id, license_id).
     */
    public boolean revokeLicenseManual(int personId, int licenseId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(licenseId, "License ID");
        int deleted = grantedLicenseRepository.revokeGrant(personId, licenseId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Quyền cấp License (Person=" + personId + ", License=" + licenseId + ")", personId);
        }
        return true;
    }

    /**
     * Revokes a license grant by grant_id.
     */
    public boolean revokeLicenseByGrantId(int grantId) {
        DataValidator.validatePositiveId(grantId, "Grant ID");
        int deleted = grantedLicenseRepository.revokeByGrantId(grantId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Bản ghi cấp License (GRANTED_LICENSE)", grantId);
        }
        return true;
    }

    public List<LicensePackage> getAllLicensePackages() {
        return licensePackageRepository.findAll();
    }

    public List<Map<String, Object>> getAllGrantedLicensesDetailed() {
        return grantedLicenseRepository.findAllDetailed();
    }

    public DigitalJournal getJournalDetails(int journalResourceId) {
        DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");
        DigitalJournal journal = journalRepository.findById(journalResourceId);
        LicenseValidator.validateJournalExists(journal, journalResourceId);
        return journal;
    }

    public List<DigitalJournal> getAllJournals() {
        return journalRepository.findAll();
    }

    public List<AccessLog> getLogsByPerson(int personId) {
        return accessLogRepository.findByPersonId(personId);
    }

    public List<AccessLog> getAllAccessLogs() {
        return accessLogRepository.findAll();
    }

    public List<Map<String, Object>> getAllAccessLogsDetailed() {
        return accessLogRepository.findAllDetailed();
    }

    public DigitalJournalRepository getJournalRepository() {
        return journalRepository;
    }

    public void setJournalRepository(DigitalJournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    public GrantedLicenseRepository getGrantedLicenseRepository() {
        return grantedLicenseRepository;
    }

    public void setGrantedLicenseRepository(GrantedLicenseRepository grantedLicenseRepository) {
        this.grantedLicenseRepository = grantedLicenseRepository;
    }

    public AccessLogRepository getAccessLogRepository() {
        return accessLogRepository;
    }

    public void setAccessLogRepository(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
    }

    public PersonRepository getPersonRepository() {
        return personRepository;
    }

    public void setPersonRepository(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }
}
