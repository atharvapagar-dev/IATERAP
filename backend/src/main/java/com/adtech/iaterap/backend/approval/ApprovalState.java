package com.adtech.iaterap.backend.approval;

/**
 * Canonical approval workflow states shared by every versioned engineering
 * artifact (TestStep, TestProcedure, and future artifacts).
 *
 * <p>REJECTED and SUPERSEDED are terminal: no legal transition currently
 * leaves either state. If a future requirement needs a "revise a rejected
 * item" flow, that must go through creating a new DRAFT artifact/revision,
 * not through re-opening a REJECTED record in place.</p>
 */
public enum ApprovalState {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    SUPERSEDED
}
