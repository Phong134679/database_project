-- =============================================================================
-- DATABASE SCHEMA: Library Management System & Open Repository
-- Based on EER Diagram, Promt.md and G5 Phase 1 Conceptual Design
-- Database Engine: MySQL 8.0+
-- =============================================================================

CREATE DATABASE IF NOT EXISTS library_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE library_db;

-- Tắt kiểm tra khóa ngoại tạm thời để thực hiện drop bảng an toàn nếu đã tồn tại
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ACCESS_LOG;
DROP TABLE IF EXISTS AUTHOR_OF;
DROP TABLE IF EXISTS RESOURCE_LICENSE;
DROP TABLE IF EXISTS GRANTED_LICENSE;
DROP TABLE IF EXISTS LICENSE_PACKAGE;
DROP TABLE IF EXISTS PAYMENT_TRANSACTION;
DROP TABLE IF EXISTS FINE;
DROP TABLE IF EXISTS BORROW;
DROP TABLE IF EXISTS BOOK_COPY;
DROP TABLE IF EXISTS DIGITAL_JOURNAL;
DROP TABLE IF EXISTS PHYSICAL_BOOK;
DROP TABLE IF EXISTS RESOURCE;
DROP TABLE IF EXISTS EXTERNAL_AUTHOR;
DROP TABLE IF EXISTS UNIVERSITY_MEMBER;
DROP TABLE IF EXISTS PERSON;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. NHÓM NGƯỜI DÙNG (PERSON & CÁC THỰC THỂ CON KẾ THỪA)
-- =============================================================================

-- Bảng cha: PERSON (Superclass lưu toàn bộ cá nhân tương tác với hệ thống)
CREATE TABLE PERSON (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    ssn VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(200),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(15),
    person_type ENUM('STUDENT', 'FACULTY', 'EXTERNAL_AUTHOR', 'GUEST') NOT NULL
) ENGINE = InnoDB;

-- Bảng con: UNIVERSITY_MEMBER (Thành viên nội bộ trường: Sinh viên / Giảng viên)
CREATE TABLE UNIVERSITY_MEMBER (
    person_id INT PRIMARY KEY,
    member_id VARCHAR(20) NOT NULL UNIQUE,
    department VARCHAR(100),
    member_type ENUM('STUDENT', 'FACULTY') NOT NULL,
    major VARCHAR(100) COMMENT 'Chỉ dùng cho Sinh viên',
    academic_rank VARCHAR(50) COMMENT 'Chỉ dùng cho Giảng viên',
    CONSTRAINT fk_univ_member_person
        FOREIGN KEY (person_id) REFERENCES PERSON(person_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng con: EXTERNAL_AUTHOR (Tác giả độc lập bên ngoài)
CREATE TABLE EXTERNAL_AUTHOR (
    person_id INT PRIMARY KEY,
    affiliation VARCHAR(150),
    biography TEXT,
    CONSTRAINT fk_external_author_person
        FOREIGN KEY (person_id) REFERENCES PERSON(person_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 2. NHÓM TÀI NGUYÊN (RESOURCE & CÁC THỰC THỂ CON KẾ THỪA)
-- =============================================================================

-- Bảng cha: RESOURCE (Danh mục chung cho cả sách vật lý và tạp chí số)
CREATE TABLE RESOURCE (
    resource_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(250) NOT NULL,
    publish_date DATE,
    topic VARCHAR(100),
    resource_type ENUM('PHYSICAL_BOOK', 'DIGITAL_JOURNAL') NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cờ tài nguyên công khai (TRUE: miễn License; FALSE: phải đủ toàn bộ License yêu cầu)'
) ENGINE = InnoDB;

-- Bảng con: PHYSICAL_BOOK (Sách in truyền thống - Đã bãi bỏ ISBN)
CREATE TABLE PHYSICAL_BOOK (
    resource_id INT PRIMARY KEY,
    shelf_location VARCHAR(50),
    CONSTRAINT fk_physical_book_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng con: DIGITAL_JOURNAL (Bài báo, tạp chí điện tử)
-- Đã loại bỏ thuộc tính 'doi'; thuộc tính 'is_public' đã chuyển lên bảng cha RESOURCE
CREATE TABLE DIGITAL_JOURNAL (
    resource_id INT PRIMARY KEY,
    url VARCHAR(250),
    file_size INT COMMENT 'Kích thước file tính theo KB/MB',
    format VARCHAR(10) COMMENT 'Định dạng PDF, EPUB, XML',
    CONSTRAINT fk_digital_journal_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng bản sao sách: BOOK_COPY (Mỗi cuốn sách vật lý có thể có nhiều bản sao)
-- Đã loại bỏ condition_status, barcode; copy_id được đánh theo quy tắc '<resource_id>_<index>' (kiểu VARCHAR)
CREATE TABLE BOOK_COPY (
    copy_id VARCHAR(30) PRIMARY KEY,
    resource_id INT NOT NULL,
    availability_status ENUM('Available', 'Borrowed') NOT NULL DEFAULT 'Available',
    CONSTRAINT fk_book_copy_physical_book
        FOREIGN KEY (resource_id) REFERENCES PHYSICAL_BOOK(resource_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 3. NHÓM GÓI BẢN QUYỀN SỐ (LICENSE_PACKAGE, GRANTED_LICENSE, RESOURCE_LICENSE)
-- =============================================================================

-- Bảng gói bản quyền
CREATE TABLE LICENSE_PACKAGE (
    license_id INT AUTO_INCREMENT PRIMARY KEY,
    license_name VARCHAR(150) NOT NULL,
    provider VARCHAR(100),
    description TEXT
) ENGINE = InnoDB;

-- Bảng cấp quyền: GRANTED_LICENSE (Phân quyền truy cập tài nguyên cho người dùng - cấp MANUAL)
-- Bỏ valid_from, valid_to; kiểm tra theo sự tồn tại bản ghi và ngăn cấp trùng bằng UNIQUE(person_id, license_id)
CREATE TABLE GRANTED_LICENSE (
    grant_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    license_id INT NOT NULL,
    CONSTRAINT uq_granted_person_license UNIQUE (person_id, license_id),
    CONSTRAINT fk_granted_license_person
        FOREIGN KEY (person_id) REFERENCES PERSON(person_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_granted_license_package
        FOREIGN KEY (license_id) REFERENCES LICENSE_PACKAGE(license_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng kết hợp N:M: RESOURCE_LICENSE (Tài nguyên thuộc các gói bản quyền bắt buộc nào)
-- FK resource_id trỏ lên bảng cha RESOURCE để áp dụng cho cả DIGITAL_JOURNAL và PHYSICAL_BOOK
CREATE TABLE RESOURCE_LICENSE (
    resource_id INT NOT NULL,
    license_id INT NOT NULL,
    PRIMARY KEY (resource_id, license_id),
    CONSTRAINT fk_reslic_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_reslic_package
        FOREIGN KEY (license_id) REFERENCES LICENSE_PACKAGE(license_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 4. NHÓM QUAN HỆ TÁC GIẢ (AUTHOR_OF: PERSON M:N RESOURCE)
-- =============================================================================

CREATE TABLE AUTHOR_OF (
    person_id INT NOT NULL,
    resource_id INT NOT NULL,
    PRIMARY KEY (person_id, resource_id),
    CONSTRAINT fk_author_of_person
        FOREIGN KEY (person_id) REFERENCES PERSON(person_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_author_of_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 5. NHÓM NGHIỆP VỤ MƯỢN TRẢ & XỬ PHẠT (BORROW, FINE, PAYMENT_TRANSACTION)
-- =============================================================================

-- Bảng giao dịch mượn sách: BORROW
CREATE TABLE BORROW (
    borrow_id INT AUTO_INCREMENT PRIMARY KEY,
    member_person_id INT NOT NULL,
    copy_id VARCHAR(30) NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE DEFAULT NULL,
    CONSTRAINT fk_borrow_member
        FOREIGN KEY (member_person_id) REFERENCES UNIVERSITY_MEMBER(person_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_borrow_copy
        FOREIGN KEY (copy_id) REFERENCES BOOK_COPY(copy_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng phiếu phạt: FINE
-- Đã loại bỏ due_date (không có hạn nộp phí trễ hạn), chỉ quản lý trạng thái Unpaid và Completed
CREATE TABLE FINE (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT NOT NULL,
    total_fine_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    payment_status ENUM('Unpaid', 'Completed') NOT NULL DEFAULT 'Unpaid',
    CONSTRAINT fk_fine_borrow
        FOREIGN KEY (borrow_id) REFERENCES BORROW(borrow_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng thanh toán tiền phạt: PAYMENT_TRANSACTION
-- Đã loại bỏ amount_paid và payment_method, chỉ ghi nhận thời điểm xác nhận thanh toán
CREATE TABLE PAYMENT_TRANSACTION (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    fine_id INT NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_fine
        FOREIGN KEY (fine_id) REFERENCES FINE(fine_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 6. NHẬT KÝ TRUY CẬP TÀI NGUYÊN SỐ (ACCESS_LOG)
-- Ràng buộc BR13: Tách rời Foreign Key để tối ưu hiệu năng ghi tốc độ cao.
-- Đã loại bỏ person_id (không theo dõi cá nhân, chỉ lưu ip_address),
-- loại bỏ access_timestamp, và loại bỏ thuộc tính download (chỉ đọc trực tuyến).
-- =============================================================================

CREATE TABLE ACCESS_LOG (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    journal_resource_id INT,
    ip_address VARCHAR(45),
    INDEX idx_access_log_journal (journal_resource_id)
) ENGINE = InnoDB;

-- =============================================================================
-- 7. SQL VIEW ĐỊNH DANH TÁC GIẢ & PHÂN BIỆT THÀNH VIÊN TRƯỜNG VS TÁC GIẢ NGOÀI
-- Giúp định danh trực quan ai là tác giả, và tác giả đó là UNIVERSITY_MEMBER hay EXTERNAL_AUTHOR
-- =============================================================================

CREATE OR REPLACE VIEW VIEW_AUTHOR_IDENTIFIER AS
SELECT 
    p.person_id,
    p.ssn,
    p.name,
    p.email,
    p.person_type AS registered_person_type,
    CASE 
        WHEN um.person_id IS NOT NULL THEN 'UNIVERSITY_MEMBER'
        WHEN ea.person_id IS NOT NULL THEN 'EXTERNAL_AUTHOR'
        ELSE 'GUEST_OR_OTHER'
    END AS author_affiliation_type,
    um.member_type AS internal_role,
    um.department,
    ea.affiliation AS external_affiliation,
    COUNT(DISTINCT ao.resource_id) AS total_published_resources
FROM PERSON p
INNER JOIN AUTHOR_OF ao ON p.person_id = ao.person_id
LEFT JOIN UNIVERSITY_MEMBER um ON p.person_id = um.person_id
LEFT JOIN EXTERNAL_AUTHOR ea ON p.person_id = ea.person_id
GROUP BY p.person_id, p.ssn, p.name, p.email, p.person_type, 
         um.person_id, um.member_type, um.department, ea.person_id, ea.affiliation;

-- =============================================================================
-- 8. STORED FUNCTION KIỂM TRA LICENSE (PHƯƠNG ÁN B: OR BÊN NGOÀI, AND BÊN TRONG)
-- Kiểm tra riêng trường hợp không có bản ghi RESOURCE_LICENSE để tránh lỗi double NOT EXISTS.
-- =============================================================================

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
