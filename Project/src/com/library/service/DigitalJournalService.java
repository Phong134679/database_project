package com.library.service;

import com.library.exception.InvalidInputException;
import com.library.exception.LibraryException;
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
import com.library.util.DatabaseConnection;
import com.library.util.SqlExceptionMapper;
import com.library.validation.DataValidator;
import com.library.validation.LicenseValidator;
import com.library.validation.MemberValidator;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
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
     * Executes digital journal access verification and ACCESS_LOG recording
     * via MySQL Stored Procedure sp_access_digital_journal (uses MySQL NOW()).
     */
    public boolean accessDigitalJournal(Integer personId, int journalResourceId, String ipAddress) {
        DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");

        String sql = "{CALL sp_access_digital_journal(?, ?, ?, ?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            if (personId != null && personId > 0) {
                cs.setInt(1, personId);
            } else {
                cs.setNull(1, Types.INTEGER);
            }
            cs.setInt(2, journalResourceId);
            if (ipAddress != null) {
                cs.setString(3, ipAddress);
            } else {
                cs.setNull(3, Types.VARCHAR);
            }
            cs.registerOutParameter(4, Types.BOOLEAN); // p_allowed
            cs.registerOutParameter(5, Types.VARCHAR); // p_reason
            cs.registerOutParameter(6, Types.BIGINT);  // p_log_id
            cs.execute();

            boolean allowed = cs.getBoolean(4);
            String reason = cs.getString(5);
            long logId = cs.getLong(6);

            System.out.println("[DigitalJournalService] Access Log #" + logId + ": "
                    + (allowed ? "ALLOWED" : "DENIED") + " — " + reason);

            if (!allowed) {
                LibraryException mapped = SqlExceptionMapper.mapFromReason(reason);
                if (mapped != null) {
                    throw mapped;
                }
                return false;
            }
            return true;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return false;
        }
    }

    public boolean accessDigitalJournal(int personId, int journalResourceId, String ipAddress) {
        return accessDigitalJournal(Integer.valueOf(personId), journalResourceId, ipAddress);
    }

    @Deprecated
    public boolean accessDigitalJournal(int personId, int journalResourceId, String ipAddress, LocalDate accessDate) {
        return accessDigitalJournal(Integer.valueOf(personId), journalResourceId, ipAddress);
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
