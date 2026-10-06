"""Deterministic similarity-based recommendations for AI-242.

This deliberately uses transparent feature engineering rather than a generative
model. Similarity is calculated over LRU name, subsystem, interfaces and
protocols, then historical fault/test evidence is aggregated by score.
"""

from __future__ import annotations

from collections import defaultdict

import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from app.advisory_models import (
    AdvisoryRecommendationRequest,
    AdvisoryRecommendationResponse,
    CandidateTestCaseRecommendation,
    ConfidenceEvidence,
    FaultScenarioRecommendation,
    SimilarLruEvidence,
)

ADVISORY_VERSION = "1.0"
METHOD = "TF-IDF cosine similarity over LRU name, subsystem, interfaces, and protocols"
MIN_SIMILARITY = 0.20
MAX_CORROBORATION_UPLIFT = 0.20
DISCLAIMER = (
    "Recommendations are decision-support only. A Test Engineer must review, "
    "adapt, and approve every suggested fault scenario and test case."
)


def recommend(request: AdvisoryRecommendationRequest) -> AdvisoryRecommendationResponse:
    """Produce stable recommendations from the supplied historical LRU cohort."""
    historical = pd.DataFrame([
        {
            "lru_id": item.lru_id,
            "name": item.name,
            "feature_text": _feature_text(item.name, item.subsystem, item.interfaces, item.protocols),
            "fault_scenarios": item.fault_scenarios,
            "test_cases": item.test_cases,
        }
        for item in request.historical_lrus
    ])
    target_features = _feature_text(
        request.target_lru_name, request.subsystem, request.interfaces, request.protocols
    )
    vectorizer = TfidfVectorizer(lowercase=True, token_pattern=r"(?u)\b[\w.-]+\b")
    matrix = vectorizer.fit_transform([target_features, *historical["feature_text"].tolist()])
    historical["similarity"] = cosine_similarity(matrix[0:1], matrix[1:]).flatten()
    historical = historical.sort_values(["similarity", "lru_id"], ascending=[False, True]).reset_index(drop=True)

    similar_lrus = [
        SimilarLruEvidence(
            lru_id=int(row.lru_id), name=str(row.name), similarity_score=_score(row.similarity)
        )
        for row in historical.itertuples()
        if row.similarity >= MIN_SIMILARITY
    ]
    known = {_normalise(value) for value in request.known_fault_scenarios}
    scenario_support: dict[str, list[SimilarLruEvidence]] = defaultdict(list)
    scenario_labels: dict[str, str] = {}
    test_support: dict[tuple[str, str], list[SimilarLruEvidence]] = defaultdict(list)
    test_objectives: dict[tuple[str, str], str | None] = {}
    test_titles: dict[tuple[str, str], str] = {}

    for row in historical.itertuples():
        if row.similarity < MIN_SIMILARITY:
            continue
        evidence = SimilarLruEvidence(
            lru_id=int(row.lru_id), name=str(row.name), similarity_score=_score(row.similarity)
        )
        for scenario in row.fault_scenarios:
            key = _normalise(scenario)
            if key not in known:
                scenario_support[key].append(evidence)
                scenario_labels.setdefault(key, scenario)
        for test_case in row.test_cases:
            scenario_key = _normalise(test_case.fault_scenario)
            if scenario_key not in known:
                key = (_normalise(test_case.title), scenario_key)
                test_support[key].append(evidence)
                test_objectives.setdefault(key, test_case.objective)
                test_titles.setdefault(key, test_case.title)
                scenario_labels.setdefault(scenario_key, test_case.fault_scenario)

    missing_faults = [
        FaultScenarioRecommendation(
            fault_scenario=scenario_labels[key],
            confidence=_confidence(evidence).score,
            confidence_evidence=_confidence(evidence),
            supporting_lrus=evidence,
            rationale=_rationale("fault scenario", scenario_labels[key], evidence),
        )
        for key, evidence in scenario_support.items()
    ]
    missing_faults.sort(key=lambda item: (-item.confidence, item.fault_scenario.casefold()))

    candidates = []
    for test_key, evidence in test_support.items():
        title_key, scenario_key = test_key
        title = test_titles[test_key]
        candidates.append(CandidateTestCaseRecommendation(
            title=title,
            objective=test_objectives[test_key],
            fault_scenario=scenario_labels[scenario_key],
            confidence=_confidence(evidence).score,
            confidence_evidence=_confidence(evidence),
            supporting_lrus=evidence,
            rationale=_rationale("test case", title, evidence),
        ))
    candidates.sort(key=lambda item: (-item.confidence, item.title.casefold()))

    limit = request.max_recommendations
    return AdvisoryRecommendationResponse(
        advisory_version=ADVISORY_VERSION,
        target_lru_id=request.target_lru_id,
        method=METHOD,
        similar_lrus=similar_lrus[:limit],
        missing_fault_scenarios=missing_faults[:limit],
        candidate_test_cases=candidates[:limit],
        disclaimer=DISCLAIMER,
    )


def _feature_text(name: str, subsystem: str | None, interfaces: list[str], protocols: list[str]) -> str:
    return " ".join(filter(None, [name, subsystem or "", *interfaces, *protocols]))


def _normalise(value: str) -> str:
    return " ".join(value.casefold().split())


def _score(value: float) -> float:
    return round(float(value), 4)


def _confidence(evidence: list[SimilarLruEvidence]) -> ConfidenceEvidence:
    """Score similarity evidence without treating a recommendation as approval.

    The closest comparable LRU is the primary signal. Independent additional
    LRUs can provide at most a 20% uplift, preventing a large cohort of weak
    matches from outscoring one genuinely strong match.
    """
    highest_similarity = max(item.similarity_score for item in evidence)
    supporting_count = len(evidence)
    corroboration_uplift = min(
        MAX_CORROBORATION_UPLIFT,
        (supporting_count - 1) * 0.10,
    )
    score = _score(highest_similarity + (1 - highest_similarity) * corroboration_uplift)
    band = "HIGH" if score >= 0.75 else "MEDIUM" if score >= 0.50 else "LOW"
    return ConfidenceEvidence(
        score=score,
        band=band,
        highest_similarity=_score(highest_similarity),
        supporting_lru_count=supporting_count,
        corroboration_uplift=_score(corroboration_uplift),
    )


def _rationale(kind: str, label: str, evidence: list[SimilarLruEvidence]) -> str:
    names = ", ".join(item.name for item in evidence[:3])
    return f"Recommended {kind} '{label}' from similarity-matched historical LRUs: {names}."
