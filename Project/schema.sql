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
    resource_type ENUM('PHYSICAL_BOOK', 'DIGITAL_JOURNAL') NOT NULL
) ENGINE = InnoDB;

-- Bảng con: PHYSICAL_BOOK (Sách in truyền thống)
CREATE TABLE PHYSICAL_BOOK (
    resource_id INT PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    shelf_location VARCHAR(50),
    CONSTRAINT fk_physical_book_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng con: DIGITAL_JOURNAL (Bài báo, tạp chí điện tử)
CREATE TABLE DIGITAL_JOURNAL (
    resource_id INT PRIMARY KEY,
    doi VARCHAR(100) NOT NULL UNIQUE,
    url VARCHAR(250),
    file_size INT COMMENT 'Kích thước file tính theo KB/MB',
    format VARCHAR(10) COMMENT 'Định dạng PDF, EPUB, XML',
    CONSTRAINT fk_digital_journal_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng bản sao sách: BOOK_COPY (Mỗi cuốn sách vật lý có thể có nhiều bản sao)
CREATE TABLE BOOK_COPY (
    copy_id INT AUTO_INCREMENT PRIMARY KEY,
    resource_id INT NOT NULL,
    barcode VARCHAR(50) NOT NULL UNIQUE,
    condition_status VARCHAR(50) NOT NULL DEFAULT 'Good',
    availability_status VARCHAR(20) NOT NULL DEFAULT 'Available',
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

-- Bảng cấp quyền: GRANTED_LICENSE (Phân quyền truy cập tài liệu số cho người dùng)
CREATE TABLE GRANTED_LICENSE (
    grant_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    license_id INT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    CONSTRAINT fk_granted_license_person
        FOREIGN KEY (person_id) REFERENCES PERSON(person_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_granted_license_package
        FOREIGN KEY (license_id) REFERENCES LICENSE_PACKAGE(license_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng kết hợp N:M: RESOURCE_LICENSE (Tài nguyên số thuộc gói bản quyền nào)
CREATE TABLE RESOURCE_LICENSE (
    resource_id INT NOT NULL,
    license_id INT NOT NULL,
    PRIMARY KEY (resource_id, license_id),
    CONSTRAINT fk_reslic_journal
        FOREIGN KEY (resource_id) REFERENCES DIGITAL_JOURNAL(resource_id)
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
    copy_id INT NOT NULL,
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
CREATE TABLE FINE (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT NOT NULL,
    total_fine_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    payment_status ENUM('Unpaid', 'Partially Paid', 'Completed') NOT NULL DEFAULT 'Unpaid',
    due_date DATE,
    CONSTRAINT fk_fine_borrow
        FOREIGN KEY (borrow_id) REFERENCES BORROW(borrow_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Bảng thanh toán tiền phạt: PAYMENT_TRANSACTION (Hỗ trợ trả góp nhiều lần)
CREATE TABLE PAYMENT_TRANSACTION (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    fine_id INT NOT NULL,
    amount_paid DECIMAL(10, 2) NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50) NOT NULL,
    CONSTRAINT fk_payment_fine
        FOREIGN KEY (fine_id) REFERENCES FINE(fine_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =============================================================================
-- 6. NHẬT KÝ TRUY CẬP TÀI NGUYÊN SỐ (ACCESS_LOG)
-- Ràng buộc BR13: Tách rời Foreign Key để tối ưu hiệu năng ghi tốc độ cao.
-- Sử dụng Index độc lập để truy vấn thay vì ràng buộc Database Engine FK.
-- =============================================================================

CREATE TABLE ACCESS_LOG (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id INT,
    journal_resource_id INT,
    access_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    download_success BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_access_log_person (person_id),
    INDEX idx_access_log_journal (journal_resource_id),
    INDEX idx_access_log_timestamp (access_timestamp)
) ENGINE = InnoDB;
