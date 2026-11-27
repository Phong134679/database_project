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
import com.library.repository.ResourceRepository;
import com.library.util.TransactionManager;
import com.library.validation.DataValidator;
import com.library.validation.LicenseValidator;
import com.library.validation.MemberValidator;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service class coordinating digital resource access, manual license grant/revoke,
 * and access log retrieval via pure Java business logic and JDBC transactions.
 */
public class DigitalJournalService {

    private DigitalJournalRepository journalRepository;
    private LicensePackageRepository licensePackageRepository;
    private GrantedLicenseRepository grantedLicenseRepository;
    private AccessLogRepository accessLogRepository;
    private PersonRepository personRepository;
    private ResourceRepository resourceRepository;
    private ResourceService resourceService;

    public DigitalJournalService() {
        this.journalRepository = new DigitalJournalRepository();
        this.licensePackageRepository = new LicensePackageRepository();
        this.grantedLicenseRepository = new GrantedLicenseRepository();
        this.accessLogRepository = new AccessLogRepository();
        this.personRepository = new PersonRepository();
        this.resourceRepository = new ResourceRepository();
        this.resourceService = new ResourceService(this.resourceRepository, this.grantedLicenseRepository);
    }

    public DigitalJournalService(DigitalJournalRepository journalRepository,
                                 LicensePackageRepository licensePackageRepository,
                                 GrantedLicenseRepository grantedLicenseRepository,
                                 AccessLogRepository accessLogRepository,
                                 PersonRepository personRepository) {
        this(journalRepository, licensePackageRepository, grantedLicenseRepository,
                accessLogRepository, personRepository, new ResourceRepository());
    }

    public DigitalJournalService(DigitalJournalRepository journalRepository,
                                 LicensePackageRepository licensePackageRepository,
                                 GrantedLicenseRepository grantedLicenseRepository,
                                 AccessLogRepository accessLogRepository,
                                 PersonRepository personRepository,
                                 ResourceRepository resourceRepository) {
        this(journalRepository, licensePackageRepository, grantedLicenseRepository,
                accessLogRepository, personRepository, resourceRepository,
                new ResourceService(resourceRepository, grantedLicenseRepository));
    }

    public DigitalJournalService(DigitalJournalRepository journalRepository,
                                 LicensePackageRepository licensePackageRepository,
                                 GrantedLicenseRepository grantedLicenseRepository,
                                 AccessLogRepository accessLogRepository,
                                 PersonRepository personRepository,
                                 ResourceRepository resourceRepository,
                                 ResourceService resourceService) {
        this.journalRepository = journalRepository;
        this.licensePackageRepository = licensePackageRepository;
        this.grantedLicenseRepository = grantedLicenseRepository;
        this.accessLogRepository = accessLogRepository;
        this.personRepository = personRepository;
        this.resourceRepository = resourceRepository;
        this.resourceService = resourceService;
    }

    /**
     * Evaluates digital journal access rules in Java and records ACCESS_LOG on success
     * within a single JDBC transaction.
     */
    public Map<String, Object> accessDigitalJournalWithResult(Integer personId, int journalResourceId) {
        DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");

        return TransactionManager.executeInTransaction(conn -> {
            DigitalJournal journal = journalRepository.findById(conn, journalResourceId);
            if (journal == null) {
                throw new ResourceNotFoundException("Không tìm thấy DIGITAL_JOURNAL với resource_id đã cho.");
            }

            boolean allowed = false;
            String reason;
            long logId = 0L;

            if (personId == null) {
                if (journal.isPublic()) {
                    allowed = true;
                    reason = "Guest không định danh được truy cập journal công khai.";
                } else {
                    reason = "Guest không định danh chỉ được truy cập journal công khai.";
                }
            } else {
                Person person = personRepository.findById(conn, personId);
                if (person == null) {
                    reason = "PERSON ID không tồn tại.";
                } else {
                    boolean isAuthor = journalRepository.isAuthorOfResource(conn, personId, journalResourceId);
                    if (isAuthor) {
                        allowed = true;
                        reason = "PERSON là tác giả của journal; bỏ qua kiểm tra License.";
                    } else if (!resourceService.hasRequiredLicensesConfigured(conn, journalResourceId)) {
                        reason = "Journal không phải do PERSON này sáng tác và chưa khai báo License yêu cầu.";
                    } else if (resourceService.hasAllRequiredLicenses(conn, personId, journalResourceId)) {
                        allowed = true;
                        reason = "PERSON không phải tác giả nhưng có đầy đủ License bắt buộc.";
                    } else {
                        reason = "PERSON không phải tác giả và chưa có đầy đủ License bắt buộc.";
                    }
                }
            }

            if (allowed) {
                String generatedIp = generateSyntheticTestNetIp();
                logId = accessLogRepository.insertAccessLog(conn, personId, journalResourceId, generatedIp);
            }

            System.out.println("[DigitalJournalService] Access Log #" + logId + ": "
                    + (allowed ? "ALLOWED" : "DENIED") + " — " + reason);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("allowed", allowed);
            result.put("reason", reason);
            result.put("logId", logId);
            return result;
        });
    }

    private String generateSyntheticTestNetIp() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int block = rng.nextInt(3);
        int host = rng.nextInt(1, 255);
        switch (block) {
            case 0:
                return "192.0.2." + host;
            case 1:
                return "198.51.100." + host;
            default:
                return "203.0.113." + host;
        }
    }

    public boolean accessDigitalJournal(Integer personId, int journalResourceId) {
        Map<String, Object> res = accessDigitalJournalWithResult(personId, journalResourceId);
        return res != null && Boolean.TRUE.equals(res.get("allowed"));
    }

    public boolean isAuthorOfResource(int personId, int journalResourceId) {
        return journalRepository.isAuthorOfResource(personId, journalResourceId);
    }

    /**
     * Grants a license package to a person via JDBC transaction.
     */
    public GrantedLicense grantLicenseManual(int personId, int licenseId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(licenseId, "License ID");

        return TransactionManager.executeInTransaction(conn -> {
            Person person = personRepository.findById(conn, personId);
            MemberValidator.validatePersonExists(person, personId);

            LicensePackage lp = licensePackageRepository.findById(conn, licenseId);
            LicenseValidator.validateLicensePackageExists(lp, licenseId);

            if (grantedLicenseRepository.existsByPersonAndLicense(conn, personId, licenseId)) {
                throw new InvalidInputException("Người dùng đã sở hữu gói License này.");
            }
            try {
                int grantId = grantedLicenseRepository.insertGrant(conn, personId, licenseId);
                return new GrantedLicense(grantId, personId, licenseId);
            } catch (SQLIntegrityConstraintViolationException dup) {
                throw new InvalidInputException("Người dùng đã sở hữu gói License này.");
            }
        });
    }

    /**
     * Revokes a license package from a person via JDBC transaction.
     */
    public boolean revokeLicenseManual(int personId, int licenseId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        DataValidator.validatePositiveId(licenseId, "License ID");

        return TransactionManager.executeInTransaction(conn -> {
            int deleted = grantedLicenseRepository.deleteByPersonAndLicense(conn, personId, licenseId);
            if (deleted == 0) {
                throw new ResourceNotFoundException(
                        "Quyền cấp License (Person=" + personId + ", License=" + licenseId + ")", personId);
            }
            return true;
        });
    }

    /**
     * Revokes a license grant by grant_id via JDBC transaction.
     */
    public boolean revokeLicenseByGrantId(int grantId) {
        DataValidator.validatePositiveId(grantId, "Grant ID");

        return TransactionManager.executeInTransaction(conn -> {
            int deleted = grantedLicenseRepository.deleteByGrantId(conn, grantId);
            if (deleted == 0) {
                throw new ResourceNotFoundException("Bản ghi cấp License (GRANTED_LICENSE)", grantId);
            }
            return true;
        });
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

    /**
     * Enriches raw access logs with accessedAs ('Guest' or personName) in Java.
     */
    public List<Map<String, Object>> getAllAccessLogsDetailed() {
        List<Map<String, Object>> rawList = accessLogRepository.findAllDetailedRaw();
        List<Map<String, Object>> result = new ArrayList<>(rawList.size());

        for (Map<String, Object> raw : rawList) {
            Integer personId = (Integer) raw.get("personId");
            String personName = (String) raw.get("personName");
            String accessedAs = (personId == null) ? "Guest" : personName;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("logId", raw.get("logId"));
            map.put("personId", personId);
            map.put("personName", personName);
            map.put("accessedAs", accessedAs);
            map.put("journalResourceId", raw.get("journalResourceId"));
            map.put("journalTitle", raw.get("journalTitle"));
            map.put("isPublic", raw.get("isPublic"));
            map.put("journalUrl", raw.get("journalUrl"));
            map.put("journalFormat", raw.get("journalFormat"));
            map.put("ipAddress", raw.get("ipAddress"));
            result.add(map);
        }
        return result;
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
