-- =============================================================================
-- KỊCH BẢN TẠO VÀI NGHÌN DÒNG DỮ LIỆU ĐỂ KIỂM THỬ HIỆU NĂNG (PERFORMANCE TEST)
-- Dữ liệu được sinh bằng Stored Procedure của MySQL để chạy cực nhanh
-- =============================================================================

USE library_db;

DELIMITER $$

DROP PROCEDURE IF EXISTS GenerateLargeTestData$$

CREATE PROCEDURE GenerateLargeTestData(
    IN num_access_logs INT,
    IN num_book_copies INT
)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE rand_resource INT;
    DECLARE rand_ip VARCHAR(45);
    
    -- Tạm thời tắt auto-commit để tối ưu tốc độ ghi hàng nghìn dòng
    SET autocommit = 0;

    -- 1. Sinh hàng nghìn dòng nhật ký truy cập ACCESS_LOG (Kiểm tra BR13 & Tốc độ ghi Decoupled)
    SELECT CONCAT('--> Đang sinh ', num_access_logs, ' dòng ACCESS_LOG...') AS Progress;
    SET i = 1;
    WHILE i <= num_access_logs DO
        -- Chọn ngẫu nhiên giữa các journal 3, 4, 5, 6
        SET rand_resource = 3 + FLOOR(RAND() * 4);
        -- Sinh ngẫu nhiên địa chỉ IP
        SET rand_ip = CONCAT('192.168.', FLOOR(RAND() * 255), '.', FLOOR(RAND() * 255));
        
        INSERT INTO ACCESS_LOG (journal_resource_id, ip_address)
        VALUES (rand_resource, rand_ip);
        
        SET i = i + 1;
    END WHILE;
    COMMIT;

    -- 2. Sinh thêm các bản sao sách BOOK_COPY
    SELECT CONCAT('--> Đang sinh ', num_book_copies, ' dòng BOOK_COPY...') AS Progress;
    SET i = 1;
    WHILE i <= num_book_copies DO
        -- Phân bổ ngẫu nhiên vào sách 1, 2, 7
        SET rand_resource = CASE FLOOR(RAND() * 3)
            WHEN 0 THEN 1
            WHEN 1 THEN 2
            ELSE 7
        END;
        
        INSERT IGNORE INTO BOOK_COPY (copy_id, resource_id, availability_status)
        VALUES (CONCAT(rand_resource, '_gen_', i), rand_resource, IF(RAND() > 0.5, 'Available', 'Borrowed'));
        
        SET i = i + 1;
    END WHILE;
    COMMIT;

    SET autocommit = 1;
    SELECT '--> HOÀN TẤT SINH DỮ LIỆU TEST HÀNG NGHÌN DÒNG THÀNH CÔNG!' AS Status;
END$$

DELIMITER ;

-- =============================================================================
-- Thực thi sinh 5,000 dòng ACCESS_LOG và 1,000 bản sao BOOK_COPY
-- =============================================================================
CALL GenerateLargeTestData(5000, 1000);

-- Kiểm tra lại tổng số lượng bản ghi sau khi sinh
SELECT 'ACCESS_LOG' AS Table_Name, COUNT(*) AS Total_Records FROM ACCESS_LOG
UNION ALL
SELECT 'BOOK_COPY', COUNT(*) FROM BOOK_COPY
UNION ALL
SELECT 'RESOURCE', COUNT(*) FROM RESOURCE
UNION ALL
SELECT 'PERSON', COUNT(*) FROM PERSON;
