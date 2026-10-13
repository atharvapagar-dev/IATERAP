"""Deterministic drafting helpers; they do not persist or approve engineering work."""
from app.drafting_models import (
    DocumentationDraftRequest, DocumentationDraftResponse,
    GapExplanationRequest, GapExplanationResponse,
)

DISCLAIMER = "Draft advisory only. A Test Engineer must verify, edit, and approve it before use."


def explain_gaps(request: GapExplanationRequest) -> GapExplanationResponse:
    details = "; ".join(
        f"{gap.element_type} {gap.element_id}: {gap.description}" for gap in request.gaps
    )
    return GapExplanationResponse(
        test_procedure_id=request.test_procedure_id,
        gap_count=len(request.gaps),
        explanation=(f"Procedure '{request.procedure_name}' has {len(request.gaps)} coverage gap(s): "
                     f"{details}. Add or revise test steps only after engineering review."),
        disclaimer=DISCLAIMER,
    )


def draft_review_note(request: DocumentationDraftRequest) -> DocumentationDraftResponse:
    findings = "; ".join(request.findings) if request.findings else "No findings were supplied."
    notes = f" Engineer notes: {request.engineer_notes}" if request.engineer_notes else ""
    return DocumentationDraftResponse(
        test_procedure_id=request.test_procedure_id,
        draft=(f"Review note — Procedure '{request.procedure_name}' (ID {request.test_procedure_id}). "
               f"Outcome: {request.review_outcome}. Findings: {findings}.{notes}"),
        disclaimer=DISCLAIMER,
    )
