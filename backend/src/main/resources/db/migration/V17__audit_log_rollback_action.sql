-- V17: Rollback audit action (AI-187 / T7-B3).
--
-- Rollback (TestProcedureService#rollbackToRevision) records its own
-- audit action, ROLLBACK, distinct from the generic UPDATE that the
-- existing AuditAspect (V6__create_audit_log.sql) writes for every other
-- content-changing service call -- so a rollback is unambiguously
-- identifiable in the audit trail rather than looking like an ordinary
-- edit. This is the only schema change T7-B3 requires: no new table, no
-- new column, just widening the existing action vocabulary.
--
-- Existing rows and the existing CREATE/UPDATE/DELETE actions already
-- written by AuditAspect are unaffected; this only adds ROLLBACK as a
-- fourth permitted value.

ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS chk_audit_log_action;

ALTER TABLE audit_log
    ADD CONSTRAINT chk_audit_log_action
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'ROLLBACK'));
