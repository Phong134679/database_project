package com.library.service;

import com.library.exception.InvalidInputException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class coordinating digital resource access, manual license grant/revoke,
 * and access log retrieval via MySQL Stored Procedures:
 * sp_access_digital_journal, sp_grant_license_manual, sp_revoke_license_manual.
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
     * via sp_access_digital_journal(p_person_id, p_resource_id, OUT p_allowed, OUT p_reason, OUT p_log_id).
     */
    public Map<String, Object> accessDigitalJournalWithResult(Integer personId, int journalResourceId) {
        DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");

        String sql = "{CALL sp_access_digital_journal(?, ?, ?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            if (personId != null) {
                cs.setInt(1, personId);
            } else {
                cs.setNull(1, Types.INTEGER);
            }
            cs.setInt(2, journalResourceId);
            cs.registerOutParameter(3, Types.BOOLEAN);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.registerOutParameter(5, Types.BIGINT);
            cs.execute();

            boolean allowed = cs.getBoolean(3);
            String reason = cs.getString(4);
            long logId = cs.getLong(5);

            System.out.println("[DigitalJournalService] Access Log #" + logId + ": "
                    + (allowed ? "ALLOWED" : "DENIED") + " — " + reason);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("allowed", allowed);
            result.put("reason", reason);
            result.put("logId", logId);
            return result;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return null;
        }
    }

    public boolean accessDigitalJournal(Integer personId, int journalResourceId) {
        Map<String, Object> res = accessDigitalJournalWithResult(personId, journalResourceId);
        return res != null && Boolean.TRUE.equals(res.get("allowed"));
    }

    /**
     * Grants a license package to a person via sp_grant_license_manual(p_person_id, p_license_id, OUT p_grant_id).
     */
    public GrantedLicense grantLicenseManual(int personId, int licenseId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(licenseId, "License ID");

        Person person = personRepository.findById(personId);
        MemberValidator.validatePersonExists(person, personId);

        LicensePackage lp = licensePackageRepository.findById(licenseId);
        LicenseValidator.validateLicensePackageExists(lp, licenseId);

        String sql = "{CALL sp_grant_license_manual(?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, personId);
            cs.setInt(2, licenseId);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();

            int grantId = cs.getInt(3);
            return new GrantedLicense(grantId, personId, licenseId);
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState()) || e.getErrorCode() == 1644) {
                throw new InvalidInputException(e.getMessage());
            }
            SqlExceptionMapper.throwMapped(e);
            return null;
        }
    }

    /**
     * Revokes a license package from a person via sp_revoke_license_manual(p_person_id, p_license_id, OUT p_deleted).
     */
    public boolean revokeLicenseManual(int personId, int licenseId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(licenseId, "License ID");

        String sql = "{CALL sp_revoke_license_manual(?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, personId);
            cs.setInt(2, licenseId);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();

            int deleted = cs.getInt(3);
            if (deleted == 0) {
                throw new ResourceNotFoundException("Quyền cấp License (Person=" + personId + ", License=" + licenseId + ")", personId);
            }
            return true;
        } catch (SQLException e) {
            SqlExceptionMapper.throwMapped(e);
            return false;
        }
    }

    /**
     * Revokes a license grant by grant_id while preserving API contract:
     * resolves (person_id, license_id) and delegates deletion to sp_revoke_license_manual.
     */
    public boolean revokeLicenseByGrantId(int grantId) {
        DataValidator.validatePositiveId(grantId, "Grant ID");
        GrantedLicense existing = grantedLicenseRepository.findById(grantId);
        if (existing == null) {
            throw new ResourceNotFoundException("Bản ghi cấp License (GRANTED_LICENSE)", grantId);
        }
        return revokeLicenseManual(existing.getPersonId(), existing.getLicenseId());
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
