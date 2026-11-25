package com.library.test;

import com.library.exception.IneligibleMemberException;
import com.library.exception.InvalidInputException;
import com.library.exception.LicenseExpiredException;
import com.library.model.GrantedLicense;
import com.library.model.Person;
import com.library.model.UniversityMember;
import com.library.repository.GrantedLicenseRepository;
import com.library.repository.PersonRepository;
import com.library.repository.UniversityMemberRepository;
import com.library.service.BorrowService;
import com.library.service.DigitalJournalService;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Integration test suite verifying all 15 mandatory License Management &
 * Resource Access Control test cases against MySQL.
 */
public class LicenseAccessControlTest {

    private static int passed = 0;
    private static int total = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("  KIỂM THỬ BẮT BUỘC: LICENSE MANAGEMENT & RESOURCE ACCESS CONTROL (15 CASES)   ");
        System.out.println("===============================================================================");

        DigitalJournalService journalService = new DigitalJournalService();
        BorrowService borrowService = new BorrowService();
        GrantedLicenseRepository grantedRepo = new GrantedLicenseRepository();
        PersonRepository personRepo = new PersonRepository();
        UniversityMemberRepository memberRepo = new UniversityMemberRepository();

        // Chuẩn bị dữ liệu test tạm cho các kịch bản:
        // - tempBothUser: có cả L1 và L2
        // - tempMultiLicJournal: private, yêu cầu cả L1 và L2
        // - tempEmptyLicJournal: private, KHÔNG khai báo License trong RESOURCE_LICENSE
        Person tempBothUser = createTempMember("999000111001", "Temp User L1+L2", "SV_TMP_BOTH");
        journalService.grantLicenseManual(tempBothUser.getPersonId(), 1);
        journalService.grantLicenseManual(tempBothUser.getPersonId(), 2);

        int multiLicJournalId = createTempDigitalJournal("Temp Multi-License Journal", false, new int[]{1, 2});
        int emptyLicJournalId = createTempDigitalJournal("Temp Private No-License Journal", false, new int[]{});

        String publicBookCopyId = findAvailableCopy(1);
        String privateBookCopyId = findAvailableCopy(2);

        try {
            // 1. Resource public: người dùng không có License vẫn được truy cập
            check("1. Resource public (#4): người dùng không có License (Guest #4) vẫn được truy cập", () ->
                    journalService.accessDigitalJournal(4, 4, "127.0.0.1")
            );

            // 2. Resource private yêu cầu L1 và L2: người có cả hai được truy cập
            check("2. Resource private (L1 & L2): người có cả hai được truy cập", () ->
                    journalService.accessDigitalJournal(tempBothUser.getPersonId(), multiLicJournalId, "127.0.0.1")
            );

            // 3. Resource private yêu cầu L1 và L2: người chỉ có L1 bị từ chối
            check("3. Resource private (L1 & L2): người chỉ có L1 (Person 1) bị từ chối", () -> {
                try {
                    journalService.accessDigitalJournal(1, multiLicJournalId, "127.0.0.1");
                    return false;
                } catch (LicenseExpiredException e) {
                    return true;
                }
            });

            // 4. Resource private yêu cầu L1 và L2: người chỉ có L2 bị từ chối
            check("4. Resource private (L1 & L2): người chỉ có L2 (Person 2) bị từ chối", () -> {
                try {
                    journalService.accessDigitalJournal(2, multiLicJournalId, "127.0.0.1");
                    return false;
                } catch (LicenseExpiredException e) {
                    return true;
                }
            });

            // 5. Resource private yêu cầu L1 và L2: người không có License bị từ chối
            check("5. Resource private (L1 & L2): người không có License (Person 4) bị từ chối", () -> {
                try {
                    journalService.accessDigitalJournal(4, multiLicJournalId, "127.0.0.1");
                    return false;
                } catch (LicenseExpiredException e) {
                    return true;
                }
            });

            // 6. Resource private không khai báo License: người dùng thông thường bị từ chối
            check("6. Resource private không khai báo License: người dùng thường (Person 1) bị từ chối", () -> {
                try {
                    journalService.accessDigitalJournal(1, emptyLicJournalId, "127.0.0.1");
                    return false;
                } catch (LicenseExpiredException e) {
                    return true;
                }
            });

            // 7. Tác giả đọc tài liệu số của mình: được phép dù private và không có License
            check("7. Tác giả đọc tài liệu số của mình (#3 bởi Person 3): được phép dù private & không License", () ->
                    journalService.accessDigitalJournal(3, 3, "127.0.0.1")
            );

            // 8. Tác giả không được dùng đặc quyền để mượn sách vật lý
            check("8. Tác giả không được dùng đặc quyền AUTHOR_OF để mượn sách vật lý", () -> {
                // Case 8a: External Author #3 luôn bị chặn bởi BR01
                boolean extBlocked = false;
                try {
                    borrowService.processNewBorrow(3, publicBookCopyId);
                } catch (IneligibleMemberException e) {
                    extBlocked = true;
                }
                // Case 8b: Thành viên nội bộ là tác giả của sách vật lý private nhưng không có License -> vẫn bị chặn!
                boolean internalAuthorBlocked = false;
                Person internalAuthor = createTempMember("999000111002", "Temp Author Member", "SV_TMP_AUTH");
                linkAuthor(internalAuthor.getPersonId(), 2); // gán làm tác giả sách #2 (private, yêu cầu L2)
                try {
                    borrowService.processNewBorrow(internalAuthor.getPersonId(), privateBookCopyId);
                } catch (LicenseExpiredException e) {
                    internalAuthorBlocked = true;
                } finally {
                    deleteTempPerson(internalAuthor.getPersonId());
                }
                return extBlocked && internalAuthorBlocked;
            });

            // 9. Sách vật lý public: người không có License vẫn qua bước kiểm tra quyền
            check("9. Sách vật lý public (#1): Person 2 không cần License IEEE vẫn mượn thành công", () -> {
                boolean ok = borrowService.processNewBorrow(2, publicBookCopyId);
                cleanupLatestBorrow(publicBookCopyId);
                return ok;
            });

            // 10. Sách vật lý private: phải có đủ License trước khi thực hiện quy trình mượn
            check("10. Sách vật lý private (#2, yêu cầu L2): thiếu L2 bị chặn; có L2 (Person 2) mượn được", () -> {
                boolean blockedWithoutLic = false;
                Person noLicStudent = createTempMember("999000111003", "Temp Student NoLic", "SV_TMP_NOLIC");
                try {
                    try {
                        borrowService.processNewBorrow(noLicStudent.getPersonId(), privateBookCopyId);
                    } catch (LicenseExpiredException e) {
                        blockedWithoutLic = true;
                    }
                    boolean allowedWithLic = borrowService.processNewBorrow(2, privateBookCopyId);
                    cleanupLatestBorrow(privateBookCopyId);
                    return blockedWithoutLic && allowedWithLic;
                } finally {
                    deleteTempPerson(noLicStudent.getPersonId());
                }
            });

            // 11, 12, 13: Cấp License, Chống cấp trùng, Thu hồi License
            GrantedLicense[] createdGrant = new GrantedLicense[1];
            check("11. Thủ thư cấp License (MANUAL) thành công cho Guest #4 (gói 1)", () -> {
                createdGrant[0] = journalService.grantLicenseManual(4, 1);
                return createdGrant[0] != null && createdGrant[0].getGrantId() > 0 && journalService.accessDigitalJournal(4, 3, "127.0.0.1");
            });

            check("12. Cấp trùng cùng 1 License cho cùng 1 PERSON bị ngăn chặn", () -> {
                try {
                    journalService.grantLicenseManual(4, 1);
                    return false;
                } catch (InvalidInputException e) {
                    return true;
                }
            });

            check("13. Thủ thư thu hồi License thành công & quyền truy cập bị thu hồi ngay", () -> {
                boolean revoked = journalService.revokeLicenseByGrantId(createdGrant[0].getGrantId());
                boolean blockedAfterRevoke = false;
                try {
                    journalService.accessDigitalJournal(4, 3, "127.0.0.1");
                } catch (LicenseExpiredException e) {
                    blockedAfterRevoke = true;
                }
                return revoked && blockedAfterRevoke;
            });

            // 14. PERSON mới mặc định không có License
            check("14. PERSON mới mặc định không có License", () -> {
                Person p = new Person(0, "999000333444", "New Guest", "HN", "newguest@test.edu", "0998", "GUEST");
                personRepo.insertPerson(p);
                try {
                    List<GrantedLicense> list = grantedRepo.findByPersonId(p.getPersonId());
                    return list.isEmpty();
                } finally {
                    deleteTempPerson(p.getPersonId());
                }
            });

            // 15. UNIVERSITY_MEMBER mới mặc định không có License
            check("15. UNIVERSITY_MEMBER mới mặc định không có License", () -> {
                Person p = createTempMember("999000555666", "New Member", "SV_TMP02");
                try {
                    List<GrantedLicense> list = grantedRepo.findByPersonId(p.getPersonId());
                    return list.isEmpty();
                } finally {
                    deleteTempPerson(p.getPersonId());
                }
            });
        } finally {
            deleteTempResource(multiLicJournalId);
            deleteTempResource(emptyLicJournalId);
            deleteTempPerson(tempBothUser.getPersonId());
        }

        System.out.println("\n===============================================================================");
        System.out.println("  KẾT QUẢ: " + passed + "/" + total + " TEST CASES BẮT BUỘC PASSED!");
        System.out.println("===============================================================================");
        if (passed != total) {
            System.exit(1);
        }
    }

    private interface Check {
        boolean run() throws Exception;
    }

    private static void check(String name, Check c) {
        total++;
        try {
            boolean ok = c.run();
            if (ok) {
                passed++;
                System.out.println("[PASS] " + name);
            } else {
                System.out.println("[FAIL] " + name);
            }
        } catch (Exception e) {
            System.out.println("[FAIL] " + name + " -> Exception: " + e.getMessage());
        }
    }

    private static Person createTempMember(String ssn, String name, String memberCode) {
        PersonRepository pr = new PersonRepository();
        UniversityMemberRepository mr = new UniversityMemberRepository();
        Person p = new Person(0, ssn, name, "HN", memberCode.toLowerCase() + "@test.edu", "0999", "STUDENT");
        pr.insertPerson(p);
        mr.insertMember(new UniversityMember(p.getPersonId(), p.getSsn(), p.getName(), "HN", p.getEmail(), "0999", "STUDENT", memberCode, "CNTT", "STUDENT", "KTPM", null));
        return p;
    }

    private static int createTempDigitalJournal(String title, boolean isPublic, int[] requiredLicenseIds) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int resId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO RESOURCE (title, publish_date, topic, resource_type, is_public) VALUES (?, '2026-01-01', 'Test', 'DIGITAL_JOURNAL', ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, title);
                ps.setBoolean(2, isPublic);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    resId = rs.getInt(1);
                }
            }
            // Hỗ trợ cả DB có cột doi lẫn DB đã bỏ cột doi
            boolean hasDoi = false;
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "DIGITAL_JOURNAL", "doi")) {
                hasDoi = rs.next();
            }
            if (hasDoi) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO DIGITAL_JOURNAL (resource_id, doi, url, file_size, format) VALUES (?, ?, 'https://test.edu/doc.pdf', 100, 'PDF')")) {
                    ps.setInt(1, resId);
                    ps.setString(2, "10.9999/test." + resId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO DIGITAL_JOURNAL (resource_id, url, file_size, format) VALUES (?, 'https://test.edu/doc.pdf', 100, 'PDF')")) {
                    ps.setInt(1, resId);
                    ps.executeUpdate();
                }
            }
            for (int lid : requiredLicenseIds) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO RESOURCE_LICENSE (resource_id, license_id) VALUES (?, ?)")) {
                    ps.setInt(1, resId);
                    ps.setInt(2, lid);
                    ps.executeUpdate();
                }
            }
            return resId;
        }
    }

    private static String findAvailableCopy(int resourceId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT copy_id FROM BOOK_COPY WHERE resource_id = ? AND availability_status = 'Available' LIMIT 1")) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        throw new IllegalStateException("No available copy found for resource_id=" + resourceId);
    }

    private static void linkAuthor(int personId, int resourceId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO AUTHOR_OF (person_id, resource_id) VALUES (?, ?)")) {
            ps.setInt(1, personId);
            ps.setInt(2, resourceId);
            ps.executeUpdate();
        }
    }

    private static void cleanupLatestBorrow(String copyId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Integer borrowId = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT borrow_id FROM BORROW WHERE copy_id = ? AND return_date IS NULL ORDER BY borrow_id DESC LIMIT 1")) {
                ps.setString(1, copyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) borrowId = rs.getInt(1);
                }
            }
            if (borrowId != null) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BORROW WHERE borrow_id = ?")) {
                    ps.setInt(1, borrowId);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE BOOK_COPY SET availability_status = 'Available' WHERE copy_id = ?")) {
                ps.setString(1, copyId);
                ps.executeUpdate();
            }
        }
    }

    private static void deleteTempPerson(int personId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM PERSON WHERE person_id = ?")) {
            ps.setInt(1, personId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private static void deleteTempResource(int resourceId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM RESOURCE WHERE resource_id = ?")) {
            ps.setInt(1, resourceId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
