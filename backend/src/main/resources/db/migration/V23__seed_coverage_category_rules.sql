INSERT INTO rules_configuration (rule_id, category, priority, description, enabled)
VALUES
    ('COVERAGE-INTERFACE', 'COVERAGE', 100, 'Interface elements referenced by test steps', TRUE),
    ('COVERAGE-SIGNAL', 'COVERAGE', 110, 'Signal elements referenced by test steps', TRUE),
    ('COVERAGE-REQUIREMENT', 'COVERAGE', 120, 'Requirement elements represented by test steps', TRUE),
    ('COVERAGE-FUNCTIONAL', 'COVERAGE', 130, 'Functional elements represented by test steps', TRUE),
    ('COVERAGE-FAULT', 'COVERAGE', 140, 'Fault elements referenced by test steps', TRUE),
    ('COVERAGE-FMEA', 'COVERAGE', 150, 'FMEA evidence represented by test steps', TRUE)
ON CONFLICT (rule_id) DO NOTHING;
