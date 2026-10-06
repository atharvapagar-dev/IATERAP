"""AI-244 regression suite for the deterministic advisory recommendation core."""

from app.advisory_models import AdvisoryRecommendationRequest
from app.advisory_service import MIN_SIMILARITY, recommend


def _recommend(payload):
    return recommend(AdvisoryRecommendationRequest.model_validate(payload))


def test_seeded_historical_procedures_rank_comparable_lrus_first(flight_control_advisory_payload):
    response = _recommend(flight_control_advisory_payload)

    assert [item.lru_id for item in response.similar_lrus] == [101, 102]
    assert all(item.similarity_score >= MIN_SIMILARITY for item in response.similar_lrus)


def test_seeded_fixture_recommends_only_missing_fault_scenarios(flight_control_advisory_payload):
    response = _recommend(flight_control_advisory_payload)

    faults = {item.fault_scenario: item for item in response.missing_fault_scenarios}
    assert set(faults) == {"Signal Loss", "Data Bus Timeout"}
    assert "Voltage Drop" not in faults
    assert "Overcurrent" not in faults
    assert faults["Signal Loss"].confidence_evidence.supporting_lru_count == 2
    assert faults["Signal Loss"].confidence_evidence.corroboration_uplift == 0.1
    assert faults["Signal Loss"].confidence == faults["Signal Loss"].confidence_evidence.score


def test_seeded_fixture_returns_candidate_test_cases_with_traceable_evidence(flight_control_advisory_payload):
    response = _recommend(flight_control_advisory_payload)

    tests = {item.title: item for item in response.candidate_test_cases}
    signal_loss = tests["Verify ARINC signal-loss recovery"]
    assert signal_loss.fault_scenario == "Signal Loss"
    assert [item.lru_id for item in signal_loss.supporting_lrus] == [101, 102]
    assert signal_loss.confidence_evidence.band in {"MEDIUM", "HIGH"}
    assert "Synthetic Flight Control Computer Variant A" in signal_loss.rationale


def test_suggestions_are_deterministic_for_the_same_seeded_fixture(clone_payload):
    first = _recommend(clone_payload()).model_dump(mode="json")
    second = _recommend(clone_payload()).model_dump(mode="json")

    assert first == second


def test_max_recommendations_limits_every_advisory_collection(clone_payload):
    payload = clone_payload()
    payload["max_recommendations"] = 1

    response = _recommend(payload)

    assert len(response.similar_lrus) == 1
    assert len(response.missing_fault_scenarios) == 1
    assert len(response.candidate_test_cases) == 1


def test_unrelated_seeded_history_produces_no_suggestions(clone_payload):
    payload = clone_payload()
    payload["target_lru_id"] = 901
    payload["target_lru_name"] = "Synthetic Cabin Lighting Controller"
    payload["subsystem"] = "Synthetic Cabin"
    payload["interfaces"] = ["PWM Lighting"]
    payload["protocols"] = ["LIN"]

    response = _recommend(payload)

    assert response.similar_lrus == []
    assert response.missing_fault_scenarios == []
    assert response.candidate_test_cases == []
