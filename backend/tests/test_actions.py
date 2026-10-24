import pytest
import uuid
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_responder_execute_isolation_success(client: AsyncClient, responder_token: str):
    key = str(uuid.uuid4())
    response = await client.post(
        "/api/v1/response-actions",
        json={
            "action_type": "ISOLATE_ENDPOINT",
            "target_id": "ep-us-prod-01",
            "target_name": "SEC-CORP-DC01",
            "idempotency_key": key,
            "note": "Quarantine suspicious DC replication"
        },
        headers={"Authorization": f"Bearer {responder_token}"}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "SUCCESS"
    assert data["target_id"] == "ep-us-prod-01"

@pytest.mark.asyncio
async def test_idempotency_duplicate_prevention(client: AsyncClient, responder_token: str):
    key = str(uuid.uuid4())
    payload = {
        "action_type": "ISOLATE_ENDPOINT",
        "target_id": "ep-us-prod-03",
        "target_name": "ENG-UBUNTU-DEV08",
        "idempotency_key": key,
        "note": "Duplicate check"
    }
    # First execution
    r1 = await client.post("/api/v1/response-actions", json=payload, headers={"Authorization": f"Bearer {responder_token}"})
    assert r1.status_code == 200
    assert r1.json()["status"] == "SUCCESS"

    # Second execution with exact same idempotency key
    r2 = await client.post("/api/v1/response-actions", json=payload, headers={"Authorization": f"Bearer {responder_token}"})
    assert r2.status_code == 200
    assert r2.json()["status"] == "SUCCESS_DUPLICATE_IGNORED"
