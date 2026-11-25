# TASK 01 — SQL Views (Thay thế N+1 Java Queries)

## Mục tiêu
Tạo file `Project/views.sql` chứa các VIEW thay thế các vòng lặp N+1 truy vấn trong Java Service.  
Sau khi có VIEW, Java chỉ cần `SELECT * FROM VIEW_...` mà không cần ghép dữ liệu thủ công.

---

## Phạm vi

### File cần tạo mới
- `Project/views.sql`

### File Java cần chỉnh sửa (sau khi view đã được deploy lên MySQL)
- `Project/src/com/library/repository/PersonRepository.java` → `findAllPersonsDetailed()`
- `Project/src/com/library/repository/BorrowRepository.java` → `findAllBorrowsDetailed()`
- `Project/src/com/library/repository/FineRepository.java` → `findAllFinesDetailed()`
- `Project/src/com/library/repository/AccessLogRepository.java` → `findAllDetailed()`
- `Project/src/com/library/repository/GrantedLicenseRepository.java` → `findAllDetailed()`

---

## Các VIEW cần tạo

### 1. VIEW_MEMBERS_DETAILED
Hiển thị thông tin đầy đủ của từng thành viên, bao gồm thông tin eligibility để borrow sách.

Cột bắt buộc:
```
person_id, name, email, person_type,
member_id, membership_type, join_date,
loan_duration_days (14 nếu STUDENT, 30 nếu FACULTY),
borrow_quota (3 nếu STUDENT, 10 nếu FACULTY),
active_borrow_count,
overdue_borrow_count,
unpaid_fine_count,
is_eligible (BOOLEAN: TRUE nếu person_type IN ('STUDENT','FACULTY') AND overdue_borrow_count = 0 AND unpaid_fine_count = 0 AND active_borrow_count < quota)
```

> [!NOTE]
> `is_eligible` chỉ là thông tin tham khảo cho UI. Logic thực tế vẫn được kiểm tra trong `sp_borrow_book`.

### 2. VIEW_RESOURCES_DETAILED
Hiển thị thông tin đầy đủ của từng Resource.

Cột bắt buộc:
```
resource_id, title, resource_type, is_public,
-- Nếu PHYSICAL_BOOK:
copy_id, copy_condition, availability_status,
-- Nếu DIGITAL_JOURNAL:
journal_id, volume_number, issue_number, publication_date, doi,
-- Chung:
required_license_count (COUNT từ RESOURCE_LICENSE)
```

> [!NOTE]
> Nếu VIEW ghép PHYSICAL_BOOK và DIGITAL_JOURNAL khó khăn, có thể tách thành VIEW_BOOKS_DETAILED và VIEW_JOURNALS_DETAILED.

### 3. VIEW_BORROWS_DETAILED
Hiển thị thông tin mượn sách kèm tên thành viên và tên tài nguyên.

Cột bắt buộc:
```
borrow_id, person_id, member_name,
copy_id, resource_title,
borrow_date, due_date, return_date,
status (COMPUTED: 'Returned' nếu return_date NOT NULL, 'Overdue' nếu due_date < CURDATE() AND return_date IS NULL, 'Active' otherwise),
fine_id (nếu có), fine_amount, fine_status
```

### 4. VIEW_FINES_DETAILED
Hiển thị phiếu phạt kèm tên thành viên và thông tin mượn sách.

Cột bắt buộc:
```
fine_id, borrow_id, person_id, member_name,
copy_id, resource_title,
borrow_date, due_date, return_date,
total_fine_amount, payment_status,
transaction_id (nếu đã thanh toán), payment_date
```

### 5. VIEW_ACCESS_LOGS_DETAILED
Hiển thị log truy cập tài liệu số kèm tên người dùng và tên tài liệu.

Cột bắt buộc:
```
log_id, person_id, person_name, person_type,
resource_id, journal_title,
access_date, ip_address, access_result
```

### 6. VIEW_GRANTED_LICENSES_DETAILED
Hiển thị danh sách cấp phép kèm tên người dùng và tên gói bản quyền.

Cột bắt buộc:
```
grant_id, person_id, person_name, person_type,
license_id, license_name, provider, description
```

---

## Cấu trúc bảng liên quan (tham khảo thực tế từ DB)

Trước khi viết VIEW, hãy kiểm tra cấu trúc thực tế:
```sql
SHOW COLUMNS FROM PERSON;
SHOW COLUMNS FROM UNIVERSITY_MEMBER;
SHOW COLUMNS FROM RESOURCE;
SHOW COLUMNS FROM PHYSICAL_BOOK;
SHOW COLUMNS FROM DIGITAL_JOURNAL;
SHOW COLUMNS FROM BOOK_COPY;
SHOW COLUMNS FROM BORROW;
SHOW COLUMNS FROM FINE;
SHOW COLUMNS FROM PAYMENT_TRANSACTION;
SHOW COLUMNS FROM ACCESS_LOG;
SHOW COLUMNS FROM GRANTED_LICENSE;
SHOW COLUMNS FROM LICENSE_PACKAGE;
SHOW COLUMNS FROM RESOURCE_LICENSE;
SHOW COLUMNS FROM AUTHOR_OF;
```

---

## Quy tắc viết VIEW

1. **Dùng LEFT JOIN** khi cột từ bảng con có thể NULL.
2. **COMPUTED columns** (is_eligible, status, loan_duration_days) dùng `CASE WHEN`.
3. **Không dùng Subquery không tương quan trong SELECT** nếu MySQL version < 8.0 không hỗ trợ window functions → dùng LEFT JOIN với aggregation thay thế.
4. Đặt `USE library_db;` đầu file.
5. Mỗi VIEW nên có `DROP VIEW IF EXISTS` trước `CREATE OR REPLACE VIEW`.

---

## Deployment

```bash
mysql -u root -p library_db < Project/views.sql
```

Kiểm tra sau deploy:
```sql
USE library_db;
SELECT * FROM VIEW_MEMBERS_DETAILED LIMIT 5;
SELECT * FROM VIEW_BORROWS_DETAILED LIMIT 5;
SELECT * FROM VIEW_FINES_DETAILED LIMIT 5;
SELECT * FROM VIEW_ACCESS_LOGS_DETAILED LIMIT 5;
SELECT * FROM VIEW_GRANTED_LICENSES_DETAILED LIMIT 5;
```

---

## Java Repository Changes

Sau khi VIEW hoạt động, thay thế trong từng Repository:

**PersonRepository.java** — tìm method `findAllPersonsDetailed()` hoặc tương đương:
```java
// Trước
// Vòng lặp N+1: for each person { query thêm }

// Sau
String sql = "SELECT * FROM VIEW_MEMBERS_DETAILED";
// Map kết quả vào List<Map<String, Object>>
```

**BorrowRepository.java** — tìm method `findAllBorrowsDetailed()`:
```java
String sql = "SELECT * FROM VIEW_BORROWS_DETAILED";
```

**FineRepository.java** — tìm method `findAllFinesDetailed()`:
```java
String sql = "SELECT * FROM VIEW_FINES_DETAILED";
```

**AccessLogRepository.java** — tìm method `findAllDetailed()`:
```java
String sql = "SELECT * FROM VIEW_ACCESS_LOGS_DETAILED";
```

**GrantedLicenseRepository.java** — tìm method `findAllDetailed()`:
```java
String sql = "SELECT * FROM VIEW_GRANTED_LICENSES_DETAILED";
```

> [!IMPORTANT]
> Tên cột trả về trong Map phải khớp với tên cột mà ApiServer đang serialize sang JSON. Kiểm tra `Project/src/com/library/api/ApiServer.java` trước khi đổi tên cột VIEW.

---

## Verification

```bash
cd /home/yuzu/Downloads/database_project/Project
javac -d bin -cp "src:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" $(find src -name "*.java")
java -cp "bin:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" com.library.api.ApiServer &
curl -s http://localhost:8080/api/members | python3 -m json.tool | head -40
curl -s http://localhost:8080/api/borrows | python3 -m json.tool | head -40
curl -s http://localhost:8080/api/fines | python3 -m json.tool | head -40
```

Kết quả mong đợi: Tất cả endpoint trả về JSON hợp lệ, không lỗi.
