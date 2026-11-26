-- =============================================================================
-- DATABASE SCHEMA: Library Management System & Open Repository
-- Target: MySQL 8.0+
-- Revised ERD: independent member_id, PERSON identity, subtype-based RESOURCE.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS library_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE library_db;

-- Drop views/functions before rebuilding their base tables.
DROP VIEW IF EXISTS VIEW_AUTHOR_IDENTIFIER;
DROP VIEW IF EXISTS VIEW_MEMBERS_DETAILED;
DROP VIEW IF EXISTS VIEW_RESOURCES_DETAILED;
DROP VIEW IF EXISTS VIEW_BOOKS_DETAILED;
DROP VIEW IF EXISTS VIEW_JOURNALS_DETAILED;
DROP VIEW IF EXISTS VIEW_BORROWS_DETAILED;
DROP VIEW IF EXISTS VIEW_FINES_DETAILED;
DROP VIEW IF EXISTS VIEW_ACCESS_LOGS_DETAILED;
DROP VIEW IF EXISTS VIEW_GRANTED_LICENSES_DETAILED;
DROP FUNCTION IF EXISTS fn_has_all_required_licenses;

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
DROP TABLE IF EXISTS UNIVERSITY_MEMBER;
DROP TABLE IF EXISTS PERSON;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. PERSON AND UNIVERSITY_MEMBER
-- PERSON stores identity only. Membership is represented by a separate key.
-- =============================================================================
CREATE TABLE PERSON (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(15)
) ENGINE = InnoDB;

CREATE TABLE UNIVERSITY_MEMBER (
    member_id VARCHAR(20) PRIMARY KEY,
    person_id INT NOT NULL UNIQUE,
    member_type ENUM('STUDENT', 'FACULTY') NOT NULL,
    major VARCHAR(100) COMMENT 'Required for STUDENT; may also be populated for FACULTY',
    academic_rank VARCHAR(50) COMMENT 'Required for FACULTY; may also be populated for STUDENT',
    CONSTRAINT fk_univ_member_person
        FOREIGN KEY (person_id) REFERENCES PERSON(person_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_member_academic_profile CHECK (
        (member_type = 'STUDENT' AND major IS NOT NULL)
        OR
        (member_type = 'FACULTY' AND academic_rank IS NOT NULL)
    )
) ENGINE = InnoDB;

-- =============================================================================
-- 2. RESOURCE AND ITS SUBTYPES
-- No stored publish_date/resource_type. The subtype tables identify the kind.
-- =============================================================================
CREATE TABLE RESOURCE (
    resource_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(250) NOT NULL,
    topic VARCHAR(100),
    is_public BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Public flag; for anonymous journal Guest access TRUE allows access. Identified access follows Author/License rules.'
) ENGINE = InnoDB;

CREATE TABLE PHYSICAL_BOOK (
    resource_id INT PRIMARY KEY,
    shelf_location VARCHAR(50),
    CONSTRAINT fk_physical_book_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE DIGITAL_JOURNAL (
    resource_id INT PRIMARY KEY,
    url VARCHAR(250),
    file_size INT COMMENT 'File size in KB/MB, consistently defined by the application',
    format VARCHAR(10) COMMENT 'PDF, EPUB, XML, etc.',
    CONSTRAINT fk_digital_journal_resource
        FOREIGN KEY (resource_id) REFERENCES RESOURCE(resource_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE BOOK_COPY (
    copy_id VARCHAR(30) PRIMARY KEY,
    resource_id INT NOT NULL,
    availability_status ENUM('Available', 'Borrowed') NOT NULL DEFAULT 'Available',
    CONSTRAINT fk_book_copy_physical_book
        FOREIGN KEY (resource_id) REFERENCES PHYSICAL_BOOK(resource_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    INDEX idx_book_copy_resource_status (resource_id, availability_status)
) ENGINE = InnoDB;

-- =============================================================================
-- 3. LICENSES
-- =============================================================================
CREATE TABLE LICENSE_PACKAGE (
    license_id INT AUTO_INCREMENT PRIMARY KEY,
    license_name VARCHAR(150) NOT NULL,
    description TEXT
) ENGINE = InnoDB;

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
-- 4. AUTHORSHIP: any PERSON can be an author; no EXTERNAL_AUTHOR subtype.
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
        ON UPDATE CASCADE,
    INDEX idx_author_of_resource (resource_id)
) ENGINE = InnoDB;

-- =============================================================================
-- 5. BORROW, FINE, PAYMENT
-- BORROW references the independent UNIVERSITY_MEMBER.member_id.
-- person_id is obtained through UNIVERSITY_MEMBER for AUTHOR_OF checks.
-- =============================================================================
CREATE TABLE BORROW (
    borrow_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(20) NOT NULL,
    copy_id VARCHAR(30) NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE DEFAULT NULL,
    CONSTRAINT fk_borrow_member
        FOREIGN KEY (member_id) REFERENCES UNIVERSITY_MEMBER(member_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_borrow_copy
        FOREIGN KEY (copy_id) REFERENCES BOOK_COPY(copy_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    INDEX idx_borrow_member_return (member_id, return_date),
    INDEX idx_borrow_copy_return (copy_id, return_date)
) ENGINE = InnoDB;

CREATE TABLE FINE (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT NOT NULL,
    total_fine_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    payment_status ENUM('Unpaid', 'Completed') NOT NULL DEFAULT 'Unpaid',
    CONSTRAINT fk_fine_borrow
        FOREIGN KEY (borrow_id) REFERENCES BORROW(borrow_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_fine_borrow_status (borrow_id, payment_status)
) ENGINE = InnoDB;

CREATE TABLE PAYMENT_TRANSACTION (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    fine_id INT NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_fine
        FOREIGN KEY (fine_id) REFERENCES FINE(fine_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_payment_fine_date (fine_id, payment_date)
) ENGINE = InnoDB;

-- =============================================================================
-- 6. DIGITAL JOURNAL ACCESS LOG
-- One record per successful access. Optional PERSON identity; synthetic IP is generated per access.
-- =============================================================================
CREATE TABLE ACCESS_LOG (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NULL COMMENT 'PERSON ID supplied at access time; NULL for anonymous Guest',
    journal_resource_id INT NOT NULL,
    ip_address VARCHAR(45) NOT NULL
        COMMENT 'Synthetic random IPv4 generated by sp_access_digital_journal; not the client real IP',
    CONSTRAINT fk_access_log_person
        FOREIGN KEY (person_id) REFERENCES PERSON(person_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_access_log_digital_journal
        FOREIGN KEY (journal_resource_id) REFERENCES DIGITAL_JOURNAL(resource_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    INDEX idx_access_log_person (person_id),
    INDEX idx_access_log_journal (journal_resource_id)
) ENGINE = InnoDB;

-- =============================================================================
-- 7. HELPER FUNCTION: every required license must be granted to the PERSON.
-- A private resource with no required-license rows is deliberately not covered
-- by this function; callers must decide that case explicitly.
-- =============================================================================
DELIMITER //
CREATE FUNCTION fn_has_all_required_licenses(
    p_person_id INT,
    p_resource_id INT
)
RETURNS BOOLEAN
READS SQL DATA
BEGIN
    DECLARE v_required_count INT DEFAULT 0;
    DECLARE v_granted_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO v_required_count
    FROM RESOURCE_LICENSE
    WHERE resource_id = p_resource_id;

    IF v_required_count = 0 THEN
        RETURN FALSE;
    END IF;

    SELECT COUNT(DISTINCT rl.license_id)
      INTO v_granted_count
    FROM RESOURCE_LICENSE rl
    INNER JOIN GRANTED_LICENSE gl
        ON gl.license_id = rl.license_id
       AND gl.person_id = p_person_id
    WHERE rl.resource_id = p_resource_id;

    RETURN v_granted_count = v_required_count;
END//
DELIMITER ;

-- =============================================================================
-- 8. AUTHOR IDENTIFIER VIEW
-- Affiliation is derived from UNIVERSITY_MEMBER; all other PERSONs are nonmembers.
-- Authorship itself is determined only by AUTHOR_OF.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_AUTHOR_IDENTIFIER AS
SELECT
    p.person_id,
    p.name,
    p.email,
    um.member_id,
    um.member_type,
    CASE
        WHEN um.member_id IS NOT NULL THEN 'UNIVERSITY_MEMBER'
        ELSE 'NON_MEMBER'
    END AS affiliation_type,
    um.major,
    um.academic_rank,
    COUNT(DISTINCT ao.resource_id) AS total_published_resources
FROM PERSON p
INNER JOIN AUTHOR_OF ao
    ON ao.person_id = p.person_id
LEFT JOIN UNIVERSITY_MEMBER um
    ON um.person_id = p.person_id
GROUP BY
    p.person_id, p.name, p.email, um.member_id, um.member_type,
    um.major, um.academic_rank;
