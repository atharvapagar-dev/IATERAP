-- V18: Add lru_name to procedure_history (AI-185)
--
-- Every reference field already captured in a procedure_history snapshot
-- follows the same pattern: store the id AND the resolved display name at
-- snapshot time, so a historical revision remains self-describing even if
-- the live entity is later renamed (see TestStepResponse: interfaceName,
-- connectorName, signalLibraryName, protocolTagName, faultCategoryName).
-- lru_id on procedure_history was the one field that broke this pattern --
-- a version-comparison diff engine reading procedure_history alone would
-- have had to re-query the live, possibly-since-renamed lru table just to
-- show which LRU a historical revision belonged to. This closes that gap.
ALTER TABLE procedure_history ADD COLUMN lru_name VARCHAR(255);

-- Best-effort backfill for rows written before this migration: uses the
-- LRU's CURRENT name, since the name at original snapshot time was never
-- captured for these older rows. Acknowledged limitation for pre-V18 rows
-- only -- every row written from this migration onward captures the true
-- point-in-time name via TestProcedureService.writeRevision().
UPDATE procedure_history ph
SET lru_name = l.name
FROM lru l
WHERE ph.lru_id = l.id
  AND ph.lru_name IS NULL;