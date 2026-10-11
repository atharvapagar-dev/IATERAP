-- AI-246: a synthetic installed LRU belongs to one simulated aircraft. The
-- source run link makes generated fleet events traceable and idempotent.
ALTER TABLE synthetic_lru_instance
    ADD COLUMN aircraft_id BIGINT;

ALTER TABLE synthetic_lru_instance
    ADD CONSTRAINT fk_synthetic_lru_instance_aircraft
    FOREIGN KEY (aircraft_id) REFERENCES aircraft(id) ON DELETE RESTRICT;

CREATE INDEX idx_synthetic_lru_instance_aircraft_lru
    ON synthetic_lru_instance (aircraft_id, lru_id);

ALTER TABLE event_log
    ADD COLUMN source_validation_run_id BIGINT;

ALTER TABLE event_log
    ADD CONSTRAINT fk_event_log_source_validation_run
    FOREIGN KEY (source_validation_run_id) REFERENCES validation_runs(id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_event_log_generated_source_type
    ON event_log (synthetic_lru_instance_id, source_validation_run_id, event_type)
    WHERE source_validation_run_id IS NOT NULL;
