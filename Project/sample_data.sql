-- =============================================================================
-- SAMPLE DATA: Library Management System & Open Repository
-- Purpose: Insert mock data to validate schema, relationships, and business rules.
-- Covers scenarios: Inheritance, Borrow/Return, Overdue Fine, Installment Payment,
--                   License Access, and Access Logs.
-- =============================================================================

USE library_db;

-- Tắt kiểm tra khóa ngoại để dọn dẹp dữ liệu cũ (nếu có)
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE ACCESS_LOG;
TRUNCATE TABLE PAYMENT_TRANSACTION;
TRUNCATE TABLE FINE;
TRUNCATE TABLE BORROW;
TRUNCATE TABLE BOOK_COPY;
TRUNCATE TABLE RESOURCE_LICENSE;
TRUNCATE TABLE AUTHOR_OF;
TRUNCATE TABLE GRANTED_LICENSE;
TRUNCATE TABLE LICENSE_PACKAGE;
TRUNCATE TABLE DIGITAL_JOURNAL;
TRUNCATE TABLE PHYSICAL_BOOK;
TRUNCATE TABLE RESOURCE;
TRUNCATE TABLE EXTERNAL_AUTHOR;
TRUNCATE TABLE UNIVERSITY_MEMBER;
TRUNCATE TABLE PERSON;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. INSERT PERSON (Superclass)
-- =============================================================================
INSERT INTO PERSON (person_id, ssn, name, address, email, phone, person_type) VALUES
(1, '001204000111', 'Nguyễn Văn An', '123 Cầu Giấy, Hà Nội', 'an.nguyen@university.edu.vn', '0912345678', 'STUDENT'),
(2, '001204000222', 'TS. Trần Thị Bình', '456 Hoàng Quốc Việt, Hà Nội', 'binh.tran@university.edu.vn', '0987654321', 'FACULTY'),
(3, '001204000333', 'Prof. John Doe', 'Cambridge, Massachusetts, USA', 'johndoe@mit.edu', '0123456789', 'EXTERNAL_AUTHOR'),
(4, '001204000444', 'Lê Văn Cường', '789 Giải Phóng, Hà Nội', 'cuong.le@gmail.com', '0901234567', 'GUEST');

-- =============================================================================
-- 2. INSERT SUBCLASSES CỦA PERSON (UNIVERSITY_MEMBER & EXTERNAL_AUTHOR)
-- =============================================================================
-- Thành viên nội bộ trường (Sinh viên, Giảng viên)
INSERT INTO UNIVERSITY_MEMBER (person_id, member_id, department, member_type, major, academic_rank) VALUES
(1, 'SV2026001', 'Khoa Công nghệ Thông tin', 'STUDENT', 'Kỹ thuật Phần mềm', NULL),
(2, 'GV2026002', 'Khoa Toán - Tin ứng dụng', 'FACULTY', NULL, 'Phó Giáo Sư');

-- Tác giả bên ngoài
INSERT INTO EXTERNAL_AUTHOR (person_id, affiliation, biography) VALUES
(3, 'MIT CSAIL & IEEE Fellow', 'Chuyên gia nghiên cứu về Hệ quản trị cơ sở dữ liệu phân tán và Trí tuệ nhân tạo.');

-- =============================================================================
-- 3. INSERT RESOURCE (Superclass)
-- =============================================================================
INSERT INTO RESOURCE (resource_id, title, publish_date, topic, resource_type) VALUES
(1, 'Database System Concepts (7th Edition)', '2019-05-15', 'Computer Science', 'PHYSICAL_BOOK'),
(2, 'Clean Code: A Handbook of Agile Software Craftsmanship', '2008-08-01', 'Software Engineering', 'PHYSICAL_BOOK'),
(3, 'Deep Learning Architectures for Big Data Analytics', '2025-03-10', 'Artificial Intelligence', 'DIGITAL_JOURNAL'),
(4, 'ACM Computing Surveys: Distributed Transaction Processing', '2024-11-20', 'Distributed Systems', 'DIGITAL_JOURNAL');

-- =============================================================================
-- 4. INSERT SUBCLASSES CỦA RESOURCE (PHYSICAL_BOOK & DIGITAL_JOURNAL)
-- =============================================================================
-- Sách in truyền thống
INSERT INTO PHYSICAL_BOOK (resource_id, isbn, shelf_location) VALUES
(1, '978-0078022159', 'Kệ A1-03-05'),
(2, '978-0132350884', 'Kệ B2-12-01');

-- Tạp chí, bài báo điện tử
INSERT INTO DIGITAL_JOURNAL (resource_id, doi, url, file_size, format) VALUES
(3, '10.1109/TPAMI.2025.1001', 'https://dl.library.edu/journals/deep-learning-2025.pdf', 4096, 'PDF'),
(4, '10.1145/3456789.3456790', 'https://dl.library.edu/journals/acm-distributed-tx.pdf', 2048, 'PDF');

-- =============================================================================
-- 5. INSERT BOOK_COPY (Bản sao vật lý của sách)
-- =============================================================================
INSERT INTO BOOK_COPY (copy_id, resource_id, barcode, condition_status, availability_status) VALUES
(1, 1, 'BC-DB-001', 'Good', 'Borrowed'),
(2, 1, 'BC-DB-002', 'Good', 'Available'),
(3, 2, 'BC-CC-001', 'Good', 'Available'),
(4, 2, 'BC-CC-002', 'Torn Cover', 'Maintenance');

-- =============================================================================
-- 6. INSERT LICENSE_PACKAGE & RESOURCE_LICENSE (Gói bản quyền số)
-- =============================================================================
INSERT INTO LICENSE_PACKAGE (license_id, license_name, provider, description) VALUES
(1, 'IEEE Xplore Institutional Access Package', 'IEEE', 'Truy cập toàn văn bài báo và kỷ yếu hội nghị IEEE.'),
(2, 'ACM Digital Library University Bundle', 'ACM', 'Gói bài báo nghiên cứu khoa học máy tính ACM toàn diện.');

-- M:N: Tạp chí số thuộc các gói bản quyền
INSERT INTO RESOURCE_LICENSE (resource_id, license_id) VALUES
(3, 1), -- Deep Learning Journal thuộc gói IEEE
(4, 2); -- ACM Surveys thuộc gói ACM

-- =============================================================================
-- 7. INSERT GRANTED_LICENSE (Cấp quyền truy cập cho người dùng)
-- =============================================================================
INSERT INTO GRANTED_LICENSE (grant_id, person_id, license_id, valid_from, valid_to) VALUES
(1, 1, 1, '2026-01-01', '2026-12-31'), -- Sinh viên Nguyễn Văn An có quyền truy cập IEEE (Còn hạn)
(2, 2, 2, '2026-01-01', '2027-12-31'), -- TS Bình có quyền truy cập ACM (Còn hạn)
(3, 4, 1, '2025-01-01', '2025-06-30'); -- Khách vãng lai Lê Văn Cường từng có gói IEEE (Đã hết hạn - Dùng cho Test 02)

-- =============================================================================
-- 8. INSERT AUTHOR_OF (Quan hệ tác giả - tài nguyên)
-- =============================================================================
INSERT INTO AUTHOR_OF (person_id, resource_id) VALUES
(3, 3), -- Prof. John Doe là tác giả bài báo Deep Learning
(2, 1); -- TS. Trần Thị Bình là đồng tác giả/dịch giả sách Database Concepts

-- =============================================================================
-- 9. INSERT BORROW (Giao dịch mượn trả)
-- =============================================================================
INSERT INTO BORROW (borrow_id, member_person_id, copy_id, borrow_date, due_date, return_date) VALUES
-- Giao dịch 1: Đang mượn hợp lệ
(1, 1, 1, '2026-10-01', '2026-10-21', NULL),

-- Giao dịch 2: Đã trả đúng hạn
(2, 2, 3, '2026-09-01', '2026-10-01', '2026-09-25'),

-- Giao dịch 3: Trả sách trễ hạn (Phát sinh tiền phạt - Dùng cho Test 03)
(3, 1, 2, '2026-09-01', '2026-09-15', '2026-09-20');

-- =============================================================================
-- 10. INSERT FINE & PAYMENT_TRANSACTION (Xử lý vi phạm và trả góp tiền phạt)
-- =============================================================================
-- Phiếu phạt cho giao dịch số 3 (trả trễ 5 ngày)
INSERT INTO FINE (fine_id, borrow_id, total_fine_amount, payment_status, due_date) VALUES
(1, 3, 50000.00, 'Partially Paid', '2026-10-05');

-- Giao dịch trả tiền đợt 1 (Đã trả 20,000 VND / 50,000 VND - Dùng cho Test 04 & BR11)
INSERT INTO PAYMENT_TRANSACTION (transaction_id, fine_id, amount_paid, payment_date, payment_method) VALUES
(1, 1, 20000.00, '2026-09-22 10:30:00', 'Cash');

-- =============================================================================
-- 11. INSERT ACCESS_LOG (Nhật ký truy cập tài nguyên số)
-- Ghi nhận lượt tải thành công (có bản quyền) và thất bại (hết hạn bản quyền)
-- =============================================================================
INSERT INTO ACCESS_LOG (log_id, person_id, journal_resource_id, access_timestamp, ip_address, download_success) VALUES
(1, 1, 3, '2026-10-10 14:15:20', '192.168.1.50', TRUE),
(2, 4, 3, '2026-10-12 09:00:00', '192.168.1.100', FALSE);
