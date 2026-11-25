-- =============================================================================
-- STORED PROCEDURES: License Management & Resource Access Control
-- Đồng bộ quy tắc nghiệp vụ với Java Service và Web UI:
--   - Phương án B: OR bên ngoài (is_public), AND bên trong (đủ mọi License yêu cầu)
--   - Đặc quyền AUTHOR_OF chỉ áp dụng cho đọc DIGITAL_JOURNAL, không áp dụng mượn sách vật lý
--   - Cấp và thu hồi License chỉ thực hiện theo phương thức MANUAL
-- =============================================================================

USE library_db;

DELIMITER //

DROP PROCEDURE IF EXISTS sp_check_resource_access//
CREATE PROCEDURE sp_check_resource_access(
    IN  p_person_id   INT,
    IN  p_resource_id INT,
    IN  p_action      VARCHAR(20), -- 'READ_DIGITAL' hoặc 'BORROW_BOOK'
    OUT p_allowed     BOOLEAN,
    OUT p_reason      VARCHAR(255)
)
BEGIN
    DECLARE v_res_type VARCHAR(30);
    DECLARE v_is_public BOOLEAN DEFAULT FALSE;
    DECLARE v_is_author INT DEFAULT 0;
    DECLARE v_req_count INT DEFAULT 0;
    DECLARE v_owned_count INT DEFAULT 0;
    DECLARE v_person_type VARCHAR(30);

    -- 1. Kiểm tra Resource tồn tại
    SELECT resource_type, is_public
    INTO v_res_type, v_is_public
    FROM RESOURCE
    WHERE resource_id = p_resource_id;

    IF v_res_type IS NULL THEN
        SET p_allowed = FALSE;
        SET p_reason = 'Tài nguyên không tồn tại.';
    ELSE
        -- 2. Nếu là mượn sách vật lý: Kiểm tra BR01 trước, KHÔNG áp dụng đặc quyền AUTHOR_OF
        IF UPPER(IFNULL(p_action, '')) = 'BORROW_BOOK' THEN
            SELECT person_type INTO v_person_type FROM PERSON WHERE person_id = p_person_id;
            IF v_person_type IS NULL OR v_person_type NOT IN ('STUDENT', 'FACULTY') THEN
                SET p_allowed = FALSE;
                SET p_reason = 'Đối tượng ngoài (GUEST / EXTERNAL_AUTHOR) không được phép mượn sách vật lý (BR01).';
            ELSEIF v_is_public = TRUE THEN
                SET p_allowed = TRUE;
                SET p_reason = 'Tài nguyên công khai (is_public = TRUE).';
            ELSE
                -- Kiểm tra riêng trường hợp không khai báo License để tránh lỗi double NOT EXISTS
                SELECT COUNT(*) INTO v_req_count FROM RESOURCE_LICENSE WHERE resource_id = p_resource_id;
                IF v_req_count = 0 THEN
                    SET p_allowed = FALSE;
                    SET p_reason = 'Sách vật lý private chưa khai báo License yêu cầu (RESOURCE_LICENSE rỗng).';
                ELSE
                    SELECT COUNT(DISTINCT rl.license_id) INTO v_owned_count
                    FROM RESOURCE_LICENSE rl
                    INNER JOIN GRANTED_LICENSE gl ON rl.license_id = gl.license_id
                    WHERE rl.resource_id = p_resource_id AND gl.person_id = p_person_id;

                    IF v_owned_count = v_req_count THEN
                        SET p_allowed = TRUE;
                        SET p_reason = 'Người dùng sở hữu đầy đủ toàn bộ License yêu cầu.';
                    ELSE
                        SET p_allowed = FALSE;
                        SET p_reason = 'Thiếu License bắt buộc để mượn tài nguyên private.';
                    END IF;
                END IF;
            END IF;
        ELSE
            -- 3. Đọc tài liệu số: Thứ tự kiểm tra chính thức
            -- Bước 3.1: Đặc quyền tác giả (AUTHOR_OF) chỉ cho đọc tài liệu số của chính mình
            IF p_person_id IS NOT NULL AND p_person_id > 0 THEN
                SELECT COUNT(*) INTO v_is_author
                FROM AUTHOR_OF
                WHERE person_id = p_person_id AND resource_id = p_resource_id;
            END IF;

            IF v_is_author > 0 THEN
                SET p_allowed = TRUE;
                SET p_reason = 'Đặc quyền Tác giả (AUTHOR_OF).';
            ELSEIF v_is_public = TRUE THEN
                SET p_allowed = TRUE;
                SET p_reason = 'Tài nguyên công khai (is_public = TRUE).';
            ELSE
                IF p_person_id IS NULL OR p_person_id <= 0 THEN
                    SET p_allowed = FALSE;
                    SET p_reason = 'Tài liệu private yêu cầu định danh người dùng và License hợp lệ.';
                ELSE
                    SELECT COUNT(*) INTO v_req_count FROM RESOURCE_LICENSE WHERE resource_id = p_resource_id;
                    IF v_req_count = 0 THEN
                        SET p_allowed = FALSE;
                        SET p_reason = 'Tài liệu private chưa khai báo gói License nào.';
                    ELSE
                        SELECT COUNT(DISTINCT rl.license_id) INTO v_owned_count
                        FROM RESOURCE_LICENSE rl
                        INNER JOIN GRANTED_LICENSE gl ON rl.license_id = gl.license_id
                        WHERE rl.resource_id = p_resource_id AND gl.person_id = p_person_id;

                        IF v_owned_count = v_req_count THEN
                            SET p_allowed = TRUE;
                            SET p_reason = 'Người dùng sở hữu đầy đủ tất cả License bắt buộc.';
                        ELSE
                            SET p_allowed = FALSE;
                            SET p_reason = 'Người dùng chưa sở hữu đầy đủ tất cả License bắt buộc.';
                        END IF;
                    END IF;
                END IF;
            END IF;
        END IF;
    END IF;
END//

DROP PROCEDURE IF EXISTS sp_grant_license_manual//
CREATE PROCEDURE sp_grant_license_manual(
    IN  p_person_id  INT,
    IN  p_license_id INT,
    OUT p_grant_id   INT
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO v_exists
    FROM GRANTED_LICENSE
    WHERE person_id = p_person_id AND license_id = p_license_id;

    IF v_exists > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Người dùng đã sở hữu gói License này (Tránh cấp trùng).';
    ELSE
        INSERT INTO GRANTED_LICENSE (person_id, license_id) VALUES (p_person_id, p_license_id);
        SET p_grant_id = LAST_INSERT_ID();
    END IF;
END//

DROP PROCEDURE IF EXISTS sp_revoke_license_manual//
CREATE PROCEDURE sp_revoke_license_manual(
    IN  p_person_id  INT,
    IN  p_license_id INT,
    OUT p_deleted    INT
)
BEGIN
    DELETE FROM GRANTED_LICENSE
    WHERE person_id = p_person_id AND license_id = p_license_id;
    SET p_deleted = ROW_COUNT();
END//

-- =============================================================================
-- sp_borrow_book: Xử lý giao dịch mượn sách vật lý (BR01, BR05, BR10, License)
-- Khóa theo thứ tự PERSON -> BOOK_COPY bằng FOR UPDATE để chống race condition
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_borrow_book//
CREATE PROCEDURE sp_borrow_book(
    IN  p_member_id INT,
    IN  p_copy_id   INT,
    OUT p_borrow_id INT,
    OUT p_due_date  DATE
)
BEGIN
    DECLARE v_person_type    VARCHAR(30);
    DECLARE v_overdue_count  INT DEFAULT 0;
    DECLARE v_fine_count     INT DEFAULT 0;
    DECLARE v_active_borrows INT DEFAULT 0;
    DECLARE v_quota          INT DEFAULT 0;
    DECLARE v_loan_days      INT DEFAULT 14;
    DECLARE v_resource_id    INT;
    DECLARE v_copy_status    VARCHAR(20);
    DECLARE v_is_public      BOOLEAN DEFAULT FALSE;
    DECLARE v_borrow_date    DATE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_borrow_id = 0;
    SET p_due_date = NULL;

    START TRANSACTION;

        -- 1. Khóa dòng PERSON để tuần tự hóa kiểm tra quota/overdue/fine theo từng thành viên
        SELECT person_type
        INTO v_person_type
        FROM PERSON
        WHERE person_id = p_member_id
        FOR UPDATE;

        IF v_person_type IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy thành viên với ID đã cho.';
        END IF;

        -- 2. BR01: Chỉ STUDENT và FACULTY được phép mượn sách vật lý
        IF v_person_type NOT IN ('STUDENT', 'FACULTY') THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR01_INELIGIBLE_MEMBER] Chỉ STUDENT và FACULTY mới được phép mượn sách vật lý.';
        END IF;

        -- 3. BR05-a: Kiểm tra sách quá hạn chưa trả
        SELECT COUNT(*) INTO v_overdue_count
        FROM BORROW
        WHERE member_person_id = p_member_id
          AND return_date IS NULL
          AND due_date < CURDATE();

        IF v_overdue_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_OVERDUE_PENDING] Thành viên đang có sách mượn quá hạn chưa trả.';
        END IF;

        -- 4. BR05-b: Kiểm tra phiếu phạt chưa thanh toán
        SELECT COUNT(*) INTO v_fine_count
        FROM FINE f
        INNER JOIN BORROW b ON f.borrow_id = b.borrow_id
        WHERE b.member_person_id = p_member_id
          AND f.payment_status <> 'Completed';

        IF v_fine_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_OUTSTANDING_FINE] Thành viên có phiếu phạt chưa thanh toán.';
        END IF;

        -- 5. BR05-c: Kiểm tra hạn mức mượn (STUDENT=3 / 14 ngày, FACULTY=10 / 30 ngày)
        IF v_person_type = 'FACULTY' THEN
            SET v_quota = 10;
            SET v_loan_days = 30;
        ELSE
            SET v_quota = 3;
            SET v_loan_days = 14;
        END IF;

        SELECT COUNT(*) INTO v_active_borrows
        FROM BORROW
        WHERE member_person_id = p_member_id
          AND return_date IS NULL;

        IF v_active_borrows >= v_quota THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR05_QUOTA_EXCEEDED] Thành viên đã đạt giới hạn số lượng sách mượn.';
        END IF;

        -- 6. BR10: Khóa dòng BOOK_COPY để chống 2 giao dịch cùng mượn 1 bản sao
        SELECT bc.resource_id, bc.availability_status, r.is_public
        INTO v_resource_id, v_copy_status, v_is_public
        FROM BOOK_COPY bc
        INNER JOIN RESOURCE r ON bc.resource_id = r.resource_id
        WHERE bc.copy_id = p_copy_id
        FOR UPDATE;

        IF v_resource_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao sách không tồn tại.';
        END IF;

        IF v_copy_status <> 'Available' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR10_BOOK_NOT_AVAILABLE] Bản sao sách hiện không sẵn sàng để mượn.';
        END IF;

        -- 7. Kiểm tra quyền truy cập Resource (is_public OR fn_has_all_required_licenses)
        IF v_is_public = FALSE AND fn_has_all_required_licenses(p_member_id, v_resource_id) = FALSE THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR04_LICENSE_EXPIRED] Thành viên chưa sở hữu đầy đủ License bắt buộc cho tài nguyên này.';
        END IF;

        -- 8. Tạo phiếu mượn và cập nhật trạng thái bản sao
        SET v_borrow_date = CURDATE();
        SET p_due_date = DATE_ADD(v_borrow_date, INTERVAL v_loan_days DAY);

        INSERT INTO BORROW (member_person_id, copy_id, borrow_date, due_date)
        VALUES (p_member_id, p_copy_id, v_borrow_date, p_due_date);
        SET p_borrow_id = LAST_INSERT_ID();

        UPDATE BOOK_COPY
        SET availability_status = 'Borrowed'
        WHERE copy_id = p_copy_id;

    COMMIT;
END//

-- =============================================================================
-- sp_return_book: Xử lý trả sách vật lý & tự động phát sinh phiếu phạt (BR02, FR05)
-- Khóa BORROW và BOOK_COPY bằng FOR UPDATE trong transaction để chống race condition
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_return_book//
CREATE PROCEDURE sp_return_book(
    IN  p_borrow_id   INT,
    IN  p_return_date DATE,
    OUT p_fine_id     INT,
    OUT p_fine_amount DECIMAL(12,2)
)
BEGIN
    DECLARE v_copy_id            INT;
    DECLARE v_locked_copy_id     INT;
    DECLARE v_borrow_date        DATE;
    DECLARE v_due_date           DATE;
    DECLARE v_ret_date           DATE;
    DECLARE v_actual_return_date DATE;
    DECLARE v_existing_fine_id   INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_fine_id = 0;
    SET p_fine_amount = 0.00;

    START TRANSACTION;

        -- 1. Khóa bản ghi BORROW trước khi kiểm tra trạng thái
        SELECT copy_id, borrow_date, due_date, return_date
        INTO v_copy_id, v_borrow_date, v_due_date, v_ret_date
        FROM BORROW
        WHERE borrow_id = p_borrow_id
        FOR UPDATE;

        IF v_copy_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy phiếu mượn với ID đã cho.';
        END IF;

        -- 2. BR02: Kiểm tra phiếu mượn chưa được trả
        IF v_ret_date IS NOT NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR02_ALREADY_RETURNED] Sách thuộc lượt mượn này đã được trả trước đó.';
        END IF;

        -- 3. Xác định ngày trả thực tế (NULL -> CURDATE()) và kiểm tra hợp lệ
        SET v_actual_return_date = COALESCE(p_return_date, CURDATE());
        IF v_borrow_date IS NOT NULL AND v_actual_return_date < v_borrow_date THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[INVALID_DATE_RANGE] Ngày trả không thể trước ngày mượn.';
        END IF;

        -- 4. Khóa BOOK_COPY trong cùng transaction
        SELECT copy_id INTO v_locked_copy_id
        FROM BOOK_COPY
        WHERE copy_id = v_copy_id
        FOR UPDATE;

        -- 5. Cập nhật BORROW và BOOK_COPY
        UPDATE BORROW
        SET return_date = v_actual_return_date
        WHERE borrow_id = p_borrow_id;

        UPDATE BOOK_COPY
        SET availability_status = 'Available'
        WHERE copy_id = v_copy_id;

        -- 6. FR05: Tự động phát sinh phiếu phạt cố định 50,000 VND nếu trả quá hạn
        IF v_due_date IS NOT NULL AND v_actual_return_date > v_due_date THEN
            SELECT fine_id INTO v_existing_fine_id
            FROM FINE
            WHERE borrow_id = p_borrow_id
            LIMIT 1;

            IF v_existing_fine_id IS NULL THEN
                INSERT INTO FINE (borrow_id, total_fine_amount, payment_status)
                VALUES (p_borrow_id, 50000.00, 'Unpaid');
                SET p_fine_id = LAST_INSERT_ID();
                SET p_fine_amount = 50000.00;
            END IF;
        END IF;

    COMMIT;
END//

-- =============================================================================
-- sp_confirm_fine_payment: Xác nhận thanh toán phiếu phạt (BR11)
-- Khóa FINE bằng FOR UPDATE trong transaction để chống thanh toán trùng
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_confirm_fine_payment//
CREATE PROCEDURE sp_confirm_fine_payment(
    IN  p_fine_id        INT,
    OUT p_transaction_id INT
)
BEGIN
    DECLARE v_payment_status VARCHAR(30);
    DECLARE v_total_amount   DECIMAL(10,2);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_transaction_id = 0;

    START TRANSACTION;

        -- 1. Khóa dòng FINE trước khi kiểm tra trạng thái
        SELECT payment_status, total_fine_amount
        INTO v_payment_status, v_total_amount
        FROM FINE
        WHERE fine_id = p_fine_id
        FOR UPDATE;

        IF v_payment_status IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy phiếu phạt với ID đã cho.';
        END IF;

        -- 2. BR11: Không cho phép thanh toán phiếu phạt đã Completed
        IF v_payment_status = 'Completed' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[BR11_FINE_ALREADY_SETTLED] Phiếu phạt này đã được thanh toán hoàn tất trước đó.';
        END IF;

        -- 3. Ghi nhận giao dịch thanh toán (khớp cột NOT NULL trên PAYMENT_TRANSACTION)
        INSERT INTO PAYMENT_TRANSACTION (fine_id, amount_paid, payment_date, payment_method)
        VALUES (p_fine_id, v_total_amount, NOW(), 'Confirmed');
        SET p_transaction_id = LAST_INSERT_ID();

        -- 4. Cập nhật trạng thái phiếu phạt thành Completed
        UPDATE FINE
        SET payment_status = 'Completed'
        WHERE fine_id = p_fine_id;

    COMMIT;
END//

-- =============================================================================
-- sp_access_digital_journal: Thẩm định quyền đọc tài liệu số & ghi ACCESS_LOG
-- Thứ tự: AUTHOR_OF -> RESOURCE.is_public -> RESOURCE_LICENSE + fn_has_all_required_licenses
-- Thời điểm truy cập: thống nhất dùng NOW() (không dùng accessDate từ Java)
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_access_digital_journal//
CREATE PROCEDURE sp_access_digital_journal(
    IN  p_person_id   INT,
    IN  p_resource_id INT,
    IN  p_ip_address  VARCHAR(45),
    OUT p_allowed     BOOLEAN,
    OUT p_reason      VARCHAR(255),
    OUT p_log_id      BIGINT
)
BEGIN
    DECLARE v_res_type    VARCHAR(30);
    DECLARE v_is_public   BOOLEAN DEFAULT FALSE;
    DECLARE v_is_author   INT DEFAULT 0;
    DECLARE v_req_count   INT DEFAULT 0;
    DECLARE v_has_all     BOOLEAN DEFAULT FALSE;
    DECLARE v_access_res  VARCHAR(50) DEFAULT 'DENIED';
    DECLARE v_dl_success  TINYINT(1) DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_allowed = FALSE;
    SET p_reason = '';
    SET p_log_id = 0;

    START TRANSACTION;

        -- 1. Kiểm tra RESOURCE tồn tại và là DIGITAL_JOURNAL
        SELECT resource_type, is_public
        INTO v_res_type, v_is_public
        FROM RESOURCE
        WHERE resource_id = p_resource_id;

        IF v_res_type IS NULL OR v_res_type <> 'DIGITAL_JOURNAL' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '[NOT_FOUND] Không tìm thấy tài liệu số (DIGITAL_JOURNAL) với ID đã cho.';
        END IF;

        -- 2. Ghi nhận ACCESS_LOG trạng thái PENDING trước khi đánh giá quyền
        INSERT INTO ACCESS_LOG (person_id, journal_resource_id, access_timestamp, ip_address, download_success, access_result)
        VALUES (NULLIF(p_person_id, 0), p_resource_id, NOW(), p_ip_address, 0, 'PENDING');
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
            SET v_dl_success = 1;

        -- 4. Kiểm tra RESOURCE.is_public
        ELSEIF v_is_public = TRUE THEN
            SET p_allowed = TRUE;
            SET p_reason = 'Tài nguyên công khai (is_public = TRUE).';
            SET v_access_res = 'ALLOWED_PUBLIC';
            SET v_dl_success = 1;

        -- 5. Tài liệu private: kiểm tra định danh, khai báo License và fn_has_all_required_licenses
        ELSE
            IF p_person_id IS NULL OR p_person_id <= 0 THEN
                SET p_allowed = FALSE;
                SET p_reason = '[BR04_LICENSE_EXPIRED] Tài liệu private yêu cầu định danh người dùng và License hợp lệ.';
                SET v_access_res = 'DENIED_ANONYMOUS';
                SET v_dl_success = 0;
            ELSE
                SELECT COUNT(*) INTO v_req_count
                FROM RESOURCE_LICENSE
                WHERE resource_id = p_resource_id;

                IF v_req_count = 0 THEN
                    SET p_allowed = FALSE;
                    SET p_reason = '[BR04_LICENSE_EXPIRED] Tài liệu private chưa khai báo gói License nào.';
                    SET v_access_res = 'DENIED_NO_LICENSE_DECLARED';
                    SET v_dl_success = 0;
                ELSE
                    SET v_has_all = fn_has_all_required_licenses(p_person_id, p_resource_id);
                    IF v_has_all = TRUE THEN
                        SET p_allowed = TRUE;
                        SET p_reason = 'Người dùng sở hữu đầy đủ tất cả License bắt buộc.';
                        SET v_access_res = 'ALLOWED_LICENSE';
                        SET v_dl_success = 1;
                    ELSE
                        SET p_allowed = FALSE;
                        SET p_reason = '[BR04_LICENSE_EXPIRED] Người dùng chưa sở hữu đầy đủ tất cả License bắt buộc.';
                        SET v_access_res = 'DENIED_MISSING_LICENSE';
                        SET v_dl_success = 0;
                    END IF;
                END IF;
            END IF;
        END IF;

        -- 6. Cập nhật kết quả cuối vào ACCESS_LOG
        UPDATE ACCESS_LOG
        SET download_success = v_dl_success,
            access_result = v_access_res
        WHERE log_id = p_log_id;

    COMMIT;
END//

DELIMITER ;
