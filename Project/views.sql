-- =============================================================================
-- SQL VIEWS: Library Management System & Open Repository
-- Purpose: Replace N+1 Java queries with pre-aggregated relational views
-- Target Engine: MySQL 8.4+
-- =============================================================================

USE library_db;

-- =============================================================================
-- 1. VIEW_MEMBERS_DETAILED
-- Grain: 1 row per PERSON
-- Aggregates BORROW and FINE separately before joining to prevent 1:N fan-out.
-- Note: is_eligible is for UI display only; sp_borrow_book still enforces rules.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_MEMBERS_DETAILED AS
SELECT
    p.person_id,
    p.ssn,
    p.name,
    p.address,
    p.email,
    p.phone,
    p.person_type,
    um.member_id,
    um.department,
    um.member_type,
    um.member_type AS membership_type,
    um.major,
    um.academic_rank,
    CASE
        WHEN p.person_type = 'FACULTY' THEN 30
        WHEN p.person_type = 'STUDENT' THEN 14
        ELSE 0
    END AS loan_duration_days,
    CASE
        WHEN p.person_type = 'FACULTY' THEN 10
        WHEN p.person_type = 'STUDENT' THEN 3
        ELSE 0
    END AS borrow_quota,
    COALESCE(bs.active_borrow_count, 0) AS active_borrow_count,
    COALESCE(bs.overdue_borrow_count, 0) AS overdue_borrow_count,
    COALESCE(fs.unpaid_fine_count, 0) AS unpaid_fine_count,
    CASE
        WHEN p.person_type IN ('STUDENT', 'FACULTY')
             AND COALESCE(bs.overdue_borrow_count, 0) = 0
             AND COALESCE(fs.unpaid_fine_count, 0) = 0
             AND COALESCE(bs.active_borrow_count, 0) < (
                 CASE WHEN p.person_type = 'FACULTY' THEN 10 ELSE 3 END
             )
        THEN TRUE
        ELSE FALSE
    END AS is_eligible
FROM PERSON p
LEFT JOIN UNIVERSITY_MEMBER um
    ON p.person_id = um.person_id
LEFT JOIN (
    SELECT
        b.member_person_id,
        SUM(CASE WHEN b.return_date IS NULL THEN 1 ELSE 0 END) AS active_borrow_count,
        SUM(CASE WHEN b.return_date IS NULL AND b.due_date < CURDATE() THEN 1 ELSE 0 END) AS overdue_borrow_count
    FROM BORROW b
    GROUP BY b.member_person_id
) bs
    ON p.person_id = bs.member_person_id
LEFT JOIN (
    SELECT
        b.member_person_id,
        COUNT(*) AS unpaid_fine_count
    FROM FINE f
    INNER JOIN BORROW b ON f.borrow_id = b.borrow_id
    WHERE f.payment_status <> 'Completed'
    GROUP BY b.member_person_id
) fs
    ON p.person_id = fs.member_person_id;

-- =============================================================================
-- 2. VIEW_RESOURCES_DETAILED
-- Grain: 1 row per RESOURCE
-- Aggregates RESOURCE_LICENSE and BOOK_COPY separately to prevent fan-out.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_RESOURCES_DETAILED AS
SELECT
    r.resource_id,
    r.title,
    r.publish_date,
    r.topic,
    r.resource_type,
    r.is_public,
    COALESCE(rl_agg.required_license_count, 0) AS required_license_count,
    pb.isbn,
    pb.shelf_location,
    COALESCE(bc_agg.total_copies, 0) AS total_copies,
    COALESCE(bc_agg.available_copies, 0) AS available_copies,
    dj.doi,
    dj.url,
    dj.file_size,
    dj.format
FROM RESOURCE r
LEFT JOIN PHYSICAL_BOOK pb
    ON r.resource_id = pb.resource_id
LEFT JOIN DIGITAL_JOURNAL dj
    ON r.resource_id = dj.resource_id
LEFT JOIN (
    SELECT
        rl.resource_id,
        COUNT(*) AS required_license_count
    FROM RESOURCE_LICENSE rl
    GROUP BY rl.resource_id
) rl_agg
    ON r.resource_id = rl_agg.resource_id
LEFT JOIN (
    SELECT
        bc.resource_id,
        COUNT(*) AS total_copies,
        SUM(CASE WHEN bc.availability_status = 'Available' THEN 1 ELSE 0 END) AS available_copies
    FROM BOOK_COPY bc
    GROUP BY bc.resource_id
) bc_agg
    ON r.resource_id = bc_agg.resource_id;

-- =============================================================================
-- 2b. VIEW_BOOKS_DETAILED
-- Grain: 1 row per BOOK_COPY (physical book copy level)
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_BOOKS_DETAILED AS
SELECT
    bc.copy_id,
    bc.barcode,
    bc.condition_status,
    bc.availability_status,
    r.resource_id,
    r.title,
    r.publish_date,
    r.topic,
    r.is_public,
    pb.isbn,
    pb.shelf_location,
    COALESCE(rl_agg.required_license_count, 0) AS required_license_count
FROM BOOK_COPY bc
INNER JOIN PHYSICAL_BOOK pb
    ON bc.resource_id = pb.resource_id
INNER JOIN RESOURCE r
    ON pb.resource_id = r.resource_id
LEFT JOIN (
    SELECT
        rl.resource_id,
        COUNT(*) AS required_license_count
    FROM RESOURCE_LICENSE rl
    GROUP BY rl.resource_id
) rl_agg
    ON r.resource_id = rl_agg.resource_id;

-- =============================================================================
-- 2c. VIEW_JOURNALS_DETAILED
-- Grain: 1 row per DIGITAL_JOURNAL
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_JOURNALS_DETAILED AS
SELECT
    r.resource_id,
    r.title,
    r.publish_date,
    r.topic,
    r.is_public,
    dj.doi,
    dj.url,
    dj.file_size,
    dj.format,
    COALESCE(rl_agg.required_license_count, 0) AS required_license_count
FROM DIGITAL_JOURNAL dj
INNER JOIN RESOURCE r
    ON dj.resource_id = r.resource_id
LEFT JOIN (
    SELECT
        rl.resource_id,
        COUNT(*) AS required_license_count
    FROM RESOURCE_LICENSE rl
    GROUP BY rl.resource_id
) rl_agg
    ON r.resource_id = rl_agg.resource_id;

-- =============================================================================
-- 3. VIEW_BORROWS_DETAILED
-- Grain: 1 row per BORROW
-- Picks at most the latest FINE per borrow_id to avoid row duplication.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_BORROWS_DETAILED AS
SELECT
    b.borrow_id,
    b.member_person_id,
    b.member_person_id AS person_id,
    p.name AS member_name,
    p.person_type,
    b.copy_id,
    bc.barcode,
    r.resource_id,
    r.title AS book_title,
    r.title AS resource_title,
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
INNER JOIN PERSON p
    ON b.member_person_id = p.person_id
INNER JOIN BOOK_COPY bc
    ON b.copy_id = bc.copy_id
INNER JOIN RESOURCE r
    ON bc.resource_id = r.resource_id
LEFT JOIN (
    SELECT
        fine_id,
        borrow_id,
        total_fine_amount,
        payment_status
    FROM (
        SELECT
            f.fine_id,
            f.borrow_id,
            f.total_fine_amount,
            f.payment_status,
            ROW_NUMBER() OVER (PARTITION BY f.borrow_id ORDER BY f.fine_id DESC) AS rn
        FROM FINE f
    ) ranked_fines
    WHERE rn = 1
) f_latest
    ON b.borrow_id = f_latest.borrow_id;

-- =============================================================================
-- 4. VIEW_FINES_DETAILED
-- Grain: 1 row per FINE
-- A fine may have multiple PAYMENT_TRANSACTION rows; picks the most recent
-- transaction (by payment_date DESC, transaction_id DESC) to avoid fan-out.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_FINES_DETAILED AS
SELECT
    f.fine_id,
    f.borrow_id,
    b.member_person_id,
    b.member_person_id AS person_id,
    p.name AS member_name,
    b.copy_id,
    r.title AS book_title,
    r.title AS resource_title,
    b.borrow_date,
    b.due_date,
    b.return_date,
    f.total_fine_amount,
    f.payment_status,
    pt_latest.transaction_id,
    pt_latest.payment_date
FROM FINE f
INNER JOIN BORROW b
    ON f.borrow_id = b.borrow_id
INNER JOIN PERSON p
    ON b.member_person_id = p.person_id
INNER JOIN BOOK_COPY bc
    ON b.copy_id = bc.copy_id
INNER JOIN RESOURCE r
    ON bc.resource_id = r.resource_id
LEFT JOIN (
    SELECT
        transaction_id,
        fine_id,
        payment_date
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
    ) ranked_tx
    WHERE rn = 1
) pt_latest
    ON f.fine_id = pt_latest.fine_id;

-- =============================================================================
-- 5. VIEW_ACCESS_LOGS_DETAILED
-- Grain: 1 row per ACCESS_LOG
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_ACCESS_LOGS_DETAILED AS
SELECT
    l.log_id,
    l.person_id,
    p.name AS person_name,
    p.person_type,
    l.journal_resource_id,
    l.journal_resource_id AS resource_id,
    r.title AS journal_title,
    l.access_timestamp,
    l.access_timestamp AS access_date,
    l.ip_address,
    l.download_success,
    CASE
        WHEN l.access_result IS NOT NULL THEN l.access_result
        WHEN l.download_success = 1 THEN 'ALLOWED'
        ELSE 'DENIED'
    END AS access_result
FROM ACCESS_LOG l
LEFT JOIN PERSON p
    ON l.person_id = p.person_id
LEFT JOIN RESOURCE r
    ON l.journal_resource_id = r.resource_id;

-- =============================================================================
-- 6. VIEW_GRANTED_LICENSES_DETAILED
-- Grain: 1 row per GRANTED_LICENSE
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_GRANTED_LICENSES_DETAILED AS
SELECT
    gl.grant_id,
    gl.person_id,
    p.name AS person_name,
    p.person_type,
    gl.license_id,
    lp.license_name,
    lp.provider,
    lp.description
FROM GRANTED_LICENSE gl
INNER JOIN PERSON p
    ON gl.person_id = p.person_id
INNER JOIN LICENSE_PACKAGE lp
    ON gl.license_id = lp.license_id;
