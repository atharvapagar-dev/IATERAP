-- Ensure deleting a procedure removes its snapshot and coverage reports.
-- Keep V32 fault-injection and validation RESTRICT constraints unchanged.

ALTER TABLE procedure_snapshot
    DROP CONSTRAINT IF EXISTS procedure_snapshot_test_procedure_id_fkey;

ALTER TABLE procedure_snapshot
    ADD CONSTRAINT procedure_snapshot_test_procedure_id_fkey
    FOREIGN KEY (test_procedure_id)
    REFERENCES test_procedures(id)
    ON DELETE CASCADE;

ALTER TABLE coverage_report
    DROP CONSTRAINT IF EXISTS coverage_report_test_procedure_id_fkey;

ALTER TABLE coverage_report
    ADD CONSTRAINT coverage_report_test_procedure_id_fkey
    FOREIGN KEY (test_procedure_id)
    REFERENCES test_procedures(id)
    ON DELETE CASCADE;

ALTER TABLE coverage_report
    DROP CONSTRAINT IF EXISTS coverage_report_procedure_snapshot_id_fkey;

ALTER TABLE coverage_report
    ADD CONSTRAINT coverage_report_procedure_snapshot_id_fkey
    FOREIGN KEY (procedure_snapshot_id)
    REFERENCES procedure_snapshot(id)
    ON DELETE CASCADE;
