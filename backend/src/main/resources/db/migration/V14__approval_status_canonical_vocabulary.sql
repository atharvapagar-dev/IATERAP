-- V14: Align approval_status vocabulary with the canonical Approval State
-- Machine (AI-164 / T5-B1).
--
-- V10 and V11 defined approval_status CHECK constraints allowing
-- ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'). The canonical workflow
-- implemented by ApprovalStateMachine uses
-- ('DRAFT', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'SUPERSEDED').
--
-- Investigation: no application code (entities, services, or
-- SyntheticDataSeeder) ever writes 'SUBMITTED' -- every TestStep/
-- TestProcedure row defaults to 'DRAFT' in Java
-- (TestStep.approvalStatus, TestProcedure.approvalStatus) and the seeder
-- never overrides it. The old CHECK constraint is therefore a spec-only
-- mismatch, not an active data-compatibility problem, but we still handle
-- any 'SUBMITTED' rows defensively below in case of manually-inserted or
-- externally-loaded data, mapping them to the closest canonical
-- equivalent (IN_REVIEW) before tightening the constraint.
--
-- V10/V11 are NOT modified; this migration only alters the constraints.

UPDATE test_steps SET approval_status = 'IN_REVIEW' WHERE approval_status = 'SUBMITTED';
UPDATE test_procedures SET approval_status = 'IN_REVIEW' WHERE approval_status = 'SUBMITTED';

ALTER TABLE test_steps DROP CONSTRAINT IF EXISTS chk_test_step_approval_status;
ALTER TABLE test_steps ADD CONSTRAINT chk_test_step_approval_status
    CHECK (approval_status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'SUPERSEDED'));

ALTER TABLE test_procedures DROP CONSTRAINT IF EXISTS chk_test_procedure_entity_approval_status;
ALTER TABLE test_procedures ADD CONSTRAINT chk_test_procedure_entity_approval_status
    CHECK (approval_status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'SUPERSEDED'));
