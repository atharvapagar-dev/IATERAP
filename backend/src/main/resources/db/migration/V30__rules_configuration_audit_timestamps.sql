-- V21 is immutable. Add the audit columns required by the current entity
-- mapping in a new migration so existing databases do not fail checksum
-- validation after upgrading the application.
ALTER TABLE rules_configuration
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE rules_configuration
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
