-- V19: Rollback audit action (AI-187 / T7-B3).
-- Rollback is recorded as a distinct audit action rather than UPDATE.
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS chk_audit_log_action;

ALTER TABLE audit_log
    ADD CONSTRAINT chk_audit_log_action
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'ROLLBACK'));
