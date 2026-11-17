package com.library;

import com.library.controller.LibraryController;
import com.library.model.Borrow;
import com.library.repository.AccessLogRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.DigitalJournalRepository;
import com.library.repository.FineRepository;
import com.library.repository.GrantedLicenseRepository;
import com.library.repository.LicensePackageRepository;
import com.library.repository.PaymentTransactionRepository;
import com.library.repository.PersonRepository;
import com.library.repository.UniversityMemberRepository;
import com.library.service.BorrowService;
import com.library.service.DigitalJournalService;
import com.library.service.FineService;
import com.library.service.MemberService;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Main application entry point demonstrating full backend execution and all 4 validation cases:
 * - Test 01 (BR01, BR05): User Role Validation & External User Borrow Rejection.
 * - Test 02 (BR04, BR12, BR13): Digital Journal License Entitlement & Access Logging.
 * - Test 03 (FR05, BR02): Automatic Fine Generation on Overdue Return.
 * - Test 04 (FR06, BR11): Installment Fine Payment and Status Transition.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("  DIGITAL LIBRARY MANAGEMENT SYSTEM & OPEN REPOSITORY - BACKEND DEMO          ");
        System.out.println("===============================================================================");

        // 1. Kiểm tra kết nối tới MySQL và tự động khởi tạo CSDL nếu cần
        System.out.print("[1] Kiểm tra kết nối MySQL: ");
        boolean isConnected = DatabaseConnection.testConnection();
        if (!isConnected) {
            System.out.println("CHƯA KẾT NỐI ĐƯỢC CSDL 'library_db'.");
            System.out.println("    -> Đang tự động khởi tạo CSDL và nạp dữ liệu mẫu qua DatabaseInitializer...");
            String schema = new java.io.File("Project/schema.sql").exists() ? "Project/schema.sql" : "schema.sql";
            String sampleData = new java.io.File("Project/sample_data.sql").exists() ? "Project/sample_data.sql" : "sample_data.sql";
            boolean initSuccess = com.library.util.DatabaseInitializer.initializeDatabase(schema, sampleData);
            if (initSuccess) {
                isConnected = DatabaseConnection.testConnection();
            }
        }
        
        if (isConnected) {
            System.out.println("KẾT NỐI THÀNH CÔNG! (URL: " + DatabaseConnection.getUrl() + ")");
        } else {
            System.out.println("    Vui lòng đảm bảo MySQL service đang chạy và kiểm tra thông tin trong db.properties.");
        }

        // 2. Khởi tạo toàn bộ các Repositories của Backend
        PersonRepository personRepo = new PersonRepository();
        UniversityMemberRepository memberRepo = new UniversityMemberRepository();
        BorrowRepository borrowRepo = new BorrowRepository();
        FineRepository fineRepo = new FineRepository();
        PaymentTransactionRepository paymentRepo = new PaymentTransactionRepository();
        DigitalJournalRepository journalRepo = new DigitalJournalRepository();
        LicensePackageRepository licenseRepo = new LicensePackageRepository();
        GrantedLicenseRepository grantedLicenseRepo = new GrantedLicenseRepository();
        AccessLogRepository accessLogRepo = new AccessLogRepository();

        // 3. Khởi tạo toàn bộ các Services của Backend
        MemberService memberService = new MemberService(personRepo, memberRepo, borrowRepo, fineRepo);
        BorrowService borrowService = new BorrowService(borrowRepo, fineRepo, memberService);
        FineService fineService = new FineService(fineRepo, paymentRepo);
        DigitalJournalService journalService = new DigitalJournalService(
                journalRepo, licenseRepo, grantedLicenseRepo, accessLogRepo, personRepo
        );

        // 4. Khởi tạo Controller điều phối
        LibraryController controller = new LibraryController(borrowService, fineService, journalService, memberService);

        // 5. Hiển thị danh mục thành viên và tài nguyên ban đầu
        System.out.println("\n[2] Danh sách Thành viên & Độc giả trong hệ thống:");
        controller.displayAllMembers();

        System.out.println("\n[3] Danh mục Tài nguyên số (DIGITAL_JOURNAL):");
        controller.displayAllJournals();

        System.out.println("\n[4] Danh sách toàn bộ phiếu mượn từ bảng BORROW:");
        List<Borrow> allBorrows = borrowRepo.findAll();
        if (allBorrows.isEmpty()) {
            System.out.println("    (Hiện chưa có dữ liệu hoặc chưa chạy script sample_data.sql)");
        } else {
            for (Borrow b : allBorrows) {
                System.out.println("    -> " + b);
            }
        }

        // =============================================================================
        // VALIDATION CASE TEST 01 (BR01 & BR05): Kiểm tra phân quyền mượn sách vật lý
        // =============================================================================
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.println(">>> VALIDATION CASE TEST 01 (BR01 & BR05): Phân quyền mượn sách");
        System.out.println("-------------------------------------------------------------------------------");
        // Ca 1.1: Khách ngoài (Person ID 4 - Lê Văn Cường, vai trò GUEST) cố gắng mượn Bản sao 2 về nhà
        System.out.println("[Test 01.1] Khách ngoài (Person 4 - GUEST) yêu cầu mượn Bản sao sách 2:");
        controller.handleBorrowRequest(4, 2);

        // Ca 1.2: Thành viên nội bộ (Person ID 1 - Nguyễn Văn An, STUDENT) yêu cầu mượn Bản sao sách 2
        System.out.println("\n[Test 01.2] Sinh viên (Person 1 - STUDENT) yêu cầu mượn Bản sao sách 2:");
        controller.handleBorrowRequest(1, 2);

        // =============================================================================
        // VALIDATION CASE TEST 02 (BR04, BR12, BR13, FR08): Kiểm soát bản quyền số
        // =============================================================================
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.println(">>> VALIDATION CASE TEST 02 (BR04, BR12, BR13): Kiểm soát bản quyền tài liệu số");
        System.out.println("-------------------------------------------------------------------------------");
        // Ca 2.1: Khách ngoài Person 4 mở Journal 3 (gói IEEE đã hết hạn từ 30/06/2025)
        System.out.println("[Test 02.1] Person 4 truy cập Journal 3 (Gói IEEE của người này đã hết hạn):");
        controller.handleAccessDigitalJournal(4, 3, "192.168.1.100", LocalDate.of(2026, 11, 1));

        // Ca 2.2: Sinh viên Person 1 mở Journal 3 (gói IEEE còn hạn đến 31/12/2026)
        System.out.println("\n[Test 02.2] Sinh viên Person 1 truy cập Journal 3 (Gói IEEE còn hiệu lực):");
        controller.handleAccessDigitalJournal(1, 3, "192.168.1.50", LocalDate.of(2026, 11, 1));

        // Hiển thị nhật ký truy cập ACCESS_LOG (BR13 & FR08)
        controller.displayAccessLogs();

        // =============================================================================
        // VALIDATION CASE TEST 03 (FR05 & BR02): Tự động phát sinh phiếu phạt khi trả trễ
        // =============================================================================
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.println(">>> VALIDATION CASE TEST 03 (FR05 & BR02): Tự động sinh phạt trễ hạn");
        System.out.println("-------------------------------------------------------------------------------");
        // Giả lập trả sách cho Borrow ID 1 (Due Date 2026-10-21, trả ngày 2026-10-25 -> trễ 4 ngày * 10,000 = 40,000 VND)
        controller.handleReturnRequest(1, LocalDate.of(2026, 10, 25));

        // =============================================================================
        // VALIDATION CASE TEST 04 (FR06 & BR11): Thanh toán trả góp tiền phạt
        // =============================================================================
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.println(">>> VALIDATION CASE TEST 04 (FR06 & BR11): Thanh toán trả góp tiền phạt");
        System.out.println("-------------------------------------------------------------------------------");
        // Xem chi tiết tiền phạt và lịch sử thanh toán trước khi trả tiếp
        controller.displayFineDetails(1);

        // Thanh toán đợt 2 cho Fine ID 1: trả thêm 15,000 VND
        System.out.println("\n--- Thực hiện trả góp đợt 2 (15,000 VND qua Bank Transfer) ---");
        controller.handleFinePayment(1, BigDecimal.valueOf(15000), "Bank Transfer");
        controller.displayFineDetails(1);

        // Thanh toán đợt 3 để hoàn tất phiếu phạt
        System.out.println("\n--- Thực hiện trả nốt phần còn lại để tất toán phiếu phạt ---");
        controller.handleFinePayment(1, BigDecimal.valueOf(15000), "Credit Card");
        controller.displayFineDetails(1);

        System.out.println("\n===============================================================================");
        System.out.println("  HOÀN TẤT KIỂM THỬ TOÀN BỘ 4 VALIDATION CASES CỦA BACKEND                     ");
        System.out.println("===============================================================================");
    }
}
