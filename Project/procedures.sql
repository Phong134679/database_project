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

DELIMITER ;
