-- =============================================================================
-- SQL REFERENCE: FUNCTIONAL & INTEGRITY TEST SUITE
-- Target: Run ONLY on an isolated test database (loaded with schema + sample data
--         + 01_views_reference.sql + 02_functions_reference.sql + 03_procedures_reference.sql).
-- =============================================================================

-- 1. View & Resource BOTH subtype check
SELECT IF(
    (SELECT resource_type FROM VIEW_RESOURCES_DETAILED WHERE resource_id = 7) = 'BOTH',
    'PASS: Resource #7 is BOTH',
    'FAIL: Resource #7 is not BOTH'
) AS test_resource_both;

-- 2. Borrow: Author bypass on private physical book (SV2026001 borrows copy '2_1' of resource 2 without license 2)
CALL sp_borrow_book('SV2026001', '2_1', @b_id, @b_due);
SELECT @b_id AS author_borrow_id, @b_due AS author_due_date;

-- 3. Return on time (no fine)
SET @today = CURDATE();
CALL sp_return_book(@b_id, @today, @f_id_ontime, @f_amt_ontime);
SELECT @f_id_ontime AS ontime_fine_id, @f_amt_ontime AS ontime_fine_amount;

-- 4. Borrow + Return late (generates 50,000.00 Unpaid fine) + Pay fine
CALL sp_borrow_book('GV2026002', '1_2', @b_late_id, @b_late_due);
CALL sp_return_book(@b_late_id, DATE_ADD(@b_late_due, INTERVAL 5 DAY), @f_id_late, @f_amt_late);
SELECT @f_id_late AS late_fine_id, @f_amt_late AS late_fine_amount;

CALL sp_confirm_fine_payment(@f_id_late, @tx_id);
SELECT @tx_id AS payment_transaction_id,
       (SELECT payment_status FROM FINE WHERE fine_id = @f_id_late) AS fine_status_after_pay;

-- 5. Digital Journal Access: Guest (public=allowed, private=denied), Author (allowed), Licensed (allowed), Missing license (denied)
CALL sp_access_digital_journal(NULL, 4, @g_pub_ok, @g_pub_msg, @g_pub_log);
SELECT @g_pub_ok AS guest_public_allowed, @g_pub_log AS guest_public_log_id, @g_pub_msg AS msg;

CALL sp_access_digital_journal(NULL, 3, @g_priv_ok, @g_priv_msg, @g_priv_log);
SELECT @g_priv_ok AS guest_private_allowed, @g_priv_log AS guest_private_log_id, @g_priv_msg AS msg;

CALL sp_access_digital_journal(3, 3, @auth_ok, @auth_msg, @auth_log);
SELECT @auth_ok AS author_allowed, @auth_log AS author_log_id, @auth_msg AS msg;

CALL sp_access_digital_journal(6, 5, @lic_ok, @lic_msg, @lic_log);
SELECT @lic_ok AS licensed_allowed, @lic_log AS licensed_log_id, @lic_msg AS msg;

CALL sp_access_digital_journal(4, 5, @nolic_ok, @nolic_msg, @nolic_log);
SELECT @nolic_ok AS missing_license_allowed, @nolic_log AS missing_license_log_id, @nolic_msg AS msg;

-- 6. Grant & Revoke License Manual
CALL sp_grant_license_manual(4, 3, @new_grant_id);
SELECT @new_grant_id AS granted_id;
CALL sp_revoke_license_by_grant_id(@new_grant_id, @revoked_rows);
SELECT @revoked_rows AS revoked_by_grant_id;
