CREATE TABLE rules_configuration (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(30) NOT NULL CHECK (category IN ('STRUCTURAL', 'COVERAGE', 'SAFETY', 'ADVISORY')),
    priority INTEGER NOT NULL CHECK (priority >= 0),
    description VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rules_configuration_priority ON rules_configuration (priority, rule_id);

INSERT INTO rules_configuration (rule_id, category, priority, description, enabled)
VALUES ('STRUCT-TEST-STEP-SIGNAL', 'STRUCTURAL', 10,
        'Every Test Step must reference a valid Signal for the procedure LRU', TRUE);