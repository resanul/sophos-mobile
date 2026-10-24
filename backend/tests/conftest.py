import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.core.security import create_access_token
from app.core.config import settings
from app.services.sophos_client import sophos_client

@pytest.fixture(autouse=True)
def setup_test_env():
    prev_env = settings.APP_ENV
    prev_mock = settings.SOPHOS_MOCK_MODE
    
    settings.APP_ENV = "testing"
    settings.SOPHOS_MOCK_MODE = True
    
    yield
    
    settings.APP_ENV = prev_env
    settings.SOPHOS_MOCK_MODE = prev_mock

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
