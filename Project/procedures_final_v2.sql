-- =============================================================================
-- STORED PROCEDURES - Library Management System & Open Repository
-- Target schema: 01_create_database_final_v2.sql
-- =============================================================================
USE library_db;
DELIMITER //

-- =============================================================================
-- 1. sp_check_resource_access
-- Preflight helper. p_person_id is PERSON.person_id, not member_id.
-- BORROW_BOOK requires a UNIVERSITY_MEMBER whose quotas and restrictions pass.
-- READ_DIGITAL: no ID => public-only Guest; ID => Author bypass or License check.
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_check_resource_access//
CREATE PROCEDURE sp_check_resource_access(
    IN  p_person_id   INT,
    IN  p_resource_id INT,
    IN  p_action      VARCHAR(20),
    OUT p_allowed     BOOLEAN,
    OUT p_reason      VARCHAR(255)
)
BEGIN
    DECLARE v_action          VARCHAR(20) DEFAULT '';
    DECLARE v_is_public       BOOLEAN DEFAULT FALSE;
    DECLARE v_member_id       VARCHAR(20) DEFAULT NULL;
    DECLARE v_member_type     VARCHAR(20) DEFAULT NULL;
    DECLARE v_person_exists   INT DEFAULT 0;
    DECLARE v_is_author       INT DEFAULT 0;
    DECLARE v_required_count  INT DEFAULT 0;
    DECLARE v_active_count    INT DEFAULT 0;
    DECLARE v_overdue_count   INT DEFAULT 0;
    DECLARE v_unpaid_count    INT DEFAULT 0;
    DECLARE v_quota           INT DEFAULT 0;
    DECLARE v_has_all         BOOLEAN DEFAULT FALSE;

    SET p_allowed = FALSE;
    SET p_reason = '';
    SET v_action = UPPER(TRIM(COALESCE(p_action, '')));

    IF v_action NOT IN ('READ_DIGITAL', 'BORROW_BOOK') THEN
        SET p_reason = 'Hành động không hợp lệ. Chỉ chấp nhận READ_DIGITAL hoặc BORROW_BOOK.';
    ELSEIF v_action = 'BORROW_BOOK' THEN
        IF NOT EXISTS (
            SELECT 1
            FROM RESOURCE r
            INNER JOIN PHYSICAL_BOOK pb ON pb.resource_id = r.resource_id
            WHERE r.resource_id = p_resource_id
        ) THEN
            SET p_reason = 'Tài nguyên không tồn tại hoặc không phải PHYSICAL_BOOK.';
        ELSE
            SELECT um.member_id, um.member_type
              INTO v_member_id, v_member_type
            FROM UNIVERSITY_MEMBER um
            WHERE um.person_id = p_person_id;

            IF v_member_id IS NULL THEN
                SET p_reason = 'Chỉ UNIVERSITY_MEMBER được phép mượn sách.';
            ELSE
                SET v_quota = CASE WHEN v_member_type = 'FACULTY' THEN 10 ELSE 3 END;

                SELECT COUNT(*)
                  INTO v_overdue_count
                FROM BORROW
                WHERE member_id = v_member_id
                  AND return_date IS NULL
                  AND due_date < CURDATE();

                SELECT COUNT(*)
                  INTO v_unpaid_count
                FROM BORROW b
                INNER JOIN FINE f ON f.borrow_id = b.borrow_id
                WHERE b.member_id = v_member_id
                  AND f.payment_status = 'Unpaid';

                SELECT COUNT(*)
                  INTO v_active_count
                FROM BORROW
                WHERE member_id = v_member_id
                  AND return_date IS NULL;

                IF v_overdue_count > 0 THEN
                    SET p_reason = 'Thành viên đang có sách quá hạn chưa trả.';
                ELSEIF v_unpaid_count > 0 THEN
                    SET p_reason = 'Thành viên còn phiếu phạt chưa thanh toán.';
                ELSEIF v_active_count >= v_quota THEN
                    SET p_reason = 'Thành viên đã đạt quota mượn sách.';
                ELSE
                    SELECT is_public INTO v_is_public
                    FROM RESOURCE
                    WHERE resource_id = p_resource_id;

                    SELECT COUNT(*) INTO v_is_author
                    FROM AUTHOR_OF
                    WHERE person_id = p_person_id
                      AND resource_id = p_resource_id;

                    SELECT COUNT(*) INTO v_required_count
                    FROM RESOURCE_LICENSE
                    WHERE resource_id = p_resource_id;

                    IF v_is_public = TRUE OR v_is_author > 0 THEN
                        SET p_allowed = TRUE;
                        SET p_reason = IF(v_is_author > 0,
                            'Thành viên là tác giả của sách; bỏ qua kiểm tra License.',
                            'Sách công khai; không yêu cầu License.');
                    ELSEIF v_required_count = 0 THEN
                        SET p_reason = 'Sách private chưa khai báo License yêu cầu.';
                    ELSE
                        SET v_has_all = fn_has_all_required_licenses(p_person_id, p_resource_id);
                        IF v_has_all THEN
                            SET p_allowed = TRUE;
                            SET p_reason = 'Thành viên có đầy đủ License bắt buộc.';
                        ELSE
                            SET p_reason = 'Thành viên chưa có đầy đủ License bắt buộc.';
                        END IF;
                    END IF;
                END IF;
            END IF;
        END IF;
    ELSE
        IF NOT EXISTS (
            SELECT 1
            FROM DIGITAL_JOURNAL
            WHERE resource_id = p_resource_id
        ) THEN
            SET p_reason = 'Tài nguyên không tồn tại hoặc không phải DIGITAL_JOURNAL.';
        ELSE
            SELECT is_public INTO v_is_public
            FROM RESOURCE
            WHERE resource_id = p_resource_id;

            IF p_person_id IS NULL THEN
                -- No ID supplied: anonymous Guest access depends only on is_public.
                IF v_is_public = TRUE THEN
                    SET p_allowed = TRUE;
                    SET p_reason = 'Guest không định danh được truy cập journal công khai.';
                ELSE
                    SET p_reason = 'Guest không định danh chỉ được truy cập journal công khai.';
                END IF;
            ELSE
                -- ID supplied: it must identify an existing PERSON.
                SELECT COUNT(*) INTO v_person_exists
                FROM PERSON
                WHERE person_id = p_person_id;

                IF v_person_exists = 0 THEN
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
                        ELSE
                            SET v_has_all = fn_has_all_required_licenses(p_person_id, p_resource_id);
                            IF v_has_all THEN
                                SET p_allowed = TRUE;
                                SET p_reason = 'PERSON không phải tác giả nhưng có đầy đủ License bắt buộc.';
                            ELSE
                                SET p_reason = 'PERSON không phải tác giả và chưa có đầy đủ License bắt buộc.';
                            END IF;
                        END IF;
                    END IF;
                END IF;
            END IF;
        END IF;
    END IF;
END//

-- =============================================================================
-- 2. sp_grant_license_manual
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_grant_license_manual//
CREATE PROCEDURE sp_grant_license_manual(
    IN  p_person_id  INT,
    IN  p_license_id INT,
    OUT p_grant_id   INT
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;

    SET p_grant_id = 0;

    IF p_person_id IS NULL OR p_license_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'person_id và license_id không được NULL.';
    END IF;

    SELECT COUNT(*) INTO v_exists
    FROM GRANTED_LICENSE
    WHERE person_id = p_person_id
      AND license_id = p_license_id;

    IF v_exists > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Người dùng đã sở hữu gói License này.';
    END IF;

    INSERT INTO GRANTED_LICENSE (person_id, license_id)
    VALUES (p_person_id, p_license_id);
    SET p_grant_id = LAST_INSERT_ID();
END//

-- =============================================================================
-- 3. sp_revoke_license_manual
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_revoke_license_manual//
CREATE PROCEDURE sp_revoke_license_manual(
    IN  p_person_id  INT,
    IN  p_license_id INT,
    OUT p_deleted    INT
)
BEGIN
    DELETE FROM GRANTED_LICENSE
    WHERE person_id = p_person_id
      AND license_id = p_license_id;
    SET p_deleted = ROW_COUNT();
END//

-- =============================================================================
-- 4. sp_borrow_book
-- BORROW uses member_id, not person_id. person_id is resolved only for AUTHOR_OF.
-- STUDENT: 3 active loans / 14 days. FACULTY: 10 active loans / 30 days.
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_borrow_book//
CREATE PROCEDURE sp_borrow_book(
    IN  p_member_id  VARCHAR(20),
    IN  p_copy_id    VARCHAR(30),
    OUT p_borrow_id  INT,
    OUT p_due_date   DATE
)
BEGIN
    DECLARE v_person_id       INT DEFAULT NULL;
    DECLARE v_member_type     VARCHAR(20) DEFAULT NULL;
    DECLARE v_overdue_count   INT DEFAULT 0;
    DECLARE v_unpaid_count    INT DEFAULT 0;
    DECLARE v_active_count    INT DEFAULT 0;
    DECLARE v_active_copy     INT DEFAULT 0;
    DECLARE v_quota           INT DEFAULT 0;
    DECLARE v_loan_days       INT DEFAULT 14;
    DECLARE v_resource_id     INT DEFAULT NULL;
    DECLARE v_copy_status     VARCHAR(20) DEFAULT NULL;
    DECLARE v_is_public       BOOLEAN DEFAULT FALSE;
    DECLARE v_is_author       INT DEFAULT 0;
    DECLARE v_required_count  INT DEFAULT 0;
    DECLARE v_borrow_date     DATE;
    DECLARE v_has_all         BOOLEAN DEFAULT FALSE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_borrow_id = 0;
    SET p_due_date = NULL;

    START TRANSACTION;

        IF p_member_id IS NULL OR TRIM(p_member_id) = '' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR01_INELIGIBLE_MEMBER] member_id là bắt buộc.';
        END IF;

        -- Lock member row to serialize quota checks for the same member.
        SELECT person_id, member_type
          INTO v_person_id, v_member_type
        FROM UNIVERSITY_MEMBER
        WHERE member_id = p_member_id
        FOR UPDATE;

        IF v_person_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR01_INELIGIBLE_MEMBER] Chỉ UNIVERSITY_MEMBER mới được mượn sách.';
        END IF;

        SET v_quota = CASE WHEN v_member_type = 'FACULTY' THEN 10 ELSE 3 END;
        SET v_loan_days = CASE WHEN v_member_type = 'FACULTY' THEN 30 ELSE 14 END;

        SELECT COUNT(*) INTO v_overdue_count
        FROM BORROW
        WHERE member_id = p_member_id
          AND return_date IS NULL
          AND due_date < CURDATE();

        IF v_overdue_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_OVERDUE_PENDING] Thành viên đang có sách mượn quá hạn chưa trả.';
        END IF;

        SELECT COUNT(*) INTO v_unpaid_count
        FROM BORROW b
        INNER JOIN FINE f ON f.borrow_id = b.borrow_id
        WHERE b.member_id = p_member_id
          AND f.payment_status = 'Unpaid';

        IF v_unpaid_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_OUTSTANDING_FINE] Thành viên có phiếu phạt chưa thanh toán.';
        END IF;

        SELECT COUNT(*) INTO v_active_count
        FROM BORROW
        WHERE member_id = p_member_id
          AND return_date IS NULL;

        IF v_active_count >= v_quota THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_QUOTA_EXCEEDED] Thành viên đã đạt giới hạn số lượng sách mượn.';
        END IF;

        -- Lock a copy that belongs to the PHYSICAL_BOOK subtype.
        SELECT bc.resource_id, bc.availability_status, r.is_public
          INTO v_resource_id, v_copy_status, v_is_public
        FROM BOOK_COPY bc
        INNER JOIN PHYSICAL_BOOK pb ON pb.resource_id = bc.resource_id
        INNER JOIN RESOURCE r ON r.resource_id = pb.resource_id
        WHERE bc.copy_id = p_copy_id
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
        WHERE copy_id = p_copy_id
          AND return_date IS NULL;

        IF v_active_copy > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao đang có giao dịch mượn chưa trả.';
        END IF;

        SELECT COUNT(*) INTO v_is_author
        FROM AUTHOR_OF
        WHERE person_id = v_person_id
          AND resource_id = v_resource_id;

        SELECT COUNT(*) INTO v_required_count
        FROM RESOURCE_LICENSE
        WHERE resource_id = v_resource_id;

        -- Author of this exact physical book bypasses only the license check.
        IF v_is_public = FALSE AND v_is_author = 0 THEN
            IF v_required_count = 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = '[BR04_LICENSE_REQUIRED] Sách private chưa khai báo License yêu cầu.';
            END IF;

            SET v_has_all = fn_has_all_required_licenses(v_person_id, v_resource_id);
            IF v_has_all = FALSE THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = '[BR04_LICENSE_REQUIRED] Thành viên chưa sở hữu đầy đủ License bắt buộc cho sách.';
            END IF;
        END IF;

        SET v_borrow_date = CURDATE();
        SET p_due_date = DATE_ADD(v_borrow_date, INTERVAL v_loan_days DAY);

        INSERT INTO BORROW (member_id, copy_id, borrow_date, due_date)
        VALUES (p_member_id, p_copy_id, v_borrow_date, p_due_date);
        SET p_borrow_id = LAST_INSERT_ID();

        UPDATE BOOK_COPY
        SET availability_status = 'Borrowed'
        WHERE copy_id = p_copy_id;

    COMMIT;
END//

-- =============================================================================
-- 5. sp_return_book
-- Late-return fine is 50,000. Add it to an existing Unpaid fine; when all old
-- fines are Completed, create a new FINE row to preserve payment history.
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
            -- Add the new penalty to the latest outstanding fine, if one exists.
            SET v_existing_fine_id = NULL;
            SET v_existing_fine_amount = NULL;

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
                -- No outstanding fine: create a new one, including when prior
                -- fines for this borrow have already been settled.
                INSERT INTO FINE (borrow_id, total_fine_amount, payment_status)
                VALUES (p_borrow_id, 50000.00, 'Unpaid');

                SET p_fine_id = LAST_INSERT_ID();
                SET p_fine_amount = 50000.00;
            END IF;
        END IF;

    COMMIT;
END//

-- =============================================================================
-- 6. sp_confirm_fine_payment
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
    START TRANSACTION;

        SELECT payment_status INTO v_payment_status
        FROM FINE
        WHERE fine_id = p_fine_id
        FOR UPDATE;

        IF v_payment_status IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy phiếu phạt với ID đã cho.';
        END IF;

        IF v_payment_status = 'Completed' THEN
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
-- 7. sp_access_digital_journal
-- No person_id: anonymous Guest, allowed only when RESOURCE.is_public = TRUE.
-- person_id supplied: validate PERSON; author of this journal bypasses License;
-- otherwise all required License packages must be granted to that PERSON.
-- Successful accesses log person_id (nullable), journal_resource_id and a
-- synthetic random documentation-range IP generated inside this procedure.
-- No real client IP is accepted from the application or stored.
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
    DECLARE v_is_public      BOOLEAN DEFAULT FALSE;
    DECLARE v_person_count   INT DEFAULT 0;
    DECLARE v_is_author      INT DEFAULT 0;
    DECLARE v_required_count INT DEFAULT 0;
    DECLARE v_has_all        BOOLEAN DEFAULT FALSE;
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

    START TRANSACTION;

        IF NOT EXISTS (
            SELECT 1
            FROM DIGITAL_JOURNAL
            WHERE resource_id = p_resource_id
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy DIGITAL_JOURNAL với resource_id đã cho.';
        END IF;

        SELECT is_public INTO v_is_public
        FROM RESOURCE
        WHERE resource_id = p_resource_id;

        IF p_person_id IS NULL THEN
            -- No ID: Guest path, public flag is the only access criterion.
            IF v_is_public = TRUE THEN
                SET p_allowed = TRUE;
                SET p_reason = 'Guest không định danh được truy cập journal công khai.';
            ELSE
                SET p_reason = 'Guest không định danh chỉ được truy cập journal công khai.';
            END IF;
        ELSE
            -- ID supplied: do not silently downgrade an invalid ID to Guest.
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
                    ELSE
                        SET v_has_all = fn_has_all_required_licenses(p_person_id, p_resource_id);
                        IF v_has_all THEN
                            SET p_allowed = TRUE;
                            SET p_reason = 'PERSON không phải tác giả nhưng có đầy đủ License bắt buộc.';
                        ELSE
                            SET p_reason = 'PERSON không phải tác giả và chưa có đầy đủ License bắt buộc.';
                        END IF;
                    END IF;
                END IF;
            END IF;
        END IF;

        -- Log successful accesses only. Every click generates a fresh synthetic
        -- IP from TEST-NET documentation ranges; it is not the user's real IP.
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

DELIMITER ;
