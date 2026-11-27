-- =============================================================================
-- SQL REFERENCE: STORED PROCEDURES
-- Source of Truth: Current Java + JDBC Service Layer
-- Note: Reference-only script. Do NOT load into application runtime.
-- Target Schema: Project/01_create_database_final_v2.sql
-- Dependencies: SQL_Reference/02_functions_reference.sql
-- =============================================================================

DELIMITER //

-- =============================================================================
-- 1. sp_borrow_book
-- Java Source: BorrowService.borrowBook(String memberId, String copyId)
-- Transaction: Self-managed (START TRANSACTION / COMMIT / EXIT HANDLER ROLLBACK)
-- Concurrency: Locks UNIVERSITY_MEMBER (FOR UPDATE) and BOOK_COPY (FOR UPDATE).
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_borrow_book//
CREATE PROCEDURE sp_borrow_book(
    IN  p_member_id  VARCHAR(20),
    IN  p_copy_id    VARCHAR(30),
    OUT p_borrow_id  INT,
    OUT p_due_date   DATE
)
BEGIN
    DECLARE v_clean_member_id VARCHAR(20);
    DECLARE v_clean_copy_id   VARCHAR(30);
    DECLARE v_person_id       INT DEFAULT NULL;
    DECLARE v_member_type     VARCHAR(20) DEFAULT NULL;
    DECLARE v_quota           INT DEFAULT 0;
    DECLARE v_loan_days       INT DEFAULT 14;
    DECLARE v_overdue_count   INT DEFAULT 0;
    DECLARE v_unpaid_count    INT DEFAULT 0;
    DECLARE v_active_count    INT DEFAULT 0;
    DECLARE v_resource_id     INT DEFAULT NULL;
    DECLARE v_copy_status     VARCHAR(20) DEFAULT NULL;
    DECLARE v_is_public       BOOLEAN DEFAULT FALSE;
    DECLARE v_active_copy     INT DEFAULT 0;
    DECLARE v_is_author       INT DEFAULT 0;
    DECLARE v_required_count  INT DEFAULT 0;
    DECLARE v_borrow_date     DATE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_borrow_id = 0;
    SET p_due_date = NULL;

    IF p_member_id IS NULL OR TRIM(p_member_id) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Member ID không được để trống hoặc mang giá trị null.';
    END IF;
    IF p_copy_id IS NULL OR TRIM(p_copy_id) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Copy ID không được để trống hoặc mang giá trị null.';
    END IF;

    SET v_clean_member_id = TRIM(p_member_id);
    SET v_clean_copy_id = TRIM(p_copy_id);

    START TRANSACTION;

        SELECT person_id, member_type
          INTO v_person_id, v_member_type
        FROM UNIVERSITY_MEMBER
        WHERE member_id = v_clean_member_id
        FOR UPDATE;

        IF v_person_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR01_INELIGIBLE_MEMBER] Chỉ UNIVERSITY_MEMBER mới được mượn sách.';
        END IF;

        SET v_quota = fn_get_borrow_quota(v_member_type);
        SET v_loan_days = fn_get_loan_duration_days(v_member_type);

        SELECT COUNT(*) INTO v_overdue_count
        FROM BORROW
        WHERE member_id = v_clean_member_id
          AND return_date IS NULL
          AND due_date < CURDATE();

        IF v_overdue_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_OVERDUE_PENDING] Thành viên đang có sách mượn quá hạn chưa trả.';
        END IF;

        SELECT COUNT(*) INTO v_unpaid_count
        FROM BORROW b
        INNER JOIN FINE f ON f.borrow_id = b.borrow_id
        WHERE b.member_id = v_clean_member_id
          AND f.payment_status = 'Unpaid';

        IF v_unpaid_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_OUTSTANDING_FINE] Thành viên có phiếu phạt chưa thanh toán.';
        END IF;

        SELECT COUNT(*) INTO v_active_count
        FROM BORROW
        WHERE member_id = v_clean_member_id
          AND return_date IS NULL;

        IF v_active_count >= v_quota THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_QUOTA_EXCEEDED] Thành viên đã đạt giới hạn số lượng sách mượn.';
        END IF;

        SELECT bc.resource_id, bc.availability_status, r.is_public
          INTO v_resource_id, v_copy_status, v_is_public
        FROM BOOK_COPY bc
        INNER JOIN PHYSICAL_BOOK pb ON pb.resource_id = bc.resource_id
        INNER JOIN RESOURCE r ON r.resource_id = pb.resource_id
        WHERE bc.copy_id = v_clean_copy_id
        FOR UPDATE;

        IF v_resource_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao sách không tồn tại hoặc không phải sách vật lý.';
        END IF;

        IF v_copy_status <> 'Available' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao sách hiện không sẵn sàng để mượn.';
        END IF;

        SELECT COUNT(*) INTO v_active_copy
        FROM BORROW
        WHERE copy_id = v_clean_copy_id
          AND return_date IS NULL;

        IF v_active_copy > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao đang có giao dịch mượn chưa trả.';
        END IF;

        SELECT COUNT(*) INTO v_is_author
        FROM AUTHOR_OF
        WHERE person_id = v_person_id
          AND resource_id = v_resource_id;

        IF v_is_public = FALSE AND v_is_author = 0 THEN
            SELECT COUNT(*) INTO v_required_count
            FROM RESOURCE_LICENSE
            WHERE resource_id = v_resource_id;

            IF v_required_count = 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = '[BR04_LICENSE_EXPIRED] Sách private chưa khai báo License yêu cầu.';
            END IF;

            IF NOT fn_has_all_required_licenses(v_person_id, v_resource_id) THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = '[BR04_LICENSE_EXPIRED] Thành viên chưa sở hữu đầy đủ License bắt buộc cho sách.';
            END IF;
        END IF;

        SET v_borrow_date = CURDATE();
        SET p_due_date = DATE_ADD(v_borrow_date, INTERVAL v_loan_days DAY);

        INSERT INTO BORROW (member_id, copy_id, borrow_date, due_date)
        VALUES (v_clean_member_id, v_clean_copy_id, v_borrow_date, p_due_date);
        SET p_borrow_id = LAST_INSERT_ID();

        UPDATE BOOK_COPY
        SET availability_status = 'Borrowed'
        WHERE copy_id = v_clean_copy_id;

    COMMIT;
END//

-- =============================================================================
-- 2. sp_return_book
-- Java Source: BorrowService.returnBookWithResult(int borrowId, LocalDate returnDate)
-- Transaction: Self-managed. Locks BORROW, BOOK_COPY, and latest Unpaid FINE (FOR UPDATE).
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_return_book//
CREATE PROCEDURE sp_return_book(
    IN  p_borrow_id   INT,
    IN  p_return_date DATE,
    OUT p_fine_id     INT,
    OUT p_fine_amount DECIMAL(12,2)
)
BEGIN
    DECLARE v_copy_id               VARCHAR(30) DEFAULT NULL;
    DECLARE v_locked_copy_id        VARCHAR(30) DEFAULT NULL;
    DECLARE v_borrow_date           DATE DEFAULT NULL;
    DECLARE v_due_date              DATE DEFAULT NULL;
    DECLARE v_saved_return_date     DATE DEFAULT NULL;
    DECLARE v_actual_return_date    DATE DEFAULT NULL;
    DECLARE v_existing_fine_id      INT DEFAULT NULL;
    DECLARE v_existing_fine_amount  DECIMAL(10,2) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_fine_id = 0;
    SET p_fine_amount = 0.00;

    IF p_borrow_id IS NULL OR p_borrow_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Borrow ID phải là số nguyên dương lớn hơn 0.';
    END IF;

    START TRANSACTION;

        SELECT copy_id, borrow_date, due_date, return_date
          INTO v_copy_id, v_borrow_date, v_due_date, v_saved_return_date
        FROM BORROW
        WHERE borrow_id = p_borrow_id
        FOR UPDATE;

        IF v_copy_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy phiếu mượn với ID đã cho.';
        END IF;

        IF v_saved_return_date IS NOT NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR02_ALREADY_RETURNED] Sách thuộc lượt mượn này đã được trả trước đó.';
        END IF;

        SET v_actual_return_date = COALESCE(p_return_date, CURDATE());
        IF v_actual_return_date < v_borrow_date THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[INVALID_DATE_RANGE] Ngày trả không thể trước ngày mượn.';
        END IF;

        SELECT copy_id INTO v_locked_copy_id
        FROM BOOK_COPY
        WHERE copy_id = v_copy_id
        FOR UPDATE;

        IF v_locked_copy_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy bản sao sách của giao dịch mượn.';
        END IF;

        UPDATE BORROW
        SET return_date = v_actual_return_date
        WHERE borrow_id = p_borrow_id;

        UPDATE BOOK_COPY
        SET availability_status = 'Available'
        WHERE copy_id = v_copy_id;

        IF v_actual_return_date > v_due_date THEN
            SELECT fine_id, total_fine_amount
              INTO v_existing_fine_id, v_existing_fine_amount
            FROM FINE
            WHERE borrow_id = p_borrow_id
              AND payment_status = 'Unpaid'
            ORDER BY fine_id DESC
            LIMIT 1
            FOR UPDATE;

            IF v_existing_fine_id IS NOT NULL THEN
                UPDATE FINE
                SET total_fine_amount = total_fine_amount + 50000.00
                WHERE fine_id = v_existing_fine_id;

                SET p_fine_id = v_existing_fine_id;
                SET p_fine_amount = v_existing_fine_amount + 50000.00;
            ELSE
                INSERT INTO FINE (borrow_id, total_fine_amount, payment_status)
                VALUES (p_borrow_id, 50000.00, 'Unpaid');

                SET p_fine_id = LAST_INSERT_ID();
                SET p_fine_amount = 50000.00;
            END IF;
        END IF;

    COMMIT;
END//

-- =============================================================================
-- 3. sp_confirm_fine_payment
-- Java Source: FineService.confirmPayment(int fineId)
-- Transaction: Self-managed. Locks FINE row (FOR UPDATE) to prevent double payment.
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_confirm_fine_payment//
CREATE PROCEDURE sp_confirm_fine_payment(
    IN  p_fine_id        INT,
    OUT p_transaction_id INT
)
BEGIN
    DECLARE v_payment_status VARCHAR(30) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_transaction_id = 0;

    IF p_fine_id IS NULL OR p_fine_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Fine ID phải là số nguyên dương lớn hơn 0.';
    END IF;

    START TRANSACTION;

        SELECT payment_status INTO v_payment_status
        FROM FINE
        WHERE fine_id = p_fine_id
        FOR UPDATE;

        IF v_payment_status IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy phiếu phạt với ID đã cho.';
        END IF;

        IF UPPER(v_payment_status) = 'COMPLETED' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR11_FINE_ALREADY_SETTLED] Phiếu phạt này đã được thanh toán hoàn tất.';
        END IF;

        INSERT INTO PAYMENT_TRANSACTION (fine_id, payment_date)
        VALUES (p_fine_id, CURRENT_TIMESTAMP);
        SET p_transaction_id = LAST_INSERT_ID();

        UPDATE FINE
        SET payment_status = 'Completed'
        WHERE fine_id = p_fine_id;

    COMMIT;
END//

-- =============================================================================
-- 4. sp_access_digital_journal
-- Java Source: DigitalJournalService.accessDigitalJournalWithResult(Integer personId, int journalResourceId)
-- Transaction: Self-managed. Evaluates Guest / PERSON / Author / License policy
-- and inserts ACCESS_LOG with synthetic TEST-NET IP only when allowed = TRUE.
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_access_digital_journal//
CREATE PROCEDURE sp_access_digital_journal(
    IN  p_person_id   INT,
    IN  p_resource_id INT,
    OUT p_allowed     BOOLEAN,
    OUT p_reason      VARCHAR(255),
    OUT p_log_id      BIGINT
)
BEGIN
    DECLARE v_is_public      BOOLEAN DEFAULT NULL;
    DECLARE v_person_count   INT DEFAULT 0;
    DECLARE v_is_author      INT DEFAULT 0;
    DECLARE v_required_count INT DEFAULT 0;
    DECLARE v_ip_block       INT DEFAULT 0;
    DECLARE v_ip_host        INT DEFAULT 0;
    DECLARE v_generated_ip   VARCHAR(15) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_allowed = FALSE;
    SET p_reason = '';
    SET p_log_id = 0;

    IF p_resource_id IS NULL OR p_resource_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Journal Resource ID phải là số nguyên dương lớn hơn 0.';
    END IF;

    START TRANSACTION;

        SELECT r.is_public
          INTO v_is_public
        FROM DIGITAL_JOURNAL dj
        INNER JOIN RESOURCE r ON r.resource_id = dj.resource_id
        WHERE r.resource_id = p_resource_id;

        IF v_is_public IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy DIGITAL_JOURNAL với resource_id đã cho.';
        END IF;

        IF p_person_id IS NULL THEN
            IF v_is_public = TRUE THEN
                SET p_allowed = TRUE;
                SET p_reason = 'Guest không định danh được truy cập journal công khai.';
            ELSE
                SET p_reason = 'Guest không định danh chỉ được truy cập journal công khai.';
            END IF;
        ELSE
            SELECT COUNT(*) INTO v_person_count
            FROM PERSON
            WHERE person_id = p_person_id;

            IF v_person_count = 0 THEN
                SET p_reason = 'PERSON ID không tồn tại.';
            ELSE
                SELECT COUNT(*) INTO v_is_author
                FROM AUTHOR_OF
                WHERE person_id = p_person_id
                  AND resource_id = p_resource_id;

                IF v_is_author > 0 THEN
                    SET p_allowed = TRUE;
                    SET p_reason = 'PERSON là tác giả của journal; bỏ qua kiểm tra License.';
                ELSE
                    SELECT COUNT(*) INTO v_required_count
                    FROM RESOURCE_LICENSE
                    WHERE resource_id = p_resource_id;

                    IF v_required_count = 0 THEN
                        SET p_reason = 'Journal không phải do PERSON này sáng tác và chưa khai báo License yêu cầu.';
                    ELSEIF fn_has_all_required_licenses(p_person_id, p_resource_id) THEN
                        SET p_allowed = TRUE;
                        SET p_reason = 'PERSON không phải tác giả nhưng có đầy đủ License bắt buộc.';
                    ELSE
                        SET p_reason = 'PERSON không phải tác giả và chưa có đầy đủ License bắt buộc.';
                    END IF;
                END IF;
            END IF;
        END IF;

        IF p_allowed = TRUE THEN
            SET v_ip_block = FLOOR(RAND() * 3);
            SET v_ip_host = FLOOR(RAND() * 254) + 1;
            SET v_generated_ip = CASE v_ip_block
                WHEN 0 THEN CONCAT('192.0.2.', v_ip_host)
                WHEN 1 THEN CONCAT('198.51.100.', v_ip_host)
                ELSE CONCAT('203.0.113.', v_ip_host)
            END;

            INSERT INTO ACCESS_LOG (person_id, journal_resource_id, ip_address)
            VALUES (p_person_id, p_resource_id, v_generated_ip);
            SET p_log_id = LAST_INSERT_ID();
        END IF;

    COMMIT;
END//

-- =============================================================================
-- 5. sp_grant_license_manual
-- Java Source: DigitalJournalService.grantLicenseManual(int personId, int licenseId)
-- Transaction: Self-managed. Validates Person, LicensePackage, and duplicate grant.
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_grant_license_manual//
CREATE PROCEDURE sp_grant_license_manual(
    IN  p_person_id  INT,
    IN  p_license_id INT,
    OUT p_grant_id   INT
)
BEGIN
    DECLARE v_person_exists  INT DEFAULT 0;
    DECLARE v_license_exists INT DEFAULT 0;
    DECLARE v_dup_exists     INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR 1062
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Người dùng đã sở hữu gói License này.';
    END;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_grant_id = 0;

    IF p_person_id IS NULL OR p_person_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Person ID phải là số nguyên dương lớn hơn 0.';
    END IF;
    IF p_license_id IS NULL OR p_license_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] License ID phải là số nguyên dương lớn hơn 0.';
    END IF;

    START TRANSACTION;

        SELECT COUNT(*) INTO v_person_exists
        FROM PERSON
        WHERE person_id = p_person_id;

        IF v_person_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Độc giả/Người dùng không tồn tại.';
        END IF;

        SELECT COUNT(*) INTO v_license_exists
        FROM LICENSE_PACKAGE
        WHERE license_id = p_license_id;

        IF v_license_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Gói bản quyền (LicensePackage) không tồn tại.';
        END IF;

        SELECT COUNT(*) INTO v_dup_exists
        FROM GRANTED_LICENSE
        WHERE person_id = p_person_id
          AND license_id = p_license_id;

        IF v_dup_exists > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[INVALID_INPUT] Người dùng đã sở hữu gói License này.';
        END IF;

        INSERT INTO GRANTED_LICENSE (person_id, license_id)
        VALUES (p_person_id, p_license_id);
        SET p_grant_id = LAST_INSERT_ID();

    COMMIT;
END//

-- =============================================================================
-- 6. sp_revoke_license_manual
-- Java Source: DigitalJournalService.revokeLicenseManual(int personId, int licenseId)
-- Transaction: Self-managed.
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_revoke_license_manual//
CREATE PROCEDURE sp_revoke_license_manual(
    IN  p_person_id  INT,
    IN  p_license_id INT,
    OUT p_deleted    INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_deleted = 0;

    IF p_person_id IS NULL OR p_person_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Person ID phải là số nguyên dương lớn hơn 0.';
    END IF;
    IF p_license_id IS NULL OR p_license_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] License ID phải là số nguyên dương lớn hơn 0.';
    END IF;

    START TRANSACTION;

        DELETE FROM GRANTED_LICENSE
        WHERE person_id = p_person_id
          AND license_id = p_license_id;
        SET p_deleted = ROW_COUNT();

        IF p_deleted = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy quyền cấp License tương ứng để thu hồi.';
        END IF;

    COMMIT;
END//

-- =============================================================================
-- 7. sp_revoke_license_by_grant_id
-- Java Source: DigitalJournalService.revokeLicenseByGrantId(int grantId)
-- Transaction: Self-managed.
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_revoke_license_by_grant_id//
CREATE PROCEDURE sp_revoke_license_by_grant_id(
    IN  p_grant_id INT,
    OUT p_deleted  INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_deleted = 0;

    IF p_grant_id IS NULL OR p_grant_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[INVALID_INPUT] Grant ID phải là số nguyên dương lớn hơn 0.';
    END IF;

    START TRANSACTION;

        DELETE FROM GRANTED_LICENSE
        WHERE grant_id = p_grant_id;
        SET p_deleted = ROW_COUNT();

        IF p_deleted = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Bản ghi cấp License (GRANTED_LICENSE) không tồn tại.';
        END IF;

    COMMIT;
END//

DELIMITER ;
