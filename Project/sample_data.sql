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
(4, '001204000444', 'Lê Văn Cường', '789 Giải Phóng, Hà Nội', 'cuong.le@gmail.com', '0901234567', 'GUEST'),
(5, '001204000555', 'Dr. Robert Martin', 'Chicago, Illinois, USA', 'unclebob@cleancoder.com', '0905555555', 'EXTERNAL_AUTHOR'),
(6, '001204000666', 'ThS. Hoàng Minh Đức', '102 Nguyễn Trãi, Hà Nội', 'duc.hoang@university.edu.vn', '0918889999', 'FACULTY');

-- =============================================================================
-- 2. INSERT SUBCLASSES CỦA PERSON (UNIVERSITY_MEMBER & EXTERNAL_AUTHOR)
-- =============================================================================
-- Thành viên nội bộ trường (Sinh viên, Giảng viên)
INSERT INTO UNIVERSITY_MEMBER (person_id, member_id, department, member_type, major, academic_rank) VALUES
(1, 'SV2026001', 'Khoa Công nghệ Thông tin', 'STUDENT', 'Kỹ thuật Phần mềm', NULL),
(2, 'GV2026002', 'Khoa Toán - Tin ứng dụng', 'FACULTY', NULL, 'Phó Giáo Sư'),
(6, 'GV2026006', 'Khoa Hệ thống Thông tin', 'FACULTY', NULL, 'Thạc sĩ - Giảng viên chính');

-- Tác giả bên ngoài
INSERT INTO EXTERNAL_AUTHOR (person_id, affiliation, biography) VALUES
(3, 'MIT CSAIL & IEEE Fellow', 'Chuyên gia nghiên cứu về Hệ quản trị cơ sở dữ liệu phân tán và Trí tuệ nhân tạo.'),
(5, 'Clean Code Alliance & ACM Fellow', 'Chuyên gia quốc tế về Kiến trúc phần mềm và Kỹ thuật phần mềm sạch.');

-- =============================================================================
-- 3. INSERT RESOURCE (Superclass)
-- =============================================================================
INSERT INTO RESOURCE (resource_id, title, publish_date, topic, resource_type) VALUES
(1, 'Database System Concepts (7th Edition)', '2019-05-15', 'Computer Science', 'PHYSICAL_BOOK'),
(2, 'Clean Code: A Handbook of Agile Software Craftsmanship', '2008-08-01', 'Software Engineering', 'PHYSICAL_BOOK'),
(3, 'Deep Learning Architectures for Big Data Analytics', '2025-03-10', 'Artificial Intelligence', 'DIGITAL_JOURNAL'),
(4, 'ACM Computing Surveys: Distributed Transaction Processing', '2024-11-20', 'Distributed Systems', 'DIGITAL_JOURNAL'),
(5, 'Clean Architecture: Software Structure and Design', '2024-05-10', 'Software Engineering', 'DIGITAL_JOURNAL'),
(6, 'Cloud Database Scaling and Distributed Query Optimization', '2025-08-20', 'Database Systems', 'DIGITAL_JOURNAL'),
(7, 'Giáo trình Cơ sở Dữ liệu Nâng cao và Hệ phân tán', '2025-01-15', 'Computer Science', 'PHYSICAL_BOOK');

-- =============================================================================
-- 4. INSERT SUBCLASSES CỦA RESOURCE (PHYSICAL_BOOK & DIGITAL_JOURNAL)
-- Bỏ ISBN ở PHYSICAL_BOOK
-- =============================================================================
-- Sách in truyền thống
INSERT INTO PHYSICAL_BOOK (resource_id, shelf_location) VALUES
(1, 'Kệ A1-03-05'),
(2, 'Kệ B2-12-01'),
(7, 'Kệ C1-05-02');

-- Tạp chí, bài báo điện tử
INSERT INTO DIGITAL_JOURNAL (resource_id, doi, url, file_size, format, is_public) VALUES
(3, '10.1109/TPAMI.2025.1001', 'https://dl.library.edu/journals/deep-learning-2025.pdf', 4096, 'PDF', FALSE),
(4, '10.1145/3456789.3456790', 'https://dl.library.edu/journals/acm-distributed-tx.pdf', 2048, 'PDF', TRUE),
(5, '10.1007/978-0-13-449416-6', 'https://dl.library.edu/journals/clean-architecture-2024.pdf', 5120, 'PDF', FALSE),
(6, '10.1109/TKDE.2025.2006', 'https://dl.library.edu/journals/cloud-db-scaling-2025.pdf', 3584, 'PDF', FALSE);

-- =============================================================================
-- 5. INSERT BOOK_COPY (Bản sao vật lý của sách)
-- copy_id dạng VARCHAR đánh theo '<resource_id>_<index>'; Bỏ barcode & condition_status
-- =============================================================================
INSERT INTO BOOK_COPY (copy_id, resource_id, availability_status) VALUES
('1_1', 1, 'Borrowed'),
('1_2', 1, 'Available'),
('2_1', 2, 'Available'),
('2_2', 2, 'Borrowed'),
('7_1', 7, 'Available');

-- =============================================================================
-- 6. INSERT LICENSE_PACKAGE & RESOURCE_LICENSE (Gói bản quyền số)
-- =============================================================================
INSERT INTO LICENSE_PACKAGE (license_id, license_name, provider, description) VALUES
(1, 'IEEE Xplore Institutional Access Package', 'IEEE', 'Truy cập toàn văn bài báo và kỷ yếu hội nghị IEEE.'),
(2, 'ACM Digital Library University Bundle', 'ACM', 'Gói bài báo nghiên cứu khoa học máy tính ACM toàn diện.');

-- M:N: Tạp chí số thuộc các gói bản quyền
INSERT INTO RESOURCE_LICENSE (resource_id, license_id) VALUES
(3, 1), -- Deep Learning Journal thuộc gói IEEE
(4, 2), -- ACM Surveys thuộc gói ACM
(5, 2), -- Clean Architecture Journal thuộc gói ACM
(6, 1); -- Cloud Database Scaling thuộc gói IEEE

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
(3, 3), -- Prof. John Doe (EXTERNAL_AUTHOR) là tác giả bài báo Deep Learning (Journal 3)
(2, 1), -- TS. Trần Thị Bình (FACULTY, UNIVERSITY_MEMBER) là đồng tác giả sách Database Concepts (Book 1)
(5, 5), -- Dr. Robert Martin (EXTERNAL_AUTHOR) là tác giả bài báo Clean Architecture (Journal 5)
(6, 6), -- ThS. Hoàng Minh Đức (FACULTY, UNIVERSITY_MEMBER) là tác giả bài báo Cloud DB Scaling (Journal 6)
(6, 7); -- ThS. Hoàng Minh Đức (FACULTY, UNIVERSITY_MEMBER) là tác giả sách giáo trình CSDL Nâng cao (Book 7)

-- =============================================================================
-- 9. INSERT BORROW (Giao dịch mượn trả)
-- =============================================================================
INSERT INTO BORROW (borrow_id, member_person_id, copy_id, borrow_date, due_date, return_date) VALUES
-- Giao dịch 1: Đang mượn hợp lệ (Bản sao '1_1')
(1, 1, '1_1', '2026-10-01', '2026-10-21', NULL),

-- Giao dịch 2: Đã trả đúng hạn (Bản sao '2_1')
(2, 2, '2_1', '2026-09-01', '2026-10-01', '2026-09-25'),

-- Giao dịch 3: Trả sách trễ hạn (Bản sao '1_2' - Phát sinh tiền phạt - Dùng cho Test 03)
(3, 1, '1_2', '2026-09-01', '2026-09-15', '2026-09-20');

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
