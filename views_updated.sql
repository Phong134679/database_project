-- =============================================================================
-- SQL VIEWS - Library Management System & Open Repository
-- Target schema: 01_create_database_updated.sql
-- =============================================================================
USE library_db;

-- =============================================================================
-- 1. VIEW_MEMBERS_DETAILED
-- One row per UNIVERSITY_MEMBER. BORROW is grouped by member_id.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_MEMBERS_DETAILED AS
SELECT
    um.member_id,
    um.person_id,
    p.name AS member_name,
    p.email,
    p.phone,
    um.member_type,
    um.major,
    um.academic_rank,
    CASE WHEN um.member_type = 'FACULTY' THEN 30 ELSE 14 END AS loan_duration_days,
    CASE WHEN um.member_type = 'FACULTY' THEN 10 ELSE 3 END AS borrow_quota,
    COALESCE(bs.active_borrow_count, 0) AS active_borrow_count,
    COALESCE(bs.overdue_borrow_count, 0) AS overdue_borrow_count,
    COALESCE(fs.unpaid_fine_count, 0) AS unpaid_fine_count,
    CASE
        WHEN COALESCE(bs.overdue_borrow_count, 0) = 0
         AND COALESCE(fs.unpaid_fine_count, 0) = 0
         AND COALESCE(bs.active_borrow_count, 0) <
             CASE WHEN um.member_type = 'FACULTY' THEN 10 ELSE 3 END
        THEN TRUE ELSE FALSE
    END AS is_eligible
FROM UNIVERSITY_MEMBER um
INNER JOIN PERSON p
    ON p.person_id = um.person_id
LEFT JOIN (
    SELECT
        b.member_id,
        SUM(CASE WHEN b.return_date IS NULL THEN 1 ELSE 0 END) AS active_borrow_count,
        SUM(CASE
                WHEN b.return_date IS NULL AND b.due_date < CURDATE() THEN 1
                ELSE 0
            END) AS overdue_borrow_count
    FROM BORROW b
    GROUP BY b.member_id
) bs
    ON bs.member_id = um.member_id
LEFT JOIN (
    SELECT
        b.member_id,
        COUNT(*) AS unpaid_fine_count
    FROM FINE f
    INNER JOIN BORROW b ON b.borrow_id = f.borrow_id
    WHERE f.payment_status = 'Unpaid'
    GROUP BY b.member_id
) fs
    ON fs.member_id = um.member_id;

-- =============================================================================
-- 2. VIEW_RESOURCES_DETAILED
-- One row per RESOURCE. The subtype joins show data without RESOURCE.resource_type.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_RESOURCES_DETAILED AS
SELECT
    r.resource_id,
    r.title,
    r.topic,
    r.is_public,
    COALESCE(rl_agg.required_license_count, 0) AS required_license_count,
    pb.shelf_location,
    COALESCE(bc_agg.total_copies, 0) AS total_copies,
    COALESCE(bc_agg.available_copies, 0) AS available_copies,
    dj.url AS journal_url,
    dj.file_size,
    dj.format AS journal_format
FROM RESOURCE r
LEFT JOIN PHYSICAL_BOOK pb
    ON pb.resource_id = r.resource_id
LEFT JOIN DIGITAL_JOURNAL dj
    ON dj.resource_id = r.resource_id
LEFT JOIN (
    SELECT resource_id, COUNT(*) AS required_license_count
    FROM RESOURCE_LICENSE
    GROUP BY resource_id
) rl_agg
    ON rl_agg.resource_id = r.resource_id
LEFT JOIN (
    SELECT
        resource_id,
        COUNT(*) AS total_copies,
        SUM(CASE WHEN availability_status = 'Available' THEN 1 ELSE 0 END) AS available_copies
    FROM BOOK_COPY
    GROUP BY resource_id
) bc_agg
    ON bc_agg.resource_id = r.resource_id;

-- =============================================================================
-- 3. VIEW_BOOKS_DETAILED
-- One row per physical book copy. No removed publication date field.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_BOOKS_DETAILED AS
SELECT
    bc.copy_id,
    bc.availability_status,
    r.resource_id,
    r.title,
    r.topic,
    r.is_public,
    pb.shelf_location,
    COALESCE(rl_agg.required_license_count, 0) AS required_license_count
FROM BOOK_COPY bc
INNER JOIN PHYSICAL_BOOK pb
    ON pb.resource_id = bc.resource_id
INNER JOIN RESOURCE r
    ON r.resource_id = pb.resource_id
LEFT JOIN (
    SELECT resource_id, COUNT(*) AS required_license_count
    FROM RESOURCE_LICENSE
    GROUP BY resource_id
) rl_agg
    ON rl_agg.resource_id = r.resource_id;

-- =============================================================================
-- 4. VIEW_JOURNALS_DETAILED
-- One row per DIGITAL_JOURNAL. The subtype table confirms journal identity.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_JOURNALS_DETAILED AS
SELECT
    r.resource_id,
    r.title,
    r.topic,
    r.is_public,
    dj.url,
    dj.file_size,
    dj.format,
    COALESCE(rl_agg.required_license_count, 0) AS required_license_count
FROM DIGITAL_JOURNAL dj
INNER JOIN RESOURCE r
    ON r.resource_id = dj.resource_id
LEFT JOIN (
    SELECT resource_id, COUNT(*) AS required_license_count
    FROM RESOURCE_LICENSE
    GROUP BY resource_id
) rl_agg
    ON rl_agg.resource_id = r.resource_id;

-- =============================================================================
-- 5. VIEW_BORROWS_DETAILED
-- One row per BORROW. Member identity is resolved through member_id -> person_id.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_BORROWS_DETAILED AS
SELECT
    b.borrow_id,
    b.member_id,
    um.person_id,
    p.name AS member_name,
    um.member_type,
    um.major,
    um.academic_rank,
    b.copy_id,
    bc.availability_status,
    r.resource_id,
    r.title AS book_title,
    r.is_public,
    pb.shelf_location,
    b.borrow_date,
    b.due_date,
    b.return_date,
    CASE
        WHEN b.return_date IS NULL AND b.due_date < CURDATE() THEN TRUE
        ELSE FALSE
    END AS is_overdue,
    CASE
        WHEN b.return_date IS NOT NULL THEN 'Returned'
        WHEN b.due_date < CURDATE() THEN 'Overdue'
        ELSE 'Active'
    END AS status,
    f_latest.fine_id,
    f_latest.total_fine_amount AS fine_amount,
    f_latest.payment_status AS fine_status
FROM BORROW b
INNER JOIN UNIVERSITY_MEMBER um
    ON um.member_id = b.member_id
INNER JOIN PERSON p
    ON p.person_id = um.person_id
INNER JOIN BOOK_COPY bc
    ON bc.copy_id = b.copy_id
INNER JOIN PHYSICAL_BOOK pb
    ON pb.resource_id = bc.resource_id
INNER JOIN RESOURCE r
    ON r.resource_id = pb.resource_id
LEFT JOIN (
    SELECT fine_id, borrow_id, total_fine_amount, payment_status
    FROM (
        SELECT
            f.fine_id,
            f.borrow_id,
            f.total_fine_amount,
            f.payment_status,
            ROW_NUMBER() OVER (
                PARTITION BY f.borrow_id
                ORDER BY f.fine_id DESC
            ) AS rn
        FROM FINE f
    ) ranked
    WHERE rn = 1
) f_latest
    ON f_latest.borrow_id = b.borrow_id;

-- =============================================================================
-- 6. VIEW_FINES_DETAILED
-- One row per FINE. Multiple fine rows per borrow are supported.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_FINES_DETAILED AS
SELECT
    f.fine_id,
    f.borrow_id,
    b.member_id,
    um.person_id,
    p.name AS member_name,
    um.member_type,
    b.copy_id,
    r.title AS book_title,
    b.borrow_date,
    b.due_date,
    b.return_date,
    f.total_fine_amount,
    f.payment_status,
    pt_latest.transaction_id,
    pt_latest.payment_date
FROM FINE f
INNER JOIN BORROW b
    ON b.borrow_id = f.borrow_id
INNER JOIN UNIVERSITY_MEMBER um
    ON um.member_id = b.member_id
INNER JOIN PERSON p
    ON p.person_id = um.person_id
INNER JOIN BOOK_COPY bc
    ON bc.copy_id = b.copy_id
INNER JOIN RESOURCE r
    ON r.resource_id = bc.resource_id
LEFT JOIN (
    SELECT transaction_id, fine_id, payment_date
    FROM (
        SELECT
            pt.transaction_id,
            pt.fine_id,
            pt.payment_date,
            ROW_NUMBER() OVER (
                PARTITION BY pt.fine_id
                ORDER BY pt.payment_date DESC, pt.transaction_id DESC
            ) AS rn
        FROM PAYMENT_TRANSACTION pt
    ) ranked
    WHERE rn = 1
) pt_latest
    ON pt_latest.fine_id = f.fine_id;

-- =============================================================================
-- 7. VIEW_ACCESS_LOGS_DETAILED
-- No removed RESOURCE.resource_type field is referenced.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_ACCESS_LOGS_DETAILED AS
SELECT
    l.log_id,
    l.journal_resource_id,
    l.journal_resource_id AS resource_id,
    r.title AS journal_title,
    r.is_public,
    dj.url AS journal_url,
    dj.format AS journal_format,
    l.ip_address
FROM ACCESS_LOG l
LEFT JOIN RESOURCE r
    ON r.resource_id = l.journal_resource_id
LEFT JOIN DIGITAL_JOURNAL dj
    ON dj.resource_id = l.journal_resource_id;

-- =============================================================================
-- 8. VIEW_GRANTED_LICENSES_DETAILED
-- No removed PERSON.person_type or LICENSE_PACKAGE.provider fields.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_GRANTED_LICENSES_DETAILED AS
SELECT
    gl.grant_id,
    gl.person_id,
    p.name AS person_name,
    um.member_id,
    um.member_type,
    gl.license_id,
    lp.license_name,
    lp.description
FROM GRANTED_LICENSE gl
INNER JOIN PERSON p
    ON p.person_id = gl.person_id
INNER JOIN LICENSE_PACKAGE lp
    ON lp.license_id = gl.license_id
LEFT JOIN UNIVERSITY_MEMBER um
    ON um.person_id = gl.person_id;
