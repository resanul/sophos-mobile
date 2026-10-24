import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.core.security import create_access_token

@pytest_asyncio.fixture
async def client():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac

@pytest.fixture
def responder_token():
    return create_access_token(
        subject="test-responder@enterprise.local",
        role="responder",
        tenant_id="test-tenant"
    )

@pytest.fixture
def viewer_token():
    return create_access_token(
        subject="test-viewer@enterprise.local",
        role="viewer",
        tenant_id="test-tenant"
    )

@pytest.fixture
def admin_token():
    return create_access_token(
        subject="test-admin@enterprise.local",
        role="admin",
        tenant_id="test-tenant"
    )
