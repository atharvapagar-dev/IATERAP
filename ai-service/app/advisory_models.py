"""Input and output contracts for AI-242's explainable advisory engine.

The service consumes caller-supplied, read-only historical data. It never
persists a recommendation or changes a procedure, which keeps the final
engineering decision with the Test Engineer and approval workflow.
"""

from __future__ import annotations

from typing import Annotated

from pydantic import BaseModel, Field, model_validator


NonEmptyText = Annotated[str, Field(min_length=1, max_length=500)]


class HistoricalTestCase(BaseModel):
    """A test case that previously addressed a fault scenario on one LRU."""

    title: NonEmptyText
    objective: str | None = Field(default=None, max_length=2000)
    fault_scenario: NonEmptyText


class HistoricalLru(BaseModel):
    """Read-only historical evidence used as a similarity candidate."""

    lru_id: int = Field(gt=0)
    name: NonEmptyText
    subsystem: str | None = Field(default=None, max_length=500)
    interfaces: list[NonEmptyText] = Field(default_factory=list)
    protocols: list[NonEmptyText] = Field(default_factory=list)
    fault_scenarios: list[NonEmptyText] = Field(default_factory=list)
    test_cases: list[HistoricalTestCase] = Field(default_factory=list)


class AdvisoryRecommendationRequest(BaseModel):
    """Target LRU and a bounded historical cohort for deterministic scoring."""

    target_lru_id: int = Field(gt=0)
    target_lru_name: NonEmptyText
    subsystem: str | None = Field(default=None, max_length=500)
    interfaces: list[NonEmptyText] = Field(default_factory=list)
    protocols: list[NonEmptyText] = Field(default_factory=list)
    known_fault_scenarios: list[NonEmptyText] = Field(default_factory=list)
    historical_lrus: list[HistoricalLru] = Field(min_length=1, max_length=500)
    max_recommendations: int = Field(default=5, ge=1, le=20)

    @model_validator(mode="after")
    def target_must_not_be_in_historical_cohort(self) -> "AdvisoryRecommendationRequest":
        if self.target_lru_id in {lru.lru_id for lru in self.historical_lrus}:
            raise ValueError("target_lru_id must not also appear in historical_lrus")
        return self


class SimilarLruEvidence(BaseModel):
    lru_id: int
    name: str
    similarity_score: float = Field(ge=0.0, le=1.0)


class FaultScenarioRecommendation(BaseModel):
    fault_scenario: str
    confidence: float = Field(ge=0.0, le=1.0)
    supporting_lrus: list[SimilarLruEvidence] = Field(min_length=1)
    rationale: str


class CandidateTestCaseRecommendation(BaseModel):
    title: str
    objective: str | None = None
    fault_scenario: str
    confidence: float = Field(ge=0.0, le=1.0)
    supporting_lrus: list[SimilarLruEvidence] = Field(min_length=1)
    rationale: str


class AdvisoryRecommendationResponse(BaseModel):
    advisory_version: str
    target_lru_id: int
    method: str
    similar_lrus: list[SimilarLruEvidence]
    missing_fault_scenarios: list[FaultScenarioRecommendation]
    candidate_test_cases: list[CandidateTestCaseRecommendation]
    disclaimer: str
