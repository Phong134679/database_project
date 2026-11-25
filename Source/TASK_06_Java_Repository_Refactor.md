# TASK 06 — Java Repository & Service Refactor

## Mục tiêu
Sau khi các Stored Procedures đã được tạo (TASK 01–05), refactor Java Service/Repository để:
- Gọi SP thay vì xử lý logic trong Java
- Đọc VIEW thay vì N+1 queries
- Parse error code từ `SQLException.getMessage()` → throw đúng `BusinessException`

> [!IMPORTANT]
> **Không thay đổi JSON contract của API.** Kiểm tra `ApiServer.java` trước và sau để đảm bảo response format không đổi.
> **Giữ nguyên DataValidator** (null check, ID ≤ 0, empty string, amount ≤ 0).

---

## Điều kiện tiên quyết

Phải hoàn thành trước khi bắt đầu TASK này:
- [x] TASK 01: `views.sql` đã deploy
- [x] TASK 02: `sp_borrow_book` đã deploy
- [x] TASK 03: `sp_return_book` đã deploy
- [x] TASK 04: `sp_confirm_fine_payment` đã deploy
- [x] TASK 05: `sp_access_digital_journal` đã deploy

---

## Helper: parseErrorCode (thêm vào BorrowService hoặc class tiện ích riêng)

```java
/**
 * Parse error code prefix từ SIGNAL MESSAGE_TEXT của MySQL.
 * Format: '[ERROR_CODE] human readable message'
 * → throw đúng BusinessException subclass tương ứng.
 */
private void throwFromSqlException(SQLException e) {
    String msg = e.getMessage() != null ? e.getMessage() : "";
    if (msg.startsWith("[NOT_FOUND]"))
        throw new ResourceNotFoundException(msg, 0);
    if (msg.startsWith("[BR01_INELIGIBLE_MEMBER]"))
        throw new IneligibleMemberException(msg);
    if (msg.startsWith("[BR02_ALREADY_RETURNED]"))
        throw new BookAlreadyReturnedException(msg);
    if (msg.startsWith("[BR04_LICENSE_EXPIRED]"))
        throw new LicenseExpiredException(msg);
    if (msg.startsWith("[BR05_OVERDUE_PENDING]"))
        throw new OverdueBooksPendingException(msg);
    if (msg.startsWith("[BR05_OUTSTANDING_FINE]"))
        throw new OutstandingFineException(msg);
    if (msg.startsWith("[BR05_QUOTA_EXCEEDED]"))
        throw new BorrowQuotaExceededException(msg);
    if (msg.startsWith("[BR10_BOOK_NOT_AVAILABLE]"))
        throw new BookNotAvailableException(msg);
    if (msg.startsWith("[BR11_FINE_ALREADY_SETTLED]"))
        throw new FineAlreadySettledException(msg);
    if (msg.startsWith("[INVALID_INPUT]"))
        throw new InvalidInputException(msg);
    throw new DatabaseException("Lỗi database: " + msg, e);
}
```

> [!NOTE]
> Đặt method này vào từng Service class cần dùng, hoặc tạo class `SqlExceptionMapper.java` trong package `com.library.util`.

---

## 1. BorrowService.java — processNewBorrow()

### Trước (Java xử lý tất cả):
```java
public boolean processNewBorrow(int memberId, String copyId) {
    DataValidator.validatePositiveId(memberId, "Member Person ID");
    DataValidator.validateNonEmptyString(copyId, "Copy ID");
    memberService.isEligibleToBorrow(memberId); // Nhiều DB queries
    // ... nhiều bước kiểm tra, INSERT, UPDATE
}
```

### Sau (gọi SP):
```java
public boolean processNewBorrow(int memberId, String copyId) {
    // Giữ DataValidator - Java validation
    DataValidator.validatePositiveId(memberId, "Member Person ID");
    DataValidator.validateNonEmptyString(copyId, "Copy ID");

    String sql = "{CALL sp_borrow_book(?, ?, ?, ?)}";
    try (Connection conn = DatabaseConnection.getConnection();
         CallableStatement cs = conn.prepareCall(sql)) {
        cs.setInt(1, memberId);
        cs.setString(2, copyId);
        cs.registerOutParameter(3, Types.INTEGER); // p_borrow_id
        cs.registerOutParameter(4, Types.DATE);    // p_due_date
        cs.execute();

        int borrowId = cs.getInt(3);
        Date dueDate = cs.getDate(4);
        System.out.println("[BorrowService] SUCCESS: Borrow #" + borrowId +
            " created. Due: " + dueDate);
        return true;
    } catch (SQLException e) {
        throwFromSqlException(e);
        return false; // unreachable
    }
}
```

---

## 2. BorrowService.java — returnBook()

### Trước:
```java
public boolean returnBook(int borrowId, LocalDate actualReturnDate) {
    // Nhiều bước: validate, check, UPDATE BORROW, UPDATE BOOK_COPY, INSERT FINE
}
```

### Sau:
```java
public boolean returnBook(int borrowId, LocalDate actualReturnDate) {
    DataValidator.validatePositiveId(borrowId, "Borrow ID");

    String sql = "{CALL sp_return_book(?, ?, ?, ?)}";
    try (Connection conn = DatabaseConnection.getConnection();
         CallableStatement cs = conn.prepareCall(sql)) {
        cs.setInt(1, borrowId);
        if (actualReturnDate != null) {
            cs.setDate(2, java.sql.Date.valueOf(actualReturnDate));
        } else {
            cs.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
        }
        cs.registerOutParameter(3, Types.INTEGER);        // p_fine_id
        cs.registerOutParameter(4, Types.DECIMAL);        // p_fine_amount
        cs.execute();

        int fineId = cs.getInt(3);
        double fineAmount = cs.getDouble(4);
        if (fineId > 0) {
            System.out.println("[BorrowService] OVERDUE. Fine #" + fineId +
                " generated (" + String.format("%,.0f", fineAmount) + " VND)");
        }
        return true;
    } catch (SQLException e) {
        throwFromSqlException(e);
        return false;
    }
}
```

---

## 3. FineService.java — confirmPayment()

### Trước:
```java
public boolean confirmPayment(int fineId) {
    DataValidator.validatePositiveId(fineId, "Fine ID");
    Fine fine = fineRepository.findById(fineId);
    FineValidator.validatePaymentEligibility(fine, fineId);
    // INSERT PAYMENT_TRANSACTION + UPDATE FINE trong transaction
}
```

### Sau:
```java
public boolean confirmPayment(int fineId) {
    DataValidator.validatePositiveId(fineId, "Fine ID");

    String sql = "{CALL sp_confirm_fine_payment(?, ?)}";
    try (Connection conn = DatabaseConnection.getConnection();
         CallableStatement cs = conn.prepareCall(sql)) {
        cs.setInt(1, fineId);
        cs.registerOutParameter(2, Types.INTEGER); // p_transaction_id
        cs.execute();

        int txnId = cs.getInt(2);
        System.out.println("[FineService] SUCCESS: Fine #" + fineId +
            " confirmed PAID (Transaction #" + txnId + ")");
        return true;
    } catch (SQLException e) {
        throwFromSqlException(e);
        return false;
    }
}
```

---

## 4. DigitalJournalService.java — accessDigitalJournal()

### Trước:
```java
public boolean accessDigitalJournal(Integer personId, int journalResourceId,
                                     String ipAddress, LocalDate accessDate) {
    // Nhiều bước: check journal, INSERT LOG, check author, check public, check license
}
```

### Sau:
```java
public boolean accessDigitalJournal(Integer personId, int journalResourceId,
                                     String ipAddress, LocalDate accessDate) {
    DataValidator.validatePositiveId(journalResourceId, "Journal Resource ID");
    DataValidator.validateNonEmptyString(ipAddress, "IP Address");

    String sql = "{CALL sp_access_digital_journal(?, ?, ?, ?, ?, ?)}";
    try (Connection conn = DatabaseConnection.getConnection();
         CallableStatement cs = conn.prepareCall(sql)) {
        if (personId != null && personId > 0) {
            cs.setInt(1, personId);
        } else {
            cs.setNull(1, Types.INTEGER);
        }
        cs.setInt(2, journalResourceId);
        cs.setString(3, ipAddress != null ? ipAddress : "0.0.0.0");
        cs.registerOutParameter(4, Types.BOOLEAN); // p_allowed
        cs.registerOutParameter(5, Types.VARCHAR); // p_reason
        cs.registerOutParameter(6, Types.INTEGER); // p_log_id
        cs.execute();

        boolean allowed = cs.getBoolean(4);
        String reason = cs.getString(5);
        int logId = cs.getInt(6);

        System.out.println("[DigitalJournalService] Access #" + logId +
            ": " + (allowed ? "ALLOWED" : "DENIED") + " — " + reason);

        if (!allowed) {
            if (reason != null && reason.startsWith("[BR04_LICENSE_EXPIRED]")) {
                throw new LicenseExpiredException(reason);
            }
            // Các từ chối khác: trả false (không throw exception)
        }
        return allowed;
    } catch (LicenseExpiredException e) {
        throw e;
    } catch (SQLException e) {
        throwFromSqlException(e);
        return false;
    }
}
```

---

## 5. MemberService.java — getAllPersonsDetailed()

### Trước (N+1 loop):
```java
public List<Map<String, Object>> getAllPersonsDetailed() {
    List<Person> persons = personRepository.findAll();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Person p : persons) {
        Map<String, Object> map = new LinkedHashMap<>();
        // ... nhiều query thêm cho từng person
        result.add(map);
    }
    return result;
}
```

### Sau (VIEW):
```java
public List<Map<String, Object>> getAllPersonsDetailed() {
    return personRepository.findAllPersonsDetailed();
}
```

Trong `PersonRepository.java`:
```java
public List<Map<String, Object>> findAllPersonsDetailed() {
    String sql = "SELECT * FROM VIEW_MEMBERS_DETAILED";
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        ResultSetMetaData meta = rs.getMetaData();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            result.add(row);
        }
    } catch (SQLException e) {
        throw new DatabaseException("Lỗi khi đọc VIEW_MEMBERS_DETAILED: " + e.getMessage(), e);
    }
    return result;
}
```

**Áp dụng tương tự cho:**
- `BorrowRepository.findAllBorrowsDetailed()` → `SELECT * FROM VIEW_BORROWS_DETAILED`
- `FineRepository.findAllFinesDetailed()` → `SELECT * FROM VIEW_FINES_DETAILED`
- `AccessLogRepository.findAllDetailed()` → `SELECT * FROM VIEW_ACCESS_LOGS_DETAILED`
- `GrantedLicenseRepository.findAllDetailed()` → `SELECT * FROM VIEW_GRANTED_LICENSES_DETAILED`

---

## 6. Dọn dẹp sau refactor

Các method Java có thể xóa hoặc để lại tùy theo có được gọi ở nơi khác không:

| Method | Action |
|---|---|
| `MemberService.isEligibleToBorrow()` | Kiểm tra xem có nơi nào khác gọi không. Nếu không: xóa. |
| `MemberService.getLoanDurationDays()` | Kiểm tra, nếu không: xóa. |
| `BorrowService.calculateLatePenalty()` | Giữ lại nếu UI dùng, hoặc tính trong VIEW. |
| `BorrowService.calculateMemberOverduePenalty()` | Giữ lại nếu UI dùng. |
| `FineValidator.validatePaymentEligibility()` | Có thể bỏ, SP đã kiểm tra. |
| `BorrowValidator.*` | Có thể bỏ, SP đã kiểm tra. |

> [!WARNING]
> Không xóa `DataValidator.*` và các method READ-ONLY không liên quan đến business logic.

---

## Import cần thêm vào các Service file

```java
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
```

---

## Build & Verification

```bash
cd /home/yuzu/Downloads/database_project/Project

# Compile
javac -d bin -cp "src:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" $(find src -name "*.java")

# Phải không có lỗi compile
echo "Build result: $?"

# Chạy API server
java -cp "bin:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" com.library.api.ApiServer &
sleep 2

# Smoke test endpoints
curl -s http://localhost:8080/api/members | python3 -m json.tool | head -20
curl -s http://localhost:8080/api/borrows | python3 -m json.tool | head -20
curl -s http://localhost:8080/api/fines | python3 -m json.tool | head -20
```

Kết quả mong đợi: Tất cả endpoint trả về JSON hợp lệ, format giống như trước khi refactor.
