-- AI-194 / T8-SH: keep the reviewer queue index focused on active reviews.
-- Historical DRAFT/APPROVED/REJECTED/SUPERSEDED rows do not belong in this
-- queue, so a partial index avoids carrying them in the review lookup path.
CREATE INDEX IF NOT EXISTS idx_test_procedures_in_review
    ON test_procedures (approval_status)
    WHERE approval_status = 'IN_REVIEW';
