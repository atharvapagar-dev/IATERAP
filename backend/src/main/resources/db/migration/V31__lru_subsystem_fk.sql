-- AI-216: restore the proposal hierarchy needed for subsystem-level heatmaps.
ALTER TABLE lru ADD COLUMN IF NOT EXISTS subsystem_id BIGINT;
ALTER TABLE lru DROP CONSTRAINT IF EXISTS fk_lru_subsystem;
ALTER TABLE lru ADD CONSTRAINT fk_lru_subsystem
    FOREIGN KEY (subsystem_id) REFERENCES subsystem(id);
CREATE INDEX IF NOT EXISTS idx_lru_subsystem ON lru(subsystem_id);
