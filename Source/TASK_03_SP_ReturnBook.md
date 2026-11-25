# TASK 03 — Stored Procedure: sp_return_book

## Mục tiêu
Chuyển toàn bộ logic nghiệp vụ của `BorrowService.returnBook()` vào MySQL Stored Procedure.  
Java chỉ còn gọi `CALL sp_return_book(?,?,?,?)` và đọc tham số OUT.

---

## Phạm vi

### File SQL cần tạo / cập nhật
- `Project/procedures.sql` — Thêm `sp_return_book` vào sau `sp_borrow_book`

### File Java cần chỉnh sửa (xem TASK 06)
- `Project/src/com/library/service/BorrowService.java` — `returnBook()`

---

## Logic nghiệp vụ hiện tại trong Java (cần chuyển sang SQL)

Từ `BorrowService.returnBook(int borrowId, LocalDate actualReturnDate)`:

```
1. Kiểm tra borrow tồn tại
   → Nếu không: throw ResourceNotFoundException("[NOT_FOUND] ...")

2. [BR02] Kiểm tra chưa trả (return_date IS NULL)
   → Nếu đã trả: throw BookAlreadyReturnedException("[BR02_ALREADY_RETURNED] ...")

3. UPDATE BORROW SET return_date = actualReturnDate WHERE borrow_id = ?

4. UPDATE BOOK_COPY SET availability_status = 'Available' WHERE copy_id = ?

5. [FR05] Nếu actualReturnDate > due_date:
   → Kiểm tra xem đã có FINE cho borrow này chưa (tránh trùng)
   → Nếu chưa: INSERT INTO FINE (borrow_id, total_fine_amount, payment_status)
                VALUES (borrowId, 50000.00, 'Unpaid')

6. Toàn bộ trong 1 transaction (COMMIT hoặc ROLLBACK)
```

**Hằng số quan trọng:**
- `OVERDUE_PENALTY_PER_BORROW = 50000.0` VND (cố định, không tính theo ngày)

---

## Stored Procedure cần viết

### Signature
```sql
CREATE PROCEDURE sp_return_book(
    IN  p_borrow_id    INT,       -- borrow_id cần trả
    IN  p_return_date  DATE,      -- ngày trả thực tế (NULL = CURDATE())
    OUT p_fine_id      INT,       -- fine_id nếu sinh phiếu phạt (0 nếu không có)
    OUT p_fine_amount  DECIMAL(12,2) -- số tiền phạt (0 nếu không có)
)
```

### Logic SQL

```sql
DELIMITER //
DROP PROCEDURE IF EXISTS sp_return_book//
CREATE PROCEDURE sp_return_book(
    IN  p_borrow_id   INT,
    IN  p_return_date DATE,
    OUT p_fine_id     INT,
    OUT p_fine_amount DECIMAL(12,2)
)
BEGIN
    DECLARE v_copy_id      VARCHAR(50);
    DECLARE v_due_date     DATE;
    DECLARE v_ret_date     DATE;
    DECLARE v_already_ret  INT DEFAULT 0;
    DECLARE v_fine_exists  INT DEFAULT 0;

    SET p_fine_id = 0;
    SET p_fine_amount = 0.00;

    -- Kiểm tra borrow tồn tại
    SELECT copy_id, due_date, return_date
    INTO v_copy_id, v_due_date, v_ret_date
    FROM BORROW WHERE borrow_id = p_borrow_id;

    IF v_copy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy phiếu mượn với ID đã cho.';
    END IF;

    -- BR02: Kiểm tra chưa trả
    IF v_ret_date IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR02_ALREADY_RETURNED] Sách này đã được trả trước đó.';
    END IF;

    -- Xác định ngày trả thực tế
    IF p_return_date IS NULL THEN
        SET p_return_date = CURDATE();
    END IF;

    START TRANSACTION;
        -- Cập nhật borrow
        UPDATE BORROW
        SET return_date = p_return_date
        WHERE borrow_id = p_borrow_id;

        -- Trả lại bản sao sách
        UPDATE BOOK_COPY
        SET availability_status = 'Available'
        WHERE copy_id = v_copy_id;

        -- FR05: Sinh phiếu phạt nếu trả quá hạn
        IF v_due_date IS NOT NULL AND p_return_date > v_due_date THEN
            -- Kiểm tra tránh trùng lặp phiếu phạt
            SELECT COUNT(*) INTO v_fine_exists
            FROM FINE WHERE borrow_id = p_borrow_id;

            IF v_fine_exists = 0 THEN
                INSERT INTO FINE (borrow_id, total_fine_amount, payment_status)
                VALUES (p_borrow_id, 50000.00, 'Unpaid');
                SET p_fine_id = LAST_INSERT_ID();
                SET p_fine_amount = 50000.00;
            END IF;
        END IF;
    COMMIT;
END//
DELIMITER ;
```

---

## Error Code Convention

| SIGNAL MESSAGE_TEXT prefix | Java Exception class | HTTP Response |
|---|---|---|
| `[NOT_FOUND]` | `ResourceNotFoundException` | 404 |
| `[BR02_ALREADY_RETURNED]` | `BookAlreadyReturnedException` | 409 |

---

## Lưu ý về ngày trả

- Java hiện truyền `LocalDate.now()` nếu không có ngày cụ thể.
- SP nhận `DATE` (YYYY-MM-DD).
- Java cần convert: `java.sql.Date.valueOf(actualReturnDate)` khi set parameter.

---

## Deployment

```bash
mysql -u root -p library_db < Project/procedures.sql
```

---

## Test thủ công tại MySQL

```sql
USE library_db;

-- Case 1: Trả sách đúng hạn (không phạt)
SET @fine_id = 0; SET @fine_amt = 0;
CALL sp_return_book(1, '2026-11-20', @fine_id, @fine_amt);
SELECT @fine_id, @fine_amt; -- Expect: 0, 0.00

-- Case 2: Trả sách quá hạn (sinh phiếu phạt 50000)
CALL sp_return_book(2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), @fine_id, @fine_amt);
SELECT @fine_id, @fine_amt; -- Expect: fine_id > 0, 50000.00

-- Case 3: Trả lại borrow đã trả (BR02)
CALL sp_return_book(1, CURDATE(), @fine_id, @fine_amt); -- Expect: [BR02_ALREADY_RETURNED]

-- Case 4: Borrow không tồn tại
CALL sp_return_book(9999, CURDATE(), @fine_id, @fine_amt); -- Expect: [NOT_FOUND]
```

---

## Điều kiện hoàn thành TASK này

- [ ] `sp_return_book` được thêm vào `procedures.sql`
- [ ] Procedure deploy thành công lên MySQL `library_db`
- [ ] Test thủ công qua MySQL client pass các case cơ bản
- [ ] TASK 06 cập nhật Java để gọi SP (xem file TASK_06)
