-- ============================================================
-- V13: Drop the AIRLINE_STAFF role
-- ------------------------------------------------------------
-- The role existed only in @PreAuthorize rules and this CHECK
-- constraint. Nothing ever assigned it, no user held it, and
-- there was no way to create one — so every endpoint it appeared
-- on was effectively ADMIN-only already, just less obviously.
--
-- Leaving the constraint permitting a value the application can
-- no longer produce would invite exactly that confusion back, so
-- it is narrowed to match the enum.
-- ============================================================

-- Refuse to run rather than silently strand rows the new constraint
-- would reject. There are none today, but a environment that has been
-- around longer than this one deserves the check.
DO $$
    DECLARE staff_count INT;
    BEGIN
        SELECT count(*) INTO staff_count FROM users WHERE role = 'AIRLINE_STAFF';
        IF staff_count > 0 THEN
            RAISE EXCEPTION
                'Cannot drop AIRLINE_STAFF: % user(s) still hold it. Reassign them first.',
                staff_count;
        END IF;
    END
$$;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_role;

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'));
