-- Supports chronological, evidence-only reliability aggregation without
-- altering or duplicating the authoritative validation evidence tables.
CREATE INDEX IF NOT EXISTS idx_validation_run_uploaded_at
    ON validation_runs (uploaded_at);

CREATE INDEX IF NOT EXISTS idx_validation_reading_result
    ON validation_readings (result);
