-- =============================================================================
-- MIGRATION SCRIPT (NON-DESTRUCTIVE): Chuyển đổi License & is_public lên RESOURCE
-- Mục tiêu:
--   1. Bảo toàn dữ liệu cũ trước khi xóa valid_from / valid_to (lưu vào GRANTED_LICENSE_BACKUP_DATES).
--   2. Chuyển is_public từ DIGITAL_JOURNAL (nếu có) lên bảng cha RESOURCE.
--   3. Chuyển FK của RESOURCE_LICENSE từ DIGITAL_JOURNAL lên RESOURCE.
--   4. Bỏ valid_from, valid_to khỏi GRANTED_LICENSE và thêm UNIQUE(person_id, license_id).
--   5. Tạo Stored Function fn_has_all_required_licenses.
-- =============================================================================

USE library_db;

-- 1. Sao lưu bảng GRANTED_LICENSE (nếu vẫn còn valid_from/valid_to và chưa có bảng backup)
SET @has_vf = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'GRANTED_LICENSE' AND COLUMN_NAME = 'valid_from'
);
SET @sql_backup = IF(@has_vf > 0,
    'CREATE TABLE IF NOT EXISTS GRANTED_LICENSE_BACKUP_DATES AS SELECT grant_id, person_id, license_id, valid_from, valid_to, NOW() AS backed_up_at FROM GRANTED_LICENSE;',
    'SELECT "GRANTED_LICENSE_BACKUP_DATES skipped (valid_from already removed)." AS notice;'
);
PREPARE stmt_bk FROM @sql_backup;
EXECUTE stmt_bk;
DEALLOCATE PREPARE stmt_bk;

-- 2. Thêm cột is_public vào bảng cha RESOURCE (nếu chưa có)
SET @has_res_pub = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'RESOURCE' AND COLUMN_NAME = 'is_public'
);
SET @sql_add_res_pub = IF(@has_res_pub = 0,
    'ALTER TABLE RESOURCE ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;',
    'SELECT "RESOURCE.is_public already exists." AS notice;'
);
PREPARE stmt_arp FROM @sql_add_res_pub;
EXECUTE stmt_arp;
DEALLOCATE PREPARE stmt_arp;

-- 3. Bảo toàn dữ liệu is_public từ DIGITAL_JOURNAL (nếu cột dj.is_public tồn tại)
SET @has_dj_pub = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'DIGITAL_JOURNAL' AND COLUMN_NAME = 'is_public'
);
SET @sql_copy_pub = IF(@has_dj_pub > 0,
    'UPDATE RESOURCE r JOIN DIGITAL_JOURNAL dj ON r.resource_id = dj.resource_id SET r.is_public = dj.is_public;',
    'UPDATE RESOURCE SET is_public = TRUE WHERE resource_id IN (1, 4, 7);'
);
PREPARE stmt_cp FROM @sql_copy_pub;
EXECUTE stmt_cp;
DEALLOCATE PREPARE stmt_cp;

-- Nếu bảng con DIGITAL_JOURNAL vẫn còn cột is_public, gỡ bỏ để tránh trùng lặp
SET @sql_drop_dj_pub = IF(@has_dj_pub > 0,
    'ALTER TABLE DIGITAL_JOURNAL DROP COLUMN is_public;',
    'SELECT "DIGITAL_JOURNAL.is_public already removed." AS notice;'
);
PREPARE stmt_ddp FROM @sql_drop_dj_pub;
EXECUTE stmt_ddp;
DEALLOCATE PREPARE stmt_ddp;

-- 4. Chuyển FK của RESOURCE_LICENSE sang RESOURCE(resource_id)
SET @old_fk = (
    SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'RESOURCE_LICENSE'
      AND COLUMN_NAME = 'resource_id'
      AND REFERENCED_TABLE_NAME = 'DIGITAL_JOURNAL'
    LIMIT 1
);
SET @sql_drop_fk = IF(@old_fk IS NOT NULL,
    CONCAT('ALTER TABLE RESOURCE_LICENSE DROP FOREIGN KEY ', @old_fk, ';'),
    'SELECT "Old FK fk_reslic_journal not present." AS notice;'
);
PREPARE stmt_dfk FROM @sql_drop_fk;
EXECUTE stmt_dfk;
DEALLOCATE PREPARE stmt_dfk;

SET @has_new_fk = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'RESOURCE_LICENSE'
      AND COLUMN_NAME = 'resource_id'
      AND REFERENCED_TABLE_NAME = 'RESOURCE'
);
SET @sql_add_fk = IF(@has_new_fk = 0,
    'ALTER TABLE RESOURCE_LICENSE ADD CONSTRAINT fk_reslic_resource FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id) ON DELETE CASCADE ON UPDATE CASCADE;',
    'SELECT "New FK fk_reslic_resource already exists." AS notice;'
);
PREPARE stmt_afk FROM @sql_add_fk;
EXECUTE stmt_afk;
DEALLOCATE PREPARE stmt_afk;

-- 5. Gỡ valid_from, valid_to khỏi GRANTED_LICENSE và thêm UNIQUE(person_id, license_id)
SET @sql_drop_vf = IF(@has_vf > 0,
    'ALTER TABLE GRANTED_LICENSE DROP COLUMN valid_from, DROP COLUMN valid_to;',
    'SELECT "valid_from/valid_to already dropped." AS notice;'
);
PREPARE stmt_dvf FROM @sql_drop_vf;
EXECUTE stmt_dvf;
DEALLOCATE PREPARE stmt_dvf;

SET @has_uq = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'GRANTED_LICENSE' AND CONSTRAINT_NAME = 'uq_granted_person_license'
);
SET @sql_add_uq = IF(@has_uq = 0,
    'ALTER TABLE GRANTED_LICENSE ADD CONSTRAINT uq_granted_person_license UNIQUE (person_id, license_id);',
    'SELECT "UNIQUE(person_id, license_id) already exists." AS notice;'
);
PREPARE stmt_uq FROM @sql_add_uq;
EXECUTE stmt_uq;
DEALLOCATE PREPARE stmt_uq;

-- 6. Tạo Stored Function kiểm tra License
DROP FUNCTION IF EXISTS fn_has_all_required_licenses;
CREATE FUNCTION fn_has_all_required_licenses(p_person_id INT, p_resource_id INT)
RETURNS BOOLEAN
READS SQL DATA
RETURN (
    (SELECT COUNT(*) FROM RESOURCE_LICENSE WHERE resource_id = p_resource_id) > 0
    AND
    (SELECT COUNT(DISTINCT rl.license_id)
     FROM RESOURCE_LICENSE rl
     INNER JOIN GRANTED_LICENSE gl ON rl.license_id = gl.license_id
     WHERE rl.resource_id = p_resource_id
       AND gl.person_id = p_person_id)
    =
    (SELECT COUNT(*) FROM RESOURCE_LICENSE WHERE resource_id = p_resource_id)
);
