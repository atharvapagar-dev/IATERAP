-- V13: Enforce cross-LRU FK integrity on Test Step references.
-- A TestStep's Interface/Connector/SignalLibrary/ProtocolTag/FaultTemplate
-- must belong to the same LRU as its TestProcedure. Enforced via Postgres
-- composite foreign keys rather than application code: test_steps.lru_id
-- is a denormalized column (kept in sync from test_procedures.lru_id at
-- the application layer), and composite FKs of the form
-- (lru_id, x_id) REFERENCES x(lru_id, id) reject any cross-LRU reference
-- at the database level. Postgres MATCH SIMPLE (default) skips the check
-- when either column is NULL, so optional FK fields remain optional.

ALTER TABLE test_steps ADD COLUMN IF NOT EXISTS lru_id BIGINT;

UPDATE test_steps ts
SET lru_id = tp.lru_id
FROM test_procedures tp
WHERE ts.test_procedure_id = tp.id
  AND ts.lru_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_test_step_lru ON test_steps (lru_id);

-- Composite unique constraints required as FK targets
ALTER TABLE "interface" ADD CONSTRAINT uq_interface_lru_id UNIQUE (lru_id, id);
ALTER TABLE connector ADD CONSTRAINT uq_connector_lru_id UNIQUE (lru_id, id);
ALTER TABLE signal_libraries ADD CONSTRAINT uq_signal_library_lru_id UNIQUE (lru_id, id);
ALTER TABLE protocol_tags ADD CONSTRAINT uq_protocol_tag_lru_id UNIQUE (lru_id, id);
ALTER TABLE fault_templates ADD CONSTRAINT uq_fault_template_lru_id UNIQUE (lru_id, id);
ALTER TABLE test_procedures ADD CONSTRAINT uq_test_procedure_lru_id UNIQUE (lru_id, id);

-- Composite FKs on test_steps: cross-LRU references now rejected by Postgres
ALTER TABLE test_steps ADD CONSTRAINT fk_test_step_lru_interface
    FOREIGN KEY (lru_id, interface_id) REFERENCES "interface" (lru_id, id);
ALTER TABLE test_steps ADD CONSTRAINT fk_test_step_lru_connector
    FOREIGN KEY (lru_id, connector_id) REFERENCES connector (lru_id, id);
ALTER TABLE test_steps ADD CONSTRAINT fk_test_step_lru_signal_library
    FOREIGN KEY (lru_id, signal_library_id) REFERENCES signal_libraries (lru_id, id);
ALTER TABLE test_steps ADD CONSTRAINT fk_test_step_lru_protocol_tag
    FOREIGN KEY (lru_id, protocol_tag_id) REFERENCES protocol_tags (lru_id, id);
ALTER TABLE test_steps ADD CONSTRAINT fk_test_step_lru_fault_template
    FOREIGN KEY (lru_id, fault_template_id) REFERENCES fault_templates (lru_id, id);
ALTER TABLE test_steps ADD CONSTRAINT fk_test_step_lru_test_procedure
    FOREIGN KEY (lru_id, test_procedure_id) REFERENCES test_procedures (lru_id, id);
