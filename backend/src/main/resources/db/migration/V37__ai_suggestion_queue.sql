CREATE TABLE ai_suggestion_queue (
    id BIGSERIAL PRIMARY KEY,
    test_procedure_id BIGINT,
    test_step_id BIGINT,
    suggestion_text TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    source VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    requested_by VARCHAR(255) NOT NULL,
    decided_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP,
    CONSTRAINT ck_ai_suggestion_queue_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DISMISSED')),
    CONSTRAINT fk_ai_suggestion_queue_procedure FOREIGN KEY (test_procedure_id) REFERENCES test_procedures(id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_suggestion_queue_step FOREIGN KEY (test_step_id) REFERENCES test_steps(id) ON DELETE SET NULL
);

CREATE INDEX idx_ai_suggestion_queue_requester_status
    ON ai_suggestion_queue (requested_by, status, created_at DESC);
