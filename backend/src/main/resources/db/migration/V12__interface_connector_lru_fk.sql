-- V12: Add LRU ownership FK to Interface and Connector.
-- These were previously global libraries with no LRU scope, which made
-- cross-LRU FK validation (per Coverage Engine spec, Section 16.1 / 21.7)
-- structurally impossible. Existing synthetic rows are cleared since they
-- carry no LRU association to backfill from; reseed via synthetic-data profile.

TRUNCATE TABLE signal_ranges, signal_libraries, test_steps, test_procedures,
    interface, connector RESTART IDENTITY CASCADE;

ALTER TABLE interface ADD COLUMN lru_id BIGINT;
ALTER TABLE connector ADD COLUMN lru_id BIGINT;

ALTER TABLE interface ALTER COLUMN lru_id SET NOT NULL;
ALTER TABLE connector ALTER COLUMN lru_id SET NOT NULL;

ALTER TABLE interface ADD CONSTRAINT fk_interface_lru FOREIGN KEY (lru_id) REFERENCES lru(id);
ALTER TABLE connector ADD CONSTRAINT fk_connector_lru FOREIGN KEY (lru_id) REFERENCES lru(id);
