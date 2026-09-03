INSERT INTO rules_configuration (rule_id, category, priority, description, enabled)
VALUES (
    'STRUCT-TEST-STEP-SIGNAL',
    'STRUCTURAL',
    10,
    'Every Test Step must reference a valid Signal for the procedure LRU',
    TRUE
)
ON CONFLICT (rule_id) DO NOTHING;
