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


def test_openapi_schema_exposes_all_endpoints():
    response = client.get("/openapi.json")
    assert response.status_code == 200
    paths = response.json()["paths"]
    assert "/health" in paths
    assert "/suggest" in paths
    assert "/context/test-procedure/{test_procedure_id}" in paths
