# TASK 04 — Stored Procedure: sp_confirm_fine_payment

## Mục tiêu
Chuyển toàn bộ logic nghiệp vụ của `FineService.confirmPayment()` vào MySQL Stored Procedure.  
Java chỉ còn gọi `CALL sp_confirm_fine_payment(?,?)` và đọc tham số OUT.

---

## Phạm vi

### File SQL cần tạo / cập nhật
- `Project/procedures.sql` — Thêm `sp_confirm_fine_payment` vào sau `sp_return_book`

### File Java cần chỉnh sửa (xem TASK 06)
- `Project/src/com/library/service/FineService.java` — `confirmPayment()`

---

## Logic nghiệp vụ hiện tại trong Java (cần chuyển sang SQL)

Từ `FineService.confirmPayment(int fineId)`:

```
1. Kiểm tra fine tồn tại
   → Nếu không: throw ResourceNotFoundException("[NOT_FOUND] ...")

2. [BR11] Kiểm tra payment_status != 'Completed'
   → Nếu đã Completed: throw FineAlreadySettledException("[BR11_FINE_ALREADY_SETTLED] ...")

3. INSERT INTO PAYMENT_TRANSACTION (fine_id, payment_date)
   VALUES (fineId, NOW())

4. UPDATE FINE SET payment_status = 'Completed' WHERE fine_id = ?

5. Toàn bộ trong 1 transaction (COMMIT hoặc ROLLBACK)
```

---

## Cấu trúc bảng PAYMENT_TRANSACTION (kiểm tra thực tế)

```sql
SHOW COLUMNS FROM PAYMENT_TRANSACTION;
```

Dự kiến (kiểm tra trước khi viết SP):
```
transaction_id  INT AUTO_INCREMENT PK
fine_id         INT FK → FINE.fine_id
payment_date    DATETIME
```

---

## Stored Procedure cần viết

### Signature
```sql
CREATE PROCEDURE sp_confirm_fine_payment(
    IN  p_fine_id        INT,   -- fine_id cần xác nhận thanh toán
    OUT p_transaction_id INT    -- transaction_id sau khi INSERT thành công
)
```

### Logic SQL

```sql
DELIMITER //
DROP PROCEDURE IF EXISTS sp_confirm_fine_payment//
CREATE PROCEDURE sp_confirm_fine_payment(
    IN  p_fine_id        INT,
    OUT p_transaction_id INT
)
BEGIN
    DECLARE v_payment_status VARCHAR(30);
    DECLARE v_fine_exists    INT DEFAULT 0;

    SET p_transaction_id = 0;

    -- Kiểm tra fine tồn tại
    SELECT payment_status INTO v_payment_status
    FROM FINE WHERE fine_id = p_fine_id;

    IF v_payment_status IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy phiếu phạt với ID đã cho.';
    END IF;

    -- BR11: Kiểm tra chưa thanh toán
    IF v_payment_status = 'Completed' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[BR11_FINE_ALREADY_SETTLED] Phiếu phạt này đã được thanh toán trước đó.';
    END IF;

    START TRANSACTION;
        -- Ghi nhận giao dịch thanh toán
        INSERT INTO PAYMENT_TRANSACTION (fine_id, payment_date)
        VALUES (p_fine_id, NOW());
        SET p_transaction_id = LAST_INSERT_ID();

        -- Cập nhật trạng thái phiếu phạt
        UPDATE FINE
        SET payment_status = 'Completed'
        WHERE fine_id = p_fine_id;
    COMMIT;
END//
DELIMITER ;
```

---

## Error Code Convention

| SIGNAL MESSAGE_TEXT prefix | Java Exception class | HTTP Response |
|---|---|---|
| `[NOT_FOUND]` | `ResourceNotFoundException` | 404 |
| `[BR11_FINE_ALREADY_SETTLED]` | `FineAlreadySettledException` | 409 |

---

## Deployment

```bash
mysql -u root -p library_db < Project/procedures.sql
```

---

## Test thủ công tại MySQL

```sql
USE library_db;

-- Case 1: Xác nhận thanh toán phiếu phạt Unpaid
SET @txn_id = 0;
CALL sp_confirm_fine_payment(1, @txn_id);
SELECT @txn_id; -- Expect: transaction_id > 0
SELECT payment_status FROM FINE WHERE fine_id = 1; -- Expect: Completed

-- Case 2: Xác nhận lại phiếu đã Completed (BR11)
CALL sp_confirm_fine_payment(1, @txn_id); -- Expect: [BR11_FINE_ALREADY_SETTLED]

-- Case 3: Fine không tồn tại
CALL sp_confirm_fine_payment(9999, @txn_id); -- Expect: [NOT_FOUND]
```

---

## Điều kiện hoàn thành TASK này

- [ ] `sp_confirm_fine_payment` được thêm vào `procedures.sql`
- [ ] Procedure deploy thành công lên MySQL `library_db`
- [ ] Test thủ công qua MySQL client pass các case cơ bản
- [ ] TASK 06 cập nhật Java để gọi SP (xem file TASK_06)
