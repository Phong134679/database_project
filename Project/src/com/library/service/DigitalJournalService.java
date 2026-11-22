package com.library.service;

import com.library.model.AccessLog;
import com.library.model.DigitalJournal;
import com.library.model.GrantedLicense;
import com.library.model.Person;
import com.library.repository.AccessLogRepository;
import com.library.repository.DigitalJournalRepository;
import com.library.repository.GrantedLicenseRepository;
import com.library.repository.LicensePackageRepository;
import com.library.repository.PersonRepository;
import com.library.validation.DataValidator;
import com.library.validation.LicenseValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class implementing digital resource access control, license entitlement,
 * and access logging (Validation Case Test 02, BR04, BR12, BR13, FR07, FR08).
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
     * Executes digital journal access verification and records access log (Test 02, BR04, BR12, BR13).
     *
     * @param personId User attempting to read/download.
     * @param journalResourceId Target digital resource.
     * @param ipAddress IP address of the requesting client.
     * @param accessDate Date of access request.
     * @return true if access granted (valid unexpired license), throws LicenseExpiredException if blocked.
     */
    public boolean accessDigitalJournal(int personId, int journalResourceId, String ipAddress, LocalDate accessDate) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");

        LocalDate checkDate = accessDate != null ? accessDate : LocalDate.now();

        // 1. Kiểm tra tài nguyên số có tồn tại không
        DigitalJournal journal = journalRepository.findById(journalResourceId);
        LicenseValidator.validateJournalExists(journal, journalResourceId);

        // 2. Kiểm tra thông tin người dùng
        Person person = personRepository.findById(personId);
        String userName = person != null ? person.getName() : ("Person ID " + personId);

        // 3. Kiểm tra tính hợp lệ của bản quyền (BR04, BR12), tài liệu công khai (Open Access),
        //    hoặc Đặc quyền Tác giả (Tác giả được tự do truy cập tác phẩm của mình)
        boolean isPublic = journal.isPublic();
        boolean isAuthor = journalRepository.isAuthorOfResource(personId, journalResourceId);
        boolean hasActiveLicense = isPublic || isAuthor || grantedLicenseRepository.hasActiveLicenseForJournal(personId, journalResourceId, checkDate);

        // 4. Ghi nhận nhật ký truy cập vào ACCESS_LOG bất kể thành công hay thất bại (BR13 & FR08)
        AccessLog log = new AccessLog(
                0,
                personId,
                journalResourceId,
                LocalDateTime.now(),
                ipAddress != null ? ipAddress : "127.0.0.1",
                hasActiveLicense
        );
        accessLogRepository.insertLog(log);

        // 5. Trả kết quả: Nếu Open Access, là Tác giả, hoặc có bản quyền hợp lệ -> Cấp quyền
        if (hasActiveLicense) {
            String accessType;
            if (isAuthor) {
                accessType = "Đặc quyền Tác giả (Tác giả tự do truy cập tác phẩm của mình)";
            } else if (isPublic) {
                accessType = "Tài liệu công khai (Open Access)";
            } else {
                accessType = "Gói bản quyền hợp lệ";
            }
            System.out.println("[DigitalJournalService] ACCESS GRANTED (" + accessType + "): User [" + userName +
                    "] accessed journal: \"" + journal.getTitle() + "\" (DOI: " + journal.getDoi() + ")");
            System.out.println("    -> Download URL: " + journal.getUrl() + " [Format: " + journal.getFormat() +
                    ", Size: " + journal.getFileSize() + " KB]");
            return true;
        } else {
            System.out.println("[DigitalJournalService] ACCESS BLOCKED: User [" + userName +
                    "] does NOT have an active license package for journal: \"" + journal.getTitle() + "\" on " + checkDate);
            LicenseValidator.validateActiveLicense(false, userName, journal.getTitle(), checkDate);
            return false;
        }
    }

    /**
     * Convenience method using current date for access check.
     */
    public boolean accessDigitalJournal(int personId, int journalResourceId, String ipAddress) {
        return accessDigitalJournal(personId, journalResourceId, ipAddress, LocalDate.now());
    }

    /**
     * Grants a license package to an individual user.
     */
    public boolean grantLicenseToUser(int personId, int licenseId, LocalDate validFrom, LocalDate validTo) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(licenseId, "License ID");
        LicenseValidator.validateLicenseGrant(validFrom, validTo);

        GrantedLicense grant = new GrantedLicense(0, personId, licenseId, validFrom, validTo);
        grantedLicenseRepository.insertGrant(grant);
        return grant.getGrantId() > 0;
    }

    /**
     * Retrieves details of a digital journal.
     */
    public DigitalJournal getJournalDetails(int journalResourceId) {
        DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");
        DigitalJournal journal = journalRepository.findById(journalResourceId);
        LicenseValidator.validateJournalExists(journal, journalResourceId);
        return journal;
    }

    /**
     * Retrieves all digital journals in the repository.
     */
    public List<DigitalJournal> getAllJournals() {
        return journalRepository.findAll();
    }

    /**
     * Retrieves access logs for a specific person.
     */
    public List<AccessLog> getLogsByPerson(int personId) {
        return accessLogRepository.findByPersonId(personId);
    }

    /**
     * Retrieves all access logs in the system.
     */
    public List<AccessLog> getAllAccessLogs() {
        return accessLogRepository.findAll();
    }

    /**
     * Retrieves all access logs with user names and journal titles for UI display.
     */
    public List<java.util.Map<String, Object>> getAllAccessLogsDetailed() {
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
