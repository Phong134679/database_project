-- =============================================================================
-- SQL REFERENCE: DATABASE VIEWS
-- Source of Truth: Current Java + JDBC Implementation
-- Note: Reference-only script. Do NOT load into application runtime.
-- Target Schema: Project/01_create_database_final_v2.sql
-- =============================================================================

-- =============================================================================
-- 1. VIEW_MEMBERS_DETAILED
-- Java Source: PersonRepository.findAllDetailedRaw() + MemberService.getAllPersonsDetailed()
-- Preserves all PERSON rows (members and non-members), author flag, and borrow/fine counts.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_MEMBERS_DETAILED AS
SELECT
    p.person_id,
    p.name,
    p.email,
    p.phone,
    um.member_id,
    (um.member_id IS NOT NULL) AS is_member,
    CASE
        WHEN EXISTS (SELECT 1 FROM AUTHOR_OF ao WHERE ao.person_id = p.person_id)
        THEN TRUE ELSE FALSE
    END AS is_author,
    um.member_type,
    um.major,
    um.academic_rank,
    CASE
        WHEN um.member_id IS NULL THEN 0
        WHEN UPPER(um.member_type) = 'FACULTY' THEN 30
        ELSE 14
    END AS loan_duration_days,
    CASE
        WHEN um.member_id IS NULL THEN 0
        WHEN UPPER(um.member_type) = 'FACULTY' THEN 10
        ELSE 3
    END AS borrow_quota,
    COALESCE(bs.active_borrow_count, 0) AS active_borrows_count,
    COALESCE(bs.overdue_borrow_count, 0) AS overdue_borrows_count,
    COALESCE(fs.unpaid_fine_count, 0) AS unpaid_fines_count,
    (COALESCE(bs.overdue_borrow_count, 0) > 0) AS has_overdue,
    (COALESCE(fs.unpaid_fine_count, 0) > 0) AS has_unpaid_fines,
    CASE
        WHEN um.member_id IS NOT NULL
         AND COALESCE(bs.overdue_borrow_count, 0) = 0
         AND COALESCE(fs.unpaid_fine_count, 0) = 0
         AND COALESCE(bs.active_borrow_count, 0) <
             CASE WHEN UPPER(um.member_type) = 'FACULTY' THEN 10 ELSE 3 END
        THEN TRUE ELSE FALSE
    END AS is_eligible_to_borrow
FROM PERSON p
LEFT JOIN UNIVERSITY_MEMBER um
    ON um.person_id = p.person_id
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
-- Java Source: ResourceRepository.findAllResourcesDetailed()
-- Handles RESOURCE with PHYSICAL_BOOK only, DIGITAL_JOURNAL only, BOTH, or UNKNOWN
-- without duplicating rows via pre-aggregated subqueries on RESOURCE_LICENSE & BOOK_COPY.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_RESOURCES_DETAILED AS
SELECT
    r.resource_id,
    r.title,
    r.topic,
    (pb.resource_id IS NOT NULL) AS is_physical_book,
    (dj.resource_id IS NOT NULL) AS is_digital_journal,
    CASE
        WHEN pb.resource_id IS NOT NULL AND dj.resource_id IS NOT NULL THEN 'BOTH'
        WHEN pb.resource_id IS NOT NULL THEN 'PHYSICAL_BOOK'
        WHEN dj.resource_id IS NOT NULL THEN 'DIGITAL_JOURNAL'
        ELSE 'UNKNOWN'
    END AS resource_type,
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
-- Java Source: BorrowRepository.findAllCopiesDetailed()
-- One row per physical BOOK_COPY with parent RESOURCE & license requirement count.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_BOOKS_DETAILED AS
SELECT
    bc.copy_id,
    r.resource_id,
    r.title AS book_title,
    bc.availability_status,
    pb.shelf_location,
    r.is_public,
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
-- Java Source: DigitalJournalRepository.findById() / findAll()
-- One row per DIGITAL_JOURNAL joined with RESOURCE.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_JOURNALS_DETAILED AS
SELECT
    r.resource_id,
    r.title,
    r.topic,
    r.is_public,
    dj.url,
    dj.file_size,
    dj.format
FROM DIGITAL_JOURNAL dj
INNER JOIN RESOURCE r
    ON r.resource_id = dj.resource_id;

-- =============================================================================
-- 5. VIEW_BORROWS_DETAILED
-- Java Source: BorrowRepository.findAllBorrowsDetailedRaw() + BorrowService.getAllBorrowsDetailed()
-- One row per BORROW with member, physical book title, status, and latest FINE.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_BORROWS_DETAILED AS
SELECT
    b.borrow_id,
    b.member_id,
    um.person_id,
    p.name AS member_name,
    um.member_type,
    b.copy_id,
    r.title AS book_title,
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
            ROW_NUMBER() OVER (PARTITION BY f.borrow_id ORDER BY f.fine_id DESC) AS rn
        FROM FINE f
    ) ranked
    WHERE rn = 1
) f_latest
    ON f_latest.borrow_id = b.borrow_id;

-- =============================================================================
-- 6. VIEW_FINES_DETAILED
-- Java Source: FineRepository.findAllFinesDetailed()
-- One row per FINE with member, borrow, book title, and latest PAYMENT_TRANSACTION.
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
-- Java Source: AccessLogRepository.findAllDetailedRaw() + DigitalJournalService.getAllAccessLogsDetailed()
-- One row per ACCESS_LOG; NULL person_id maps to 'Guest' in accessed_as.
-- =============================================================================
CREATE OR REPLACE VIEW VIEW_ACCESS_LOGS_DETAILED AS
SELECT
    l.log_id,
    l.person_id,
    p.name AS person_name,
    CASE WHEN l.person_id IS NULL THEN 'Guest' ELSE p.name END AS accessed_as,
    l.journal_resource_id,
    r.title AS journal_title,
    r.is_public,
    dj.url AS journal_url,
    dj.format AS journal_format,
    l.ip_address
FROM ACCESS_LOG l
LEFT JOIN PERSON p
    ON p.person_id = l.person_id
LEFT JOIN RESOURCE r
    ON r.resource_id = l.journal_resource_id
LEFT JOIN DIGITAL_JOURNAL dj
    ON dj.resource_id = l.journal_resource_id;

-- =============================================================================
-- 8. VIEW_GRANTED_LICENSES_DETAILED
-- Java Source: GrantedLicenseRepository.findAllDetailed()
-- One row per GRANTED_LICENSE with PERSON, LICENSE_PACKAGE, and optional UNIVERSITY_MEMBER.
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
