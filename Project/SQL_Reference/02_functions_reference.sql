-- =============================================================================
-- SQL REFERENCE: STORED FUNCTIONS
-- Source of Truth: Current Java + JDBC Implementation
-- Note: Reference-only script. Do NOT load into application runtime.
-- Target Schema: Project/01_create_database_final_v2.sql
-- =============================================================================

DELIMITER //

-- =============================================================================
-- 1. fn_has_all_required_licenses
-- Java Source: ResourceService.hasAllRequiredLicenses(Connection, int, int)
-- Tables Read: RESOURCE_LICENSE, GRANTED_LICENSE
-- Returns: TRUE iff resource has >= 1 required license AND person holds all of them.
--          Returns FALSE when required_count = 0 or any input is NULL.
-- =============================================================================
DROP FUNCTION IF EXISTS fn_has_all_required_licenses//
CREATE FUNCTION fn_has_all_required_licenses(
    p_person_id   INT,
    p_resource_id INT
)
RETURNS BOOLEAN
READS SQL DATA
BEGIN
    DECLARE v_required_count INT DEFAULT 0;
    DECLARE v_granted_count  INT DEFAULT 0;

    IF p_person_id IS NULL OR p_resource_id IS NULL THEN
        RETURN FALSE;
    END IF;

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

-- =============================================================================
-- 2. fn_get_borrow_quota
-- Java Source: MemberService.getBorrowQuota(String memberType)
-- Tables Read: None (pure business rule calculation)
-- Returns: 10 for FACULTY, 3 for STUDENT (or any non-FACULTY member_type).
-- =============================================================================
DROP FUNCTION IF EXISTS fn_get_borrow_quota//
CREATE FUNCTION fn_get_borrow_quota(
    p_member_type VARCHAR(20)
)
RETURNS INT
DETERMINISTIC
BEGIN
    IF UPPER(COALESCE(p_member_type, '')) = 'FACULTY' THEN
        RETURN 10;
    END IF;
    RETURN 3;
END//

-- =============================================================================
-- 3. fn_get_loan_duration_days
-- Java Source: MemberService.getLoanDurationDays(String memberType)
-- Tables Read: None (pure business rule calculation)
-- Returns: 30 days for FACULTY, 14 days for STUDENT.
-- =============================================================================
DROP FUNCTION IF EXISTS fn_get_loan_duration_days//
CREATE FUNCTION fn_get_loan_duration_days(
    p_member_type VARCHAR(20)
)
RETURNS INT
DETERMINISTIC
BEGIN
    IF UPPER(COALESCE(p_member_type, '')) = 'FACULTY' THEN
        RETURN 30;
    END IF;
    RETURN 14;
END//

DELIMITER ;
