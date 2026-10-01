"""Read-only context contract models for AI-175 / T6-B3.

This is a CONTRACT, not an implementation of the future Coverage Engine.
It defines a stable, versionable, read-only shape that a downstream AI
process could consume to reason about a TestProcedure and its TestSteps.
No inference, no database writes -- see PART X of the assignment.

Field names mirror the actual IATERAP domain model (TestStep, TestProcedure)
as implemented in the Spring Boot backend (backend/.../entity/TestStep.java,
TestProcedure.java), not invented terminology.
"""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field

CONTEXT_CONTRACT_VERSION = "1.0"


class TestStepContext(BaseModel):
    """Read-only view of a single TestStep, for AI context purposes."""

    test_step_id: int
    execution_order: int
    interface_name: Optional[str] = None
    connector_name: Optional[str] = None
    signal_library_name: Optional[str] = None
    protocol_tag_name: Optional[str] = None
    fault_category_name: Optional[str] = None
    expected_behaviour: str
    expected_range: Optional[str] = None
    acceptance_criteria: str
    pass_fail_criteria: str
    approval_status: str


class CoverageGapContext(BaseModel):
    """Deterministic uncovered element supplied by the backend."""

    element_type: str
    element_id: int
    description: str


class CoverageContext(BaseModel):
    """Coverage evidence; it contains no mutable backend handle."""

    categories: dict[str, Optional[float]] = Field(default_factory=dict)
    weights: dict[str, float] = Field(default_factory=dict)
    composite_score: Optional[float] = None
    threshold_results: dict[str, Optional[bool]] = Field(default_factory=dict)
    unavailable_reasons: dict[str, str] = Field(default_factory=dict)
    coverage_gaps: list[CoverageGapContext] = Field(default_factory=list)


class TestProcedureContext(BaseModel):
    """Read-only, versioned context snapshot for a TestProcedure.

    This is what a future Coverage Engine would be handed to reason about
    a procedure's current content -- it is not a live/mutable handle, and
    this endpoint performs no writes.
    """

    contract_version: str = Field(default=CONTEXT_CONTRACT_VERSION)
    test_procedure_id: int
    name: str
    description: Optional[str] = None
    lru_id: int
    approval_status: str
    version_number: int
    step_count: int
    steps: list[TestStepContext]
    coverage_context: Optional[CoverageContext] = None
    retrieved_at: datetime
