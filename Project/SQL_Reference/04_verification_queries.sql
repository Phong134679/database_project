-- =============================================================================
-- SQL REFERENCE: VERIFICATION QUERIES (READ-ONLY)
-- Purpose: Verify View consistency, JOIN cardinality, NULL semantics, and
--          edge cases without modifying any persisted data.
-- =============================================================================

-- 1. Check row counts across all 8 Reference Views vs base tables
SELECT 'VIEW_MEMBERS_DETAILED' AS view_name, COUNT(*) AS view_rows,
       (SELECT COUNT(*) FROM PERSON) AS expected_base_rows
FROM VIEW_MEMBERS_DETAILED
UNION ALL
SELECT 'VIEW_RESOURCES_DETAILED', COUNT(*),
       (SELECT COUNT(*) FROM RESOURCE)
FROM VIEW_RESOURCES_DETAILED
UNION ALL
SELECT 'VIEW_BOOKS_DETAILED', COUNT(*),
       (SELECT COUNT(*) FROM BOOK_COPY)
FROM VIEW_BOOKS_DETAILED
UNION ALL
SELECT 'VIEW_JOURNALS_DETAILED', COUNT(*),
       (SELECT COUNT(*) FROM DIGITAL_JOURNAL)
FROM VIEW_JOURNALS_DETAILED
UNION ALL
SELECT 'VIEW_BORROWS_DETAILED', COUNT(*),
       (SELECT COUNT(*) FROM BORROW)
FROM VIEW_BORROWS_DETAILED
UNION ALL
SELECT 'VIEW_FINES_DETAILED', COUNT(*),
       (SELECT COUNT(*) FROM FINE)
FROM VIEW_FINES_DETAILED
UNION ALL
SELECT 'VIEW_ACCESS_LOGS_DETAILED', COUNT(*),
       (SELECT COUNT(*) FROM ACCESS_LOG)
FROM VIEW_ACCESS_LOGS_DETAILED
UNION ALL
SELECT 'VIEW_GRANTED_LICENSES_DETAILED', COUNT(*),
       (SELECT COUNT(*) FROM GRANTED_LICENSE)
FROM VIEW_GRANTED_LICENSES_DETAILED;

-- 2. Verify RESOURCE subtypes (PHYSICAL_BOOK only, DIGITAL_JOURNAL only, BOTH, UNKNOWN)
-- Must have zero duplicate resource_id rows.
SELECT resource_id, title, is_physical_book, is_digital_journal, resource_type,
       total_copies, available_copies, journal_format, required_license_count
FROM VIEW_RESOURCES_DETAILED
ORDER BY resource_id ASC;

SELECT resource_id, COUNT(*) AS row_count
FROM VIEW_RESOURCES_DETAILED
GROUP BY resource_id
HAVING COUNT(*) > 1;

-- 3. Verify Members vs Non-Members & NULL profile fields
SELECT person_id, name, member_id, is_member, is_author, member_type,
       loan_duration_days, borrow_quota, active_borrows_count,
       overdue_borrows_count, unpaid_fines_count, is_eligible_to_borrow
FROM VIEW_MEMBERS_DETAILED
ORDER BY person_id ASC;

-- Non-members must have is_member = 0, loan_duration_days = 0, borrow_quota = 0, is_eligible_to_borrow = 0
SELECT person_id, name, member_id, is_member, is_eligible_to_borrow
FROM VIEW_MEMBERS_DETAILED
WHERE member_id IS NULL;

-- 4. Verify Book Copies and shelf/license metadata
SELECT copy_id, resource_id, book_title, availability_status, shelf_location,
       is_public, required_license_count
FROM VIEW_BOOKS_DETAILED
ORDER BY copy_id ASC;

-- 5. Verify Borrows, status derivation ('Returned'/'Overdue'/'Active'), and latest Fine (NULL when none)
SELECT borrow_id, member_id, copy_id, book_title, borrow_date, due_date,
       return_date, is_overdue, status, fine_id, fine_amount, fine_status
FROM VIEW_BORROWS_DETAILED
ORDER BY borrow_id DESC;

-- 6. Verify Fines and latest Payment Transaction (NULL transaction_id/payment_date when Unpaid)
SELECT fine_id, borrow_id, member_id, member_name, total_fine_amount,
       payment_status, transaction_id, payment_date
FROM VIEW_FINES_DETAILED
ORDER BY fine_id DESC;

-- 7. Verify Granted Licenses (including Non-members with NULL member_id/member_type)
SELECT grant_id, person_id, person_name, member_id, member_type,
       license_id, license_name
FROM VIEW_GRANTED_LICENSES_DETAILED
ORDER BY grant_id DESC;

-- 8. Verify Access Logs (including Guest with NULL person_id -> accessed_as = 'Guest')
SELECT log_id, person_id, person_name, accessed_as,
       journal_resource_id, journal_title, ip_address
FROM VIEW_ACCESS_LOGS_DETAILED
ORDER BY log_id DESC;

-- 9. Verify fn_has_all_required_licenses and quota/duration helper functions
SELECT fn_get_borrow_quota('FACULTY') AS faculty_quota,
       fn_get_borrow_quota('STUDENT') AS student_quota,
       fn_get_loan_duration_days('FACULTY') AS faculty_days,
       fn_get_loan_duration_days('STUDENT') AS student_days,
       fn_has_all_required_licenses(NULL, 3) AS null_person_check,
       fn_has_all_required_licenses(1, 4) AS public_no_req_license_check,
       fn_has_all_required_licenses(6, 5) AS person6_journal5_all_licenses,
       fn_has_all_required_licenses(1, 5) AS person1_journal5_missing_license;
