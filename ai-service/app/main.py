"""IATERAP AI Advisory microservice.

AI-165 / T5-B2: FastAPI skeleton, architecturally independent from the
Spring Boot backend. Exposes a deterministic /suggest stub and a health
check. No real AI logic, no external model calls -- see AI-175 for the
read-only context contract that a future Coverage Engine will consume.
"""

from __future__ import annotations

from datetime import datetime, timezone

from fastapi import FastAPI

from app.context_models import (
    CONTEXT_CONTRACT_VERSION,
    CoverageContext,
    CoverageGapContext,
    TestProcedureContext,
    TestStepContext,
)
from app.models import HealthResponse, SuggestRequest, SuggestResponse

SERVICE_NAME = "iaterap-ai-service"
SERVICE_VERSION = "0.1.0"

app = FastAPI(
    title="IATERAP AI Advisory Service",
    description=(
        "Architecturally independent AI advisory microservice for IATERAP. "
        "Currently a skeleton: /suggest returns a deterministic canned "
        "response and /context/test-step exposes a read-only, versionable "
        "context contract for a future Coverage Engine."
    ),
    version=SERVICE_VERSION,
)


@app.get("/health", response_model=HealthResponse, tags=["health"])
def health() -> HealthResponse:
    """Health check, consistent with the backend's existing HealthController convention."""
    return HealthResponse(status="UP", service=SERVICE_NAME, version=SERVICE_VERSION)


@app.post("/suggest", response_model=SuggestResponse, tags=["advisory"])
def suggest(request: SuggestRequest) -> SuggestResponse:
    """Stub advisory endpoint.

    Always returns the same deterministic canned response regardless of
    input. This intentionally does NOT call any external model or perform
    inference -- it exists to prove out the service skeleton, container,
    and contract shape for later sprints.
    """
    return SuggestResponse(
        suggestion=(
            "This is a placeholder suggestion from the IATERAP AI Advisory "
            "skeleton service. No inference has been performed."
        ),
        confidence=0.0,
        source="stub",
        generated_at=datetime.now(timezone.utc),
    )


@app.get(
    "/context/test-procedure/{test_procedure_id}",
    response_model=TestProcedureContext,
    tags=["context"],
)
def get_test_procedure_context(test_procedure_id: int) -> TestProcedureContext:
    """AI-175 / T6-B3: read-only context contract.

    This is a CONTRACT endpoint, not the future Coverage Engine. It defines
    the stable, versionable shape (`contract_version`) a downstream AI
    process would receive to reason about a TestProcedure and its ordered
    TestSteps -- interface/connector/signal/protocol/fault-category
    references, acceptance criteria, and approval status.

    It performs no database writes and, at this skeleton stage, returns a
    deterministic example payload rather than live backend data so the
    contract shape can be reviewed and versioned independently of the
    Coverage Engine implementation.
    """
    example_steps = [
        TestStepContext(
            test_step_id=1,
            execution_order=1,
            interface_name="ARINC429_BUS_A",
            connector_name="J1-CONN",
            signal_library_name="ALT_ENCODED",
            protocol_tag_name="ARINC429",
            fault_category_name="SIGNAL_OUT_OF_RANGE",
            expected_behaviour="Altitude signal stabilizes within tolerance.",
            expected_range="0-50000 ft",
            acceptance_criteria="Reading within +/-50 ft of reference.",
            pass_fail_criteria="Pass if within tolerance for 10s continuous.",
            approval_status="DRAFT",
        )
    ]

    return TestProcedureContext(
        contract_version=CONTEXT_CONTRACT_VERSION,
        test_procedure_id=test_procedure_id,
        name="Example Test Procedure",
        description="Deterministic example payload for the read-only AI context contract.",
        lru_id=1,
        approval_status="DRAFT",
        version_number=1,
        step_count=len(example_steps),
        steps=example_steps,
        coverage_context=CoverageContext(
            categories={"COVERAGE-INTERFACE": 100.0},
            weights={"COVERAGE-INTERFACE": 0.15},
            composite_score=15.0,
            threshold_results={"COVERAGE-INTERFACE": True},
            unavailable_reasons={},
            coverage_gaps=[CoverageGapContext(
                element_type="SIGNAL",
                element_id=2,
                description="Unreferenced signal: ALT_DISCRETE",
            )],
        ),
        retrieved_at=datetime.now(timezone.utc),
    )
