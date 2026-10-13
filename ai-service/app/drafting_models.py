"""Read-only natural-language drafting contracts for AI-252."""
from pydantic import BaseModel, Field


class CoverageGapInput(BaseModel):
    element_type: str = Field(min_length=1, max_length=100)
    element_id: int = Field(gt=0)
    description: str = Field(min_length=1, max_length=2000)


class GapExplanationRequest(BaseModel):
    test_procedure_id: int = Field(gt=0)
    procedure_name: str = Field(min_length=1, max_length=500)
    gaps: list[CoverageGapInput] = Field(min_length=1, max_length=100)


class GapExplanationResponse(BaseModel):
    test_procedure_id: int
    explanation: str
    gap_count: int
    disclaimer: str


class DocumentationDraftRequest(BaseModel):
    test_procedure_id: int = Field(gt=0)
    procedure_name: str = Field(min_length=1, max_length=500)
    review_outcome: str = Field(min_length=1, max_length=100)
    findings: list[str] = Field(default_factory=list, max_length=100)
    engineer_notes: str | None = Field(default=None, max_length=4000)


class DocumentationDraftResponse(BaseModel):
    test_procedure_id: int
    draft: str
    disclaimer: str
