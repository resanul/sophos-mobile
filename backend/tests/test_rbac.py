import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_viewer_can_read_endpoints(client: AsyncClient, viewer_token: str):
    response = await client.get(
        "/api/v1/endpoints",
        headers={"Authorization": f"Bearer {viewer_token}"}
    )
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) > 0

@pytest.mark.asyncio
async def test_viewer_forbidden_from_isolation_action(client: AsyncClient, viewer_token: str):
    response = await client.post(
        "/api/v1/response-actions",
        json={
            "action_type": "ISOLATE_ENDPOINT",
            "target_id": "ep-us-prod-01",
            "target_name": "SEC-CORP-DC01",
            "idempotency_key": "test-key-01",
            "note": "Unauthorized attempt test"
        },
        headers={"Authorization": f"Bearer {viewer_token}"}
    )
    assert response.status_code == 403
    assert "Operation requires one of the following roles" in response.json()["detail"]
