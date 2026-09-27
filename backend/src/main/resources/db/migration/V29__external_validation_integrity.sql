ALTER TABLE validation_readings ADD COLUMN IF NOT EXISTS component_type VARCHAR(32);
ALTER TABLE validation_readings ADD COLUMN IF NOT EXISTS component_id BIGINT;
ALTER TABLE validation_readings ADD COLUMN IF NOT EXISTS remediation_status VARCHAR(32) NOT NULL DEFAULT 'OPEN';
ALTER TABLE validation_readings ADD COLUMN IF NOT EXISTS remediation_note VARCHAR(2000);
ALTER TABLE validation_readings ADD COLUMN IF NOT EXISTS resolved_by VARCHAR(255);
ALTER TABLE validation_readings ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP;

ALTER TABLE validation_runs DROP CONSTRAINT IF EXISTS chk_validation_run_status;
ALTER TABLE validation_runs ADD CONSTRAINT chk_validation_run_status
    CHECK (status IN ('RECEIVED', 'ANALYZED', 'REVIEW_REQUIRED', 'ACCEPTED', 'REJECTED'));
ALTER TABLE validation_readings DROP CONSTRAINT IF EXISTS chk_validation_reading_remediation;
ALTER TABLE validation_readings ADD CONSTRAINT chk_validation_reading_remediation
    CHECK (remediation_status IN ('OPEN', 'IN_REMEDIATION', 'RESOLVED', 'WAIVED'));
CREATE INDEX IF NOT EXISTS idx_validation_reading_component
    ON validation_readings (component_type, component_id);
