# SQL Reference (Java + JDBC Equivalent)

## 1. Purpose

Bộ `SQL_Reference` này là **tài liệu tham khảo độc lập**, tái hiện lại bằng MySQL Views, Stored Functions và Stored Procedures toàn bộ các truy vấn phức tạp và nghiệp vụ đang được triển khai trong **Java Service + JDBC Repository** của hệ thống Library Management System.

- **Không phải thành phần runtime**: Ứng dụng Java hiện tại chạy hoàn toàn bằng JDBC thuần trên các bảng gốc.
- **Mục đích**: Tham khảo, đối chiếu giữa cách triển khai Java + JDBC và SQL thuần, phục vụ nghiên cứu và đánh giá thiết kế cơ sở dữ liệu.

---

## 2. Directory Structure

```text
SQL_Reference/
├── README.md                     # Tài liệu hướng dẫn & bảng đối chiếu Java ↔ SQL
├── 01_views_reference.sql        # 8 Database Views tương ứng truy vấn tổng hợp trong Repository/Service
├── 02_functions_reference.sql    # 3 Stored Functions tương ứng logic dùng chung trong Service
├── 03_procedures_reference.sql   # 7 Stored Procedures tương ứng nghiệp vụ giao dịch trong Service
├── 04_verification_queries.sql   # Các truy vấn read-only kiểm tra View, JOIN, NULL, subtype
└── tests/
    └── test_sql_reference.sql    # Kịch bản kiểm thử Function & Procedure trên database thử nghiệm
```

---

## 3. Java-to-SQL Mapping

| Java Component | Java Method | Mục đích / Nghiệp vụ | SQL Reference Object |
| :--- | :--- | :--- | :--- |
| `PersonRepository` + `MemberService` | `findAllDetailedRaw()` + `getAllPersonsDetailed()` | Danh sách người dùng, thành viên, quota, đếm mượn/quá hạn/phạt & điều kiện mượn | `VIEW_MEMBERS_DETAILED` |
| `ResourceRepository` | `findAllResourcesDetailed()` | Danh mục tài nguyên (hỗ trợ cả `PHYSICAL_BOOK`, `DIGITAL_JOURNAL`, `BOTH`) | `VIEW_RESOURCES_DETAILED` |
| `BorrowRepository` | `findAllCopiesDetailed()` | Danh sách bản sao sách vật lý kèm kệ & số license yêu cầu | `VIEW_BOOKS_DETAILED` |
| `DigitalJournalRepository` | `findById()`, `findAll()` | Danh mục tạp chí số | `VIEW_JOURNALS_DETAILED` |
| `BorrowRepository` + `BorrowService` | `findAllBorrowsDetailedRaw()` + `getAllBorrowsDetailed()` | Danh sách lượt mượn kèm trạng thái `Active`/`Overdue`/`Returned` & phạt mới nhất | `VIEW_BORROWS_DETAILED` |
| `FineRepository` | `findAllFinesDetailed()` | Danh sách phiếu phạt kèm giao dịch thanh toán mới nhất | `VIEW_FINES_DETAILED` |
| `AccessLogRepository` + `DigitalJournalService` | `findAllDetailedRaw()` + `getAllAccessLogsDetailed()` | Nhật ký truy cập tạp chí số (`Guest` hoặc tên `PERSON`) | `VIEW_ACCESS_LOGS_DETAILED` |
| `GrantedLicenseRepository` | `findAllDetailed()` | Danh sách cấp phép bản quyền | `VIEW_GRANTED_LICENSES_DETAILED` |
| `ResourceService` | `hasAllRequiredLicenses(Connection, int, int)` | Kiểm tra `PERSON` có đủ mọi license yêu cầu của `RESOURCE` | `fn_has_all_required_licenses` |
| `MemberService` | `getBorrowQuota(String)` | Tính quota mượn (`FACULTY=10`, `STUDENT=3`) | `fn_get_borrow_quota` |
| `MemberService` | `getLoanDurationDays(String)` | Tính số ngày mượn (`FACULTY=30`, `STUDENT=14`) | `fn_get_loan_duration_days` |
| `BorrowService` | `borrowBook(String, String)` | Nghiệp vụ & transaction mượn sách (`FOR UPDATE`, quota, license) | `sp_borrow_book` |
| `BorrowService` | `returnBookWithResult(int, LocalDate)` | Nghiệp vụ & transaction trả sách, tính/cộng dồn phạt 50,000 VND | `sp_return_book` |
| `FineService` | `confirmPayment(int)` | Nghiệp vụ & transaction xác nhận thanh toán phạt | `sp_confirm_fine_payment` |
| `DigitalJournalService` | `accessDigitalJournalWithResult(Integer, int)` | Thẩm định quyền đọc tạp chí số (Guest/Author/License) & ghi `ACCESS_LOG` | `sp_access_digital_journal` |
| `DigitalJournalService` | `grantLicenseManual(int, int)` | Cấp license thủ công (kiểm tra tồn tại & chống trùng) | `sp_grant_license_manual` |
| `DigitalJournalService` | `revokeLicenseManual(int, int)` | Thu hồi license theo `(person_id, license_id)` | `sp_revoke_license_manual` |
| `DigitalJournalService` | `revokeLicenseByGrantId(int)` | Thu hồi license theo `grant_id` | `sp_revoke_license_by_grant_id` |

---

## 4. Execution Instructions (Trên Database Thử Nghiệm)

> **Cảnh báo:** Không nạp các file này vào database runtime `library_db` đang chạy ứng dụng nếu không cần thiết, và **không** thêm vào `DatabaseInitializer.java`.

Thứ tự thực thi trên một database thử nghiệm riêng (ví dụ `library_db_ref_test`):

1. Tạo schema & nạp dữ liệu mẫu từ `Project/01_create_database_final_v2.sql` và `Project/02_insert_data_final_v2.sql`.
2. Chạy `SQL_Reference/01_views_reference.sql`.
3. Chạy `SQL_Reference/02_functions_reference.sql`.
4. Chạy `SQL_Reference/03_procedures_reference.sql`.
5. Chạy `SQL_Reference/04_verification_queries.sql` (read-only).
6. Chạy `SQL_Reference/tests/test_sql_reference.sql` để kiểm thử các Stored Procedures & Functions.

---

## 5. Important Notes & Limitations

- **Java + JDBC là nguồn chân lý runtime duy nhất**: Ứng dụng không gọi bất kỳ View, Function hay Procedure nào trong `SQL_Reference/`.
- **Chuỗi thông báo UI (`ineligibilityReason`)**: `MemberService.getAllPersonsDetailed()` tạo chuỗi thông báo tiếng Việt chi tiết theo từng nguyên nhân (`[name]`, quota...). `VIEW_MEMBERS_DETAILED` giữ toàn bộ cờ boolean (`is_member`, `has_overdue`, `has_unpaid_fines`, `is_eligible_to_borrow`) và số liệu gốc thay vì nhúng chuỗi UI dài vào View.
- **Đồng bộ tương lai**: Mọi thay đổi nghiệp vụ trong Java Service/Repository cần được cập nhật tương ứng vào `SQL_Reference/` nếu muốn tiếp tục dùng bộ tài liệu này để đối chiếu.
