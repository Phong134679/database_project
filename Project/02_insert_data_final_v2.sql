-- =============================================================================
-- SAMPLE DATA: Library Management System & Open Repository
-- Target schema: 01_create_database_updated.sql
-- Run once after creating the schema. Explicit IDs are used for reproducibility.
-- =============================================================================

USE library_db;
SET NAMES utf8mb4;

START TRANSACTION;

-- =============================================================================
-- 1. PERSON
-- No ssn, address or person_type. Author identity comes from AUTHOR_OF.
-- =============================================================================
INSERT INTO PERSON (person_id, name, email, phone) VALUES
    (1,  'Nguyễn Văn An',       'an.nguyen@university.edu.vn',    '0912345678'),
    (2,  'TS. Trần Thị Bình',    'binh.tran@university.edu.vn',   '0987654321'),
    (3,  'Prof. John Doe',       'johndoe@mit.edu',              '0123456789'),
    (4,  'Lê Văn Cường',         'cuong.le@gmail.com',           '0901234567'),
    (5,  'Dr. Robert Martin',    'unclebob@cleancoder.com',      '0905555555'),
    (6,  'ThS. Hoàng Minh Đức',  'duc.hoang@university.edu.vn',  '0918889999'),
    (7,  'Phạm Thu Hà',          'ha.pham@university.edu.vn',    '0917000007'),
    (8,  'Nguyễn Quốc Huy',      'huy.nguyen@university.edu.vn', '0918000008'),
    (9,  'Dr. Maria Garcia',     'maria.garcia@example.org',     '0909000009'),
    (10, 'Võ Thanh Lan',         'lan.vo@example.com',           '0910000010');

-- =============================================================================
-- 2. UNIVERSITY_MEMBER
-- member_id is independent from person_id. Both profile attributes may coexist.
-- =============================================================================
INSERT INTO UNIVERSITY_MEMBER
    (member_id, person_id, member_type, major, academic_rank)
VALUES
    ('SV2026001', 1, 'STUDENT', 'Kỹ thuật Phần mềm', NULL),
    ('GV2026002', 2, 'FACULTY', 'Toán - Tin ứng dụng', 'Phó Giáo Sư'),
    ('GV2026006', 6, 'FACULTY', 'Hệ thống Thông tin', 'Thạc sĩ - Giảng viên chính'),
    ('SV2026007', 7, 'STUDENT', 'Khoa học Máy tính', 'Trợ giảng'),
    ('SV2026008', 8, 'STUDENT', 'Hệ thống Thông tin', NULL);

-- =============================================================================
-- 3. RESOURCE
-- RESOURCE no longer stores publish_date/resource_type. Child table indicates kind.
-- =============================================================================
INSERT INTO RESOURCE (resource_id, title, topic, is_public) VALUES
    (1,  'Database System Concepts (7th Edition)', 'Computer Science', TRUE),
    (2,  'Clean Code: A Handbook of Agile Software Craftsmanship', 'Software Engineering', FALSE),
    (3,  'Deep Learning Architectures for Big Data Analytics', 'Artificial Intelligence', FALSE),
    (4,  'ACM Computing Surveys: Distributed Transaction Processing', 'Distributed Systems', TRUE),
    (5,  'Clean Architecture: Software Structure and Design', 'Software Engineering', FALSE),
    (6,  'Cloud Database Scaling and Distributed Query Optimization', 'Database Systems', FALSE),
    (7,  'Giáo trình Cơ sở Dữ liệu Nâng cao và Hệ phân tán', 'Computer Science', TRUE),
    (8,  'Designing Data-Intensive Applications', 'Database Systems', FALSE),
    (9,  'Practical Machine Learning Systems', 'Machine Learning', FALSE),
    (10, 'Open Research Data: Principles and Practice', 'Open Science', TRUE);

-- =============================================================================
-- 4. RESOURCE SUBTYPES
-- =============================================================================
INSERT INTO PHYSICAL_BOOK (resource_id, shelf_location) VALUES
    (1, 'Kệ A1-03-05'),
    (2, 'Kệ B2-12-01'),
    (7, 'Kệ C1-05-02'),
    (8, 'Kệ A2-08-04');

INSERT INTO DIGITAL_JOURNAL (resource_id, url, file_size, format) VALUES
    (3,  'https://dl.library.edu/journals/deep-learning-2025.pdf',          4096, 'PDF'),
    (4,  'https://dl.library.edu/journals/acm-distributed-tx.pdf',          2048, 'PDF'),
    (5,  'https://dl.library.edu/journals/clean-architecture-2024.pdf',     5120, 'PDF'),
    (6,  'https://dl.library.edu/journals/cloud-db-scaling-2025.pdf',       3584, 'PDF'),
    (7,  'https://dl.library.edu/journals/csdl-nang-cao-he-phan-tan.pdf',   4608, 'PDF'),
    (9,  'https://dl.library.edu/journals/practical-ml-systems.pdf',        6144, 'PDF'),
    (10, 'https://dl.library.edu/journals/open-research-data.pdf',          3072, 'PDF');

-- =============================================================================
-- 5. BOOK_COPY
-- '2_3' is borrowed by the author of resource 2 to demonstrate license bypass.
-- =============================================================================
INSERT INTO BOOK_COPY (copy_id, resource_id, availability_status) VALUES
    ('1_1', 1, 'Borrowed'),
    ('1_2', 1, 'Available'),
    ('1_3', 1, 'Available'),
    ('2_1', 2, 'Available'),
    ('2_2', 2, 'Borrowed'),
    ('2_3', 2, 'Borrowed'),
    ('7_1', 7, 'Available'),
    ('7_2', 7, 'Available'),
    ('8_1', 8, 'Borrowed'),
    ('8_2', 8, 'Available'),
    ('8_3', 8, 'Available');

-- =============================================================================
-- 6. LICENSE_PACKAGE / RESOURCE_LICENSE
-- =============================================================================
INSERT INTO LICENSE_PACKAGE (license_id, license_name, description) VALUES
    (1, 'IEEE Xplore Institutional Access Package',
        'Truy cập toàn văn bài báo và kỷ yếu hội nghị IEEE.'),
    (2, 'ACM Digital Library University Bundle',
        'Gói truy cập các bài báo nghiên cứu khoa học máy tính ACM.'),
    (3, 'Springer Nature eBook Collection',
        'Gói truy cập sách điện tử và tài liệu học thuật Springer Nature.');

INSERT INTO RESOURCE_LICENSE (resource_id, license_id) VALUES
    (2, 2),
    (3, 1),
    (5, 1),
    (5, 2),
    (8, 3),
    (9, 1),
    (9, 3);

-- =============================================================================
-- 7. GRANTED_LICENSE
-- =============================================================================
INSERT INTO GRANTED_LICENSE (grant_id, person_id, license_id) VALUES
    (1, 1, 1), -- An: IEEE only; is author of private physical book #2
    (2, 2, 2), -- Binh: ACM
    (3, 6, 1), -- Duc: IEEE
    (4, 6, 2), -- Duc: ACM
    (5, 7, 3), -- Ha: Springer
    (6, 8, 1), -- Huy: IEEE
    (7, 8, 2), -- Huy: ACM
    (8, 8, 3); -- Huy: Springer

-- =============================================================================
-- 8. AUTHOR_OF
-- Any PERSON may be an author; no EXTERNAL_AUTHOR entity is required.
-- Person 1 authored private physical book #2 and therefore bypasses its license.
-- =============================================================================
INSERT INTO AUTHOR_OF (person_id, resource_id) VALUES
    (3, 3),
    (2, 1),
    (5, 5),
    (6, 6),
    (6, 7),
    (7, 8),
    (9, 9),
    (2, 9),
    (10, 10),
    (1, 2),
    (8, 3);

-- =============================================================================
-- 9. BORROW
-- member_id is the transaction key; the person identity is reached via MEMBER.
-- Borrow #7 demonstrates author bypass: member SV2026001 lacks license #2 but
-- authored resource #2. Quota remains respected (2 of 3 active loans).
-- =============================================================================
INSERT INTO BORROW
    (borrow_id, member_id, copy_id, borrow_date, due_date, return_date)
VALUES
    (1, 'SV2026001', '1_1', '2026-11-20', '2026-12-04', NULL),
    (2, 'GV2026002', '2_1', '2026-09-01', '2026-10-01', '2026-09-25'),
    (3, 'SV2026001', '1_2', '2026-09-01', '2026-09-15', '2026-09-20'),
    (4, 'SV2026007', '7_1', '2026-10-01', '2026-10-15', '2026-10-20'),
    (5, 'SV2026008', '8_1', '2026-11-20', '2026-12-04', NULL),
    (6, 'GV2026006', '2_2', '2026-11-01', '2026-12-01', NULL),
    (7, 'SV2026001', '2_3', '2026-11-21', '2026-12-05', NULL);

-- =============================================================================
-- 10. FINE / PAYMENT_TRANSACTION
-- Fine #1 remains unpaid for member SV2026007.
-- Fine #2 is completed for member SV2026001; this member has no unpaid fine.
-- =============================================================================
INSERT INTO FINE (fine_id, borrow_id, total_fine_amount, payment_status) VALUES
    (1, 4, 50000.00, 'Unpaid'),
    (2, 3, 75000.00, 'Completed');

INSERT INTO PAYMENT_TRANSACTION (transaction_id, fine_id, payment_date) VALUES
    (1, 2, '2026-09-21 10:30:00');

-- =============================================================================
-- 11. ACCESS_LOG
-- Intentionally starts empty. Every row and synthetic IP is created at runtime
-- by sp_access_digital_journal after a successful Access action.
-- person_id is NULL for Guest and populated for a valid identified PERSON.
-- =============================================================================

COMMIT;
