# TASK 05 — Stored Procedure: sp_access_digital_journal

## Mục tiêu
Chuyển toàn bộ logic nghiệp vụ của `DigitalJournalService.accessDigitalJournal()` vào MySQL Stored Procedure.  
Java chỉ còn gọi `CALL sp_access_digital_journal(?,?,?,?,?,?)` và đọc tham số OUT.

---

## Phạm vi

### File SQL cần tạo / cập nhật
- `Project/procedures.sql` — Thêm `sp_access_digital_journal` vào sau `sp_confirm_fine_payment`

### File Java cần chỉnh sửa (xem TASK 06)
- `Project/src/com/library/service/DigitalJournalService.java` — `accessDigitalJournal()`

---

## Logic nghiệp vụ hiện tại trong Java (cần chuyển sang SQL)

Từ `DigitalJournalService.accessDigitalJournal(Integer personId, int journalResourceId, String ipAddress, LocalDate accessDate)`:

```
Thứ tự kiểm tra chính thức (theo quyết định nghiệp vụ):

1. Kiểm tra journal tồn tại (resource_id và resource_type = 'DIGITAL_JOURNAL')
   → Nếu không: throw ResourceNotFoundException("[NOT_FOUND] ...")

2. INSERT ACCESS_LOG (person_id, resource_id, access_date, ip_address, access_result = 'PENDING')
   → Luôn ghi log trước, bất kể kết quả

3. IS_AUTHOR check: Nếu person_id có trong AUTHOR_OF cho resource_id này
   → Cho phép ngay. Cập nhật access_result = 'ALLOWED_AUTHOR'

4. IS_PUBLIC check: Nếu RESOURCE.is_public = TRUE
   → Cho phép. Cập nhật access_result = 'ALLOWED_PUBLIC'

5. License check (Resource private):
   → Kiểm tra RESOURCE_LICENSE có bản ghi không
   → Nếu không có: từ chối → access_result = 'DENIED_NO_LICENSE_DECLARED'
   → Nếu có: gọi fn_has_all_required_licenses(person_id, resource_id)
     - TRUE: cho phép → access_result = 'ALLOWED_LICENSE'
     - FALSE: từ chối → access_result = 'DENIED_MISSING_LICENSE'
     - SIGNAL: '[BR04_LICENSE_EXPIRED] ...'
```

> [!IMPORTANT]
> `fn_has_all_required_licenses` đã tồn tại trong DB. Gọi lại trong SP mới.
> SP này **luôn trả về** p_allowed và p_reason, không SIGNAL lỗi ngoại trừ NOT_FOUND.
> BR04 được thể hiện qua p_allowed=FALSE và p_reason chứa prefix '[BR04_LICENSE_EXPIRED]'.

---

## Cấu trúc bảng ACCESS_LOG (kiểm tra thực tế)

```sql
SHOW COLUMNS FROM ACCESS_LOG;
```

Dự kiến:
```
log_id         INT AUTO_INCREMENT PK
person_id      INT FK → PERSON (nullable nếu anonymous)
resource_id    INT FK → RESOURCE
access_date    DATE hoặc DATETIME
ip_address     VARCHAR
access_result  VARCHAR (kết quả: ALLOWED_AUTHOR / ALLOWED_PUBLIC / ALLOWED_LICENSE / DENIED_*)
```

---

## Stored Procedure cần viết

### Signature
```sql
CREATE PROCEDURE sp_access_digital_journal(
    IN  p_person_id    INT,          -- person_id (NULL nếu anonymous)
    IN  p_resource_id  INT,          -- resource_id của digital journal
    IN  p_ip_address   VARCHAR(45),  -- IP address của người truy cập
    OUT p_allowed      BOOLEAN,      -- TRUE nếu được phép
    OUT p_reason       VARCHAR(255), -- lý do
    OUT p_log_id       INT           -- log_id sau khi INSERT ACCESS_LOG
)
```

### Logic SQL

```sql
DELIMITER //
DROP PROCEDURE IF EXISTS sp_access_digital_journal//
CREATE PROCEDURE sp_access_digital_journal(
    IN  p_person_id   INT,
    IN  p_resource_id INT,
    IN  p_ip_address  VARCHAR(45),
    OUT p_allowed     BOOLEAN,
    OUT p_reason      VARCHAR(255),
    OUT p_log_id      INT
)
BEGIN
    DECLARE v_res_type    VARCHAR(30);
    DECLARE v_is_public   BOOLEAN DEFAULT FALSE;
    DECLARE v_is_author   INT DEFAULT 0;
    DECLARE v_req_count   INT DEFAULT 0;
    DECLARE v_has_all     BOOLEAN DEFAULT FALSE;
    DECLARE v_access_res  VARCHAR(50) DEFAULT 'DENIED';

    SET p_allowed = FALSE;
    SET p_reason = '';
    SET p_log_id = 0;

    -- 1. Kiểm tra resource tồn tại và là DIGITAL_JOURNAL
    SELECT resource_type, is_public
    INTO v_res_type, v_is_public
    FROM RESOURCE WHERE resource_id = p_resource_id;

    IF v_res_type IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy tài nguyên với ID đã cho.';
    END IF;

    IF v_res_type != 'DIGITAL_JOURNAL' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[NOT_FOUND] Tài nguyên này không phải là tài liệu số.';
    END IF;

    -- 2. INSERT ACCESS_LOG (luôn ghi, kể cả khi từ chối)
    INSERT INTO ACCESS_LOG (person_id, resource_id, access_date, ip_address, access_result)
    VALUES (p_person_id, p_resource_id, NOW(), p_ip_address, 'PENDING');
    SET p_log_id = LAST_INSERT_ID();

    -- 3. Kiểm tra đặc quyền tác giả (AUTHOR_OF)
    IF p_person_id IS NOT NULL AND p_person_id > 0 THEN
        SELECT COUNT(*) INTO v_is_author
        FROM AUTHOR_OF
        WHERE person_id = p_person_id AND resource_id = p_resource_id;
    END IF;

    IF v_is_author > 0 THEN
        SET p_allowed = TRUE;
        SET p_reason = 'Đặc quyền Tác giả (AUTHOR_OF).';
        SET v_access_res = 'ALLOWED_AUTHOR';

    -- 4. Kiểm tra is_public
    ELSEIF v_is_public = TRUE THEN
        SET p_allowed = TRUE;
        SET p_reason = 'Tài nguyên công khai (is_public = TRUE).';
        SET v_access_res = 'ALLOWED_PUBLIC';

    -- 5. Kiểm tra License (resource private)
    ELSE
        IF p_person_id IS NULL OR p_person_id <= 0 THEN
            SET p_allowed = FALSE;
            SET p_reason = '[BR04_LICENSE_EXPIRED] Tài liệu private yêu cầu định danh người dùng.';
            SET v_access_res = 'DENIED_NO_IDENTITY';
        ELSE
            SELECT COUNT(*) INTO v_req_count
            FROM RESOURCE_LICENSE WHERE resource_id = p_resource_id;

            IF v_req_count = 0 THEN
                SET p_allowed = FALSE;
                SET p_reason = 'Tài liệu private chưa khai báo License nào.';
                SET v_access_res = 'DENIED_NO_LICENSE_DECLARED';
            ELSE
                SET v_has_all = fn_has_all_required_licenses(p_person_id, p_resource_id);
                IF v_has_all = TRUE THEN
                    SET p_allowed = TRUE;
                    SET p_reason = 'Người dùng sở hữu đầy đủ tất cả License bắt buộc.';
                    SET v_access_res = 'ALLOWED_LICENSE';
                ELSE
                    SET p_allowed = FALSE;
                    SET p_reason = '[BR04_LICENSE_EXPIRED] Người dùng thiếu License bắt buộc.';
                    SET v_access_res = 'DENIED_MISSING_LICENSE';
                END IF;
            END IF;
        END IF;
    END IF;

    -- 6. Cập nhật kết quả log
    UPDATE ACCESS_LOG
    SET access_result = v_access_res
    WHERE log_id = p_log_id;
END//
DELIMITER ;
```

---

## Error Code Convention

| SIGNAL / p_reason prefix | Ý nghĩa | Java xử lý |
|---|---|---|
| `SIGNAL '[NOT_FOUND]'` | Resource không tồn tại | throw `ResourceNotFoundException` |
| `p_reason '[BR04_LICENSE_EXPIRED]'` | Thiếu hoặc không có License | throw `LicenseExpiredException` hoặc trả 403 |
| `p_allowed = FALSE` không có prefix | Từ chối bình thường | trả message cho UI |

> [!NOTE]
> `sp_access_digital_journal` không SIGNAL trừ NOT_FOUND. Các từ chối khác được thể hiện qua p_allowed = FALSE.
> Java đọc p_allowed: nếu FALSE và p_reason bắt đầu bằng `[BR04_...]` thì throw `LicenseExpiredException`.

---

## Deployment

```bash
mysql -u root -p library_db < Project/procedures.sql
```

---

## Test thủ công tại MySQL

```sql
USE library_db;

-- Case 1: Tác giả truy cập tài liệu của chính họ
SET @allowed = FALSE; SET @reason = ''; SET @log = 0;
CALL sp_access_digital_journal(1, 1, '127.0.0.1', @allowed, @reason, @log);
SELECT @allowed, @reason, @log; -- Expect: 1, 'Đặc quyền Tác giả (AUTHOR_OF).', log_id > 0

-- Case 2: Truy cập tài liệu public
CALL sp_access_digital_journal(2, 2, '127.0.0.1', @allowed, @reason, @log);
SELECT @allowed, @reason, @log; -- Expect: 1, 'Tài nguyên công khai...'

-- Case 3: Truy cập tài liệu private, thiếu License
CALL sp_access_digital_journal(3, 3, '127.0.0.1', @allowed, @reason, @log);
SELECT @allowed, @reason, @log; -- Expect: 0, '[BR04_LICENSE_EXPIRED]...'

-- Case 4: Resource không tồn tại
CALL sp_access_digital_journal(1, 9999, '127.0.0.1', @allowed, @reason, @log);
-- Expect: SIGNAL '[NOT_FOUND]'

-- Kiểm tra ACCESS_LOG
SELECT * FROM ACCESS_LOG ORDER BY log_id DESC LIMIT 5;
```

---

## Điều kiện hoàn thành TASK này

- [ ] `sp_access_digital_journal` được thêm vào `procedures.sql`
- [ ] Procedure deploy thành công lên MySQL `library_db`
- [ ] ACCESS_LOG luôn được ghi, kể cả trường hợp từ chối
- [ ] Test thủ công qua MySQL client pass các case cơ bản
- [ ] TASK 06 cập nhật Java để gọi SP (xem file TASK_06)
