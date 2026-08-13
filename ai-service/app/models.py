"""Pydantic request/response models for the IATERAP AI Advisory service."""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class SuggestRequest(BaseModel):
    """Request payload for the /suggest stub endpoint.

    AI-165 only requires a deterministic canned response, so this model is
    intentionally minimal. It is shaped after the existing TestStep domain
    so that a future real implementation can grow into it without a
    breaking contract change.
    """

    test_procedure_id: Optional[int] = Field(
        default=None,
        description="Optional TestProcedure ID the suggestion is being requested for.",
    )
    test_step_id: Optional[int] = Field(
        default=None,
        description="Optional TestStep ID the suggestion is being requested for.",
    )
    prompt: Optional[str] = Field(
        default=None,
        description="Optional free-text hint from the engineer.",
        max_length=2000,
    )


class SuggestResponse(BaseModel):
    """Deterministic canned response for the /suggest stub endpoint."""

    suggestion: str
    confidence: float = Field(ge=0.0, le=1.0)
    source: str = Field(default="stub", description="Always 'stub' until real AI logic lands.")
    generated_at: datetime


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
