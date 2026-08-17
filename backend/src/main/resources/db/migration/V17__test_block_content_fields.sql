-- V17: AI-179 [T7-A1] Test Block library reusable content fields.
-- Extends the AI-153 shell (name/description only) with the TestStep
-- content fields a Test Block can hold, plus a usage_count for reuse
-- tracking. Columns are nullable: legacy seeded shell rows keep no
-- content and are rejected by TestBlockService.insertIntoProcedure()
-- rather than by a NOT NULL constraint, so the seeder does not need to
-- change in this migration.
ALTER TABLE test_blocks
    ADD COLUMN expected_behaviour VARCHAR(1000),
    ADD COLUMN expected_range VARCHAR(255),
    ADD COLUMN calibration_requirement VARCHAR(500),
    ADD COLUMN safety_notes VARCHAR(1000),
    ADD COLUMN acceptance_criteria VARCHAR(1000),
    ADD COLUMN preconditions VARCHAR(1000),
    ADD COLUMN dependencies VARCHAR(1000),
    ADD COLUMN post_conditions VARCHAR(1000),
    ADD COLUMN pass_fail_criteria VARCHAR(1000),
    ADD COLUMN expected_observation VARCHAR(1000),
    ADD COLUMN engineer_remarks VARCHAR(1000),
    ADD COLUMN usage_count INTEGER NOT NULL DEFAULT 0;