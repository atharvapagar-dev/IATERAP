from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_returns_up():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert body["service"] == "iaterap-ai-service"


def test_suggest_returns_deterministic_canned_response():
    response = client.post("/suggest", json={})
    assert response.status_code == 200
    body = response.json()
    assert body["source"] == "stub"
    assert body["confidence"] == 0.0
    assert "placeholder suggestion" in body["suggestion"]


def test_suggest_accepts_optional_fields():
    response = client.post(
        "/suggest",
        json={"test_procedure_id": 1, "test_step_id": 2, "prompt": "hello"},
    )
    assert response.status_code == 200


def test_suggest_rejects_invalid_payload_shape():
    response = client.post("/suggest", json={"test_step_id": "not-an-int"})
    assert response.status_code == 422


def test_context_endpoint_returns_versioned_contract():
    response = client.get("/context/test-procedure/42")
    assert response.status_code == 200
    body = response.json()
    assert body["contract_version"] == "1.0"
    assert body["test_procedure_id"] == 42
    assert isinstance(body["steps"], list)
    assert body["step_count"] == len(body["steps"])
    assert "coverage_context" in body
    assert isinstance(body["coverage_context"]["coverage_gaps"], list)


def test_context_endpoint_is_get_only():
    for method in ("post", "put", "patch", "delete"):
        response = getattr(client, method)("/context/test-procedure/42")
        assert response.status_code == 405


def test_context_route_declares_no_write_methods():
    context_routes = [
        route for route in app.routes
        if route.path == "/context/test-procedure/{test_procedure_id}"
    ]
    assert len(context_routes) == 1
    assert context_routes[0].methods == {"GET"}


def test_advisory_recommends_missing_faults_and_candidate_test_cases():
    response = client.post(
        "/advisory/recommendations",
        json={
            "target_lru_id": 900,
            "target_lru_name": "Flight Control Computer Variant B",
            "subsystem": "Flight Controls",
            "interfaces": ["ARINC429", "Discrete I/O"],
            "protocols": ["ARINC429"],
            "known_fault_scenarios": ["Voltage Drop"],
            "historical_lrus": [
                {
                    "lru_id": 101,
                    "name": "Flight Control Computer Variant A",
                    "subsystem": "Flight Controls",
                    "interfaces": ["ARINC429", "Discrete I/O"],
                    "protocols": ["ARINC429"],
                    "fault_scenarios": ["Voltage Drop", "Signal Loss"],
                    "test_cases": [
                        {
                            "title": "Verify ARINC signal-loss recovery",
                            "objective": "Confirm recovery following signal interruption.",
                            "fault_scenario": "Signal Loss",
                        }
                    ],
                },
                {
                    "lru_id": 102,
                    "name": "Power Distribution Unit",
                    "subsystem": "Power",
                    "interfaces": ["Discrete I/O"],
                    "protocols": ["CAN"],
                    "fault_scenarios": ["Overcurrent"],
                    "test_cases": [],
                },
            ],
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["advisory_version"] == "1.0"
    assert body["similar_lrus"][0]["lru_id"] == 101
    assert [item["fault_scenario"] for item in body["missing_fault_scenarios"]] == ["Signal Loss"]
    fault_confidence = body["missing_fault_scenarios"][0]["confidence_evidence"]
    assert fault_confidence["score"] == body["missing_fault_scenarios"][0]["confidence"]
    assert fault_confidence["supporting_lru_count"] == 1
    assert fault_confidence["corroboration_uplift"] == 0.0
    assert fault_confidence["band"] in {"LOW", "MEDIUM", "HIGH"}
    assert body["candidate_test_cases"][0]["title"] == "Verify ARINC signal-loss recovery"
    assert body["candidate_test_cases"][0]["fault_scenario"] == "Signal Loss"
    assert body["candidate_test_cases"][0]["confidence_evidence"]["score"] == body["candidate_test_cases"][0]["confidence"]
    assert "Test Engineer" in body["disclaimer"]


def test_advisory_excludes_known_faults_and_rejects_target_in_history():
    payload = {
        "target_lru_id": 10,
        "target_lru_name": "Target LRU",
        "interfaces": ["CAN"],
        "known_fault_scenarios": ["Open Circuit"],
        "historical_lrus": [{
            "lru_id": 10,
            "name": "Target LRU duplicate",
            "interfaces": ["CAN"],
            "fault_scenarios": ["Open Circuit"],
        }],
    }
    response = client.post("/advisory/recommendations", json=payload)
    assert response.status_code == 422


def test_advisory_confidence_increases_for_independent_corrobation():
    response = client.post(
        "/advisory/recommendations",
        json={
            "target_lru_id": 500,
            "target_lru_name": "Navigation Computer Variant C",
            "subsystem": "Navigation",
            "interfaces": ["ARINC429"],
            "protocols": ["ARINC429"],
            "historical_lrus": [
                {
                    "lru_id": 501,
                    "name": "Navigation Computer Variant A",
                    "subsystem": "Navigation",
                    "interfaces": ["ARINC429"],
                    "protocols": ["ARINC429"],
                    "fault_scenarios": ["Data Bus Timeout"],
                    "test_cases": [{
                        "title": "Verify data-bus timeout handling",
                        "fault_scenario": "Data Bus Timeout",
                    }],
                },
                {
                    "lru_id": 502,
                    "name": "Navigation Computer Variant B",
                    "subsystem": "Navigation",
                    "interfaces": ["ARINC429"],
                    "protocols": ["ARINC429"],
                    "fault_scenarios": ["Data Bus Timeout"],
                    "test_cases": [{
                        "title": "Verify data-bus timeout handling",
                        "fault_scenario": "Data Bus Timeout",
                    }],
                },
            ],
        },
    )
    assert response.status_code == 200
    fault = response.json()["missing_fault_scenarios"][0]
    confidence = fault["confidence_evidence"]
    assert confidence["supporting_lru_count"] == 2
    assert confidence["corroboration_uplift"] == 0.1
    assert confidence["score"] > confidence["highest_similarity"]
    assert response.json()["candidate_test_cases"][0]["confidence_evidence"] == confidence


def test_openapi_schema_exposes_all_endpoints():
    response = client.get("/openapi.json")
    assert response.status_code == 200
    paths = response.json()["paths"]
    assert "/health" in paths
    assert "/suggest" in paths
    assert "/advisory/recommendations" in paths
    assert "/context/test-procedure/{test_procedure_id}" in paths
