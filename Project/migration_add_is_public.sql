-- =============================================================================
-- MIGRATION SCRIPT: Bổ sung cờ is_public cho bảng DIGITAL_JOURNAL
-- Mục đích: Hỗ trợ tài liệu số công khai (Open Access) cho mọi đối tượng gồm khách ngoài
-- Tương thích ngược: Sử dụng ALTER TABLE, không làm mất dữ liệu hiện tại
-- =============================================================================

USE library_db;

-- 1. Bổ sung cột is_public (nếu chưa có)
SET @dbname = DATABASE();
SET @tablename = "DIGITAL_JOURNAL";
SET @columnname = "is_public";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column is_public already exists.' AS notice;",
  "ALTER TABLE DIGITAL_JOURNAL ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cờ tài liệu mở/công khai cho mọi đối tượng';"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. Đặt Journal #4 (ACM Computing Surveys) thành tài liệu mở / công khai
UPDATE DIGITAL_JOURNAL SET is_public = TRUE WHERE resource_id = 4;
