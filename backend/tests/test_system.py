import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_system_health(client: AsyncClient):
    response = await client.get("/api/v1/system/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "HEALTHY"
    assert "version" in data
    assert "sophos_mode" in data

@pytest.mark.asyncio
async def test_system_capabilities(client: AsyncClient):
    response = await client.get("/api/v1/system/capabilities")
    assert response.status_code == 200
    data = response.json()
    assert data["endpoint_isolation"] is True
    assert data["detections_graphql"] is True
