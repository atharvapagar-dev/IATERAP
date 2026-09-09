-- Week 10: existing rule configuration is also the live composite-scoring source.
-- Non-coverage rules retain NULL values and continue to use only priority/enabled metadata.
ALTER TABLE rules_configuration
    ADD COLUMN weight NUMERIC(5, 2),
    ADD COLUMN threshold NUMERIC(5, 2);

ALTER TABLE rules_configuration
    ADD CONSTRAINT chk_rules_configuration_weight
        CHECK (weight IS NULL OR (weight >= 0 AND weight <= 100)),
    ADD CONSTRAINT chk_rules_configuration_threshold
        CHECK (threshold IS NULL OR (threshold >= 0 AND threshold <= 100));

UPDATE rules_configuration
SET weight = CASE rule_id
        WHEN 'COVERAGE-FAULT' THEN 25.00
        WHEN 'COVERAGE-FMEA' THEN 20.00
        WHEN 'COVERAGE-REQUIREMENT' THEN 20.00
        WHEN 'COVERAGE-INTERFACE' THEN 15.00
        WHEN 'COVERAGE-SIGNAL' THEN 10.00
        WHEN 'COVERAGE-FUNCTIONAL' THEN 10.00
    END,
    threshold = 0.00
WHERE rule_id IN (
    'COVERAGE-FAULT', 'COVERAGE-FMEA', 'COVERAGE-REQUIREMENT',
    'COVERAGE-INTERFACE', 'COVERAGE-SIGNAL', 'COVERAGE-FUNCTIONAL'
);

INSERT INTO rules_configuration (rule_id, category, priority, description, enabled)
VALUES ('ADVISORY-UNUSED-TEST-BLOCK', 'ADVISORY', 300,
        'Shared Test Blocks not yet reused should be reviewed for promotion', TRUE)
ON CONFLICT (rule_id) DO NOTHING;
