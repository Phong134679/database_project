# TASK 02 — Stored Procedure: sp_borrow_book

## Mục tiêu
Chuyển toàn bộ logic nghiệp vụ của `BorrowService.processNewBorrow()` vào MySQL Stored Procedure.  
Java chỉ còn gọi `CALL sp_borrow_book(?,?,?,?)` và đọc tham số OUT.

---

## Phạm vi

### File SQL cần tạo / cập nhật
- `Project/procedures.sql` — Thêm `sp_borrow_book` vào cuối file

### File Java cần chỉnh sửa (xem TASK 06)
- `Project/src/com/library/service/BorrowService.java` — `processNewBorrow()`
- `Project/src/com/library/repository/BorrowRepository.java` — xóa logic kiểm tra, giữ lại insert/update

---

## Logic nghiệp vụ hiện tại trong Java (cần chuyển sang SQL)

Từ `BorrowService.processNewBorrow(int memberId, String copyId)`:

```
1. [BR01] Kiểm tra person_type ∈ {STUDENT, FACULTY}
   → Nếu không: throw IneligibleMemberException("[BR01_INELIGIBLE_MEMBER] ...")

2. [BR05-a] Kiểm tra không có phiếu mượn quá hạn chưa trả
   → Nếu có: throw OverdueBooksPendingException("[BR05_OVERDUE_PENDING] ...")

3. [BR05-b] Kiểm tra không có phiếu phạt chưa thanh toán
   → Nếu có: throw OutstandingFineException("[BR05_OUTSTANDING_FINE] ...")

4. [BR05-c] Kiểm tra quota (STUDENT=3, FACULTY=10)
   → Đếm số borrow active (return_date IS NULL)
   → Nếu vượt quota: throw BorrowQuotaExceededException("[BR05_QUOTA_EXCEEDED] ...")

5. [BR10] Kiểm tra copy_id tồn tại và availability_status = 'Available'
   → Nếu không: throw BookNotAvailableException("[BR10_BOOK_NOT_AVAILABLE] ...")

6. INSERT INTO BORROW (person_id, copy_id, borrow_date, due_date)
   → due_date = borrow_date + loan_duration (STUDENT=14, FACULTY=30 ngày)

7. UPDATE BOOK_COPY SET availability_status = 'Borrowed' WHERE copy_id = ?

8. Toàn bộ trong 1 transaction (COMMIT hoặc ROLLBACK)
```

> [!NOTE]
> Không có kiểm tra License cho physical book trong `processNewBorrow()` hiện tại (đã xác nhận từ BorrowService.java). SP cũng không cần thêm kiểm tra License cho physical book.

---

## Stored Procedure cần viết

### Signature
```sql
CREATE PROCEDURE sp_borrow_book(
    IN  p_member_id    INT,        -- person_id của thành viên mượn sách
    IN  p_copy_id      VARCHAR(50),-- copy_id của bản sao sách
    OUT p_borrow_id    INT,        -- borrow_id sau khi INSERT thành công
    OUT p_due_date     DATE        -- due_date tính toán
)
```

### Logic SQL

```sql
DELIMITER //
DROP PROCEDURE IF EXISTS sp_borrow_book//
CREATE PROCEDURE sp_borrow_book(
    IN  p_member_id  INT,
    IN  p_copy_id    VARCHAR(50),
    OUT p_borrow_id  INT,
    OUT p_due_date   DATE
)
BEGIN
    DECLARE v_person_type      VARCHAR(30);
    DECLARE v_overdue_count    INT DEFAULT 0;
    DECLARE v_fine_count       INT DEFAULT 0;
    DECLARE v_active_borrows   INT DEFAULT 0;
    DECLARE v_quota            INT DEFAULT 0;
    DECLARE v_loan_days        INT DEFAULT 14;
    DECLARE v_copy_status      VARCHAR(30);
    DECLARE v_copy_exists      INT DEFAULT 0;
    DECLARE v_borrow_date      DATE;

    -- BR01: Kiểm tra loại người dùng
    SELECT person_type INTO v_person_type
    FROM PERSON WHERE person_id = p_member_id;

    IF v_person_type IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy thành viên với ID đã cho.';
    END IF;

    IF v_person_type NOT IN ('STUDENT', 'FACULTY') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR01_INELIGIBLE_MEMBER] Chỉ STUDENT và FACULTY mới được phép mượn sách vật lý.';
    END IF;

    -- BR05-a: Kiểm tra có borrow quá hạn chưa trả không
    SELECT COUNT(*) INTO v_overdue_count
    FROM BORROW
    WHERE person_id = p_member_id
      AND return_date IS NULL
      AND due_date < CURDATE();

    IF v_overdue_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR05_OVERDUE_PENDING] Thành viên đang có sách mượn quá hạn chưa trả.';
    END IF;

    -- BR05-b: Kiểm tra phiếu phạt chưa thanh toán
    SELECT COUNT(*) INTO v_fine_count
    FROM FINE f
    INNER JOIN BORROW b ON f.borrow_id = b.borrow_id
    WHERE b.person_id = p_member_id
      AND f.payment_status = 'Unpaid';

    IF v_fine_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR05_OUTSTANDING_FINE] Thành viên có phiếu phạt chưa thanh toán.';
    END IF;

    -- BR05-c: Kiểm tra quota
    IF v_person_type = 'STUDENT' THEN
        SET v_quota = 3;
        SET v_loan_days = 14;
    ELSE -- FACULTY
        SET v_quota = 10;
        SET v_loan_days = 30;
    END IF;

    SELECT COUNT(*) INTO v_active_borrows
    FROM BORROW
    WHERE person_id = p_member_id AND return_date IS NULL;

    IF v_active_borrows >= v_quota THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR05_QUOTA_EXCEEDED] Thành viên đã đạt giới hạn số lượng sách mượn.';
    END IF;

    -- BR10: Kiểm tra copy tồn tại và sẵn sàng
    SELECT COUNT(*), MAX(availability_status)
    INTO v_copy_exists, v_copy_status
    FROM BOOK_COPY WHERE copy_id = p_copy_id;

    IF v_copy_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao sách không tồn tại.';
    END IF;

    IF v_copy_status != 'Available' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao sách hiện không sẵn sàng để mượn.';
    END IF;

    -- Thực hiện transaction
    SET v_borrow_date = CURDATE();
    SET p_due_date = DATE_ADD(v_borrow_date, INTERVAL v_loan_days DAY);

    START TRANSACTION;
        INSERT INTO BORROW (person_id, copy_id, borrow_date, due_date)
        VALUES (p_member_id, p_copy_id, v_borrow_date, p_due_date);
        SET p_borrow_id = LAST_INSERT_ID();

        UPDATE BOOK_COPY
        SET availability_status = 'Borrowed'
        WHERE copy_id = p_copy_id;
    COMMIT;
END//
DELIMITER ;
```

---

## Error Code Convention

| SIGNAL MESSAGE_TEXT prefix | Java Exception class | HTTP Response |
|---|---|---|
| `[NOT_FOUND]` | `ResourceNotFoundException` | 404 |
| `[BR01_INELIGIBLE_MEMBER]` | `IneligibleMemberException` | 403 |
| `[BR05_OVERDUE_PENDING]` | `OverdueBooksPendingException` | 409 |
| `[BR05_OUTSTANDING_FINE]` | `OutstandingFineException` | 409 |
| `[BR05_QUOTA_EXCEEDED]` | `BorrowQuotaExceededException` | 409 |
| `[BR10_BOOK_NOT_AVAILABLE]` | `BookNotAvailableException` | 409 |

---

## Deployment

```bash
mysql -u root -p library_db < Project/procedures.sql
```

Kiểm tra sau deploy:
```sql
USE library_db;
SHOW PROCEDURE STATUS WHERE Db = 'library_db' AND Name = 'sp_borrow_book';
```

---

## Test thủ công tại MySQL

```sql
-- Case 1: STUDENT hợp lệ mượn sách Available
SET @bid = 0; SET @due = NULL;
CALL sp_borrow_book(1, 'COPY001', @bid, @due);
SELECT @bid, @due;

-- Case 2: GUEST bị chặn (BR01)
CALL sp_borrow_book(3, 'COPY001', @bid, @due); -- Expect: [BR01_INELIGIBLE_MEMBER]

-- Case 3: Sách đã Borrowed (BR10)
CALL sp_borrow_book(1, 'COPY002', @bid, @due); -- (nếu COPY002 đang Borrowed)
```

---

## Điều kiện hoàn thành TASK này

- [ ] `sp_borrow_book` được thêm vào `procedures.sql`
- [ ] Procedure deploy thành công lên MySQL `library_db`
- [ ] Test thủ công qua MySQL client pass các case cơ bản
- [ ] TASK 06 cập nhật Java để gọi SP (xem file TASK_06)
