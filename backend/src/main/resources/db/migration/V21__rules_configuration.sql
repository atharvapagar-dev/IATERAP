CREATE TABLE rules_configuration (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(120) NOT NULL,
    category VARCHAR(30) NOT NULL,
    priority INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_rules_configuration_rule_id UNIQUE (rule_id),
    CONSTRAINT chk_rules_configuration_category
        CHECK (category IN ('STRUCTURAL', 'COVERAGE', 'SAFETY', 'ADVISORY'))
);

CREATE INDEX idx_rules_configuration_execution
    ON rules_configuration (enabled, category, priority);
