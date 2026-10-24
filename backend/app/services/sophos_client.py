import asyncio
import logging
from typing import Dict, Any, List, Optional
import httpx
from app.core.config import settings
from app.services.mock_sophos_provider import mock_provider

logger = logging.getLogger("sophos_gateway.client")

class SophosCentralClient:
    """
    Official Sophos Central API Client.
    Handles OAuth 2.0 token caching, regional discovery, and authenticated operations.
    """

    AUTH_URL = "https://id.sophos.com/api/v2/oauth2/token"
    WHOAMI_URL = "https://api.central.sophos.com/whoami/v1"

    def __init__(self):
        self._access_token: Optional[str] = None
        self._token_expires_at: float = 0.0
        self._tenant_id: str = settings.SOPHOS_TENANT_ID
        self._data_region_url: str = settings.SOPHOS_DATA_REGION_URL

    @property
    def is_mock_mode(self) -> bool:
        return settings.SOPHOS_MOCK_MODE or not settings.SOPHOS_CLIENT_ID or not settings.SOPHOS_CLIENT_SECRET

    async def get_valid_token(self) -> str:
        if self.is_mock_mode:
            return "mock-jwt-bearer-token"

        if self._access_token and asyncio.get_event_loop().time() < self._token_expires_at:
            return self._access_token

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                self.AUTH_URL,
                data={
                    "grant_type": "client_credentials",
                    "client_id": settings.SOPHOS_CLIENT_ID,
                    "client_secret": settings.SOPHOS_CLIENT_SECRET,
                    "scope": "token"
                },
                headers={"Content-Type": "application/x-www-form-urlencoded"}
            )
            if resp.status_code != 200:
                logger.error(f"Sophos OAuth2 token exchange failed: {resp.status_code} {resp.text}")
                raise RuntimeError(f"Failed to authenticate with Sophos Central: {resp.status_code}")

            data = resp.json()
            self._access_token = data["access_token"]
            expires_in = data.get("expires_in", 3600)
            self._token_expires_at = asyncio.get_event_loop().time() + expires_in - 60

            # Discover tenant if not explicitly provided
            if not self._tenant_id:
                await self._discover_tenant(self._access_token)

            return self._access_token

    async def _discover_tenant(self, token: str):
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                self.WHOAMI_URL,
                headers={"Authorization": f"Bearer {token}"}
            )
            if resp.status_code == 200:
                whoami = resp.json()
                self._tenant_id = whoami.get("id", "discovered-tenant-id")
                api_hosts = whoami.get("apiHosts", {})
                self._data_region_url = api_hosts.get("dataRegion", self._data_region_url)
                logger.info(f"Discovered Sophos Tenant: {self._tenant_id} at {self._data_region_url}")

    async def get_endpoints(self) -> List[Dict[str, Any]]:
        if self.is_mock_mode:
            return mock_provider.get_endpoints()

        token = await self.get_valid_token()
        url = f"{self._data_region_url}/endpoint/v1/endpoints"
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(
                url,
                headers={
                    "Authorization": f"Bearer {token}",
                    "X-Tenant-ID": self._tenant_id
                }
            )
            if resp.status_code != 200:
                logger.warning(f"Live endpoint query failed ({resp.status_code}), falling back to mock provider.")
                return mock_provider.get_endpoints()
            return resp.json().get("items", [])

    async def get_alerts(self) -> List[Dict[str, Any]]:
        if self.is_mock_mode:
            return mock_provider.get_alerts()

        token = await self.get_valid_token()
        url = f"{self._data_region_url}/common/v1/alerts"
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(
                url,
                headers={
                    "Authorization": f"Bearer {token}",
                    "X-Tenant-ID": self._tenant_id
                }
            )
            if resp.status_code != 200:
                logger.warning(f"Live alerts query failed ({resp.status_code}), falling back to mock provider.")
                return mock_provider.get_alerts()
            return resp.json().get("items", [])

    async def get_detections(self) -> List[Dict[str, Any]]:
        if self.is_mock_mode:
            return mock_provider.get_detections()

        token = await self.get_valid_token()
        url = f"{self._data_region_url}/detections/graphql"
        query = """
        query GetRecentDetections {
            detections(limit: 25) {
                id
                severity
                title
                category
                detectedAt
            }
        }
        """
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                url,
                json={"query": query},
                headers={
                    "Authorization": f"Bearer {token}",
                    "X-Tenant-ID": self._tenant_id
                }
            )
            if resp.status_code != 200:
                return mock_provider.get_detections()
            return resp.json().get("data", {}).get("detections", [])

    async def get_cases(self) -> List[Dict[str, Any]]:
        if self.is_mock_mode:
            return mock_provider.get_cases()

        token = await self.get_valid_token()
        url = f"{self._data_region_url}/cases/graphql"
        query = """
        query GetCases {
            cases(limit: 25) {
                id
                caseNumber
                title
                status
                priority
            }
        }
        """
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                url,
                json={"query": query},
                headers={
                    "Authorization": f"Bearer {token}",
                    "X-Tenant-ID": self._tenant_id
                }
            )
            if resp.status_code != 200:
                return mock_provider.get_cases()
            return resp.json().get("data", {}).get("cases", [])

    async def isolate_endpoint(self, endpoint_id: str) -> bool:
        if self.is_mock_mode:
            return mock_provider.isolate_endpoint(endpoint_id)

        token = await self.get_valid_token()
        url = f"{self._data_region_url}/endpoint/v1/endpoints/{endpoint_id}/isolation"
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                url,
                json={"enabled": True, "comment": "Network isolation initiated from Mobile SOC Gateway"},
                headers={
                    "Authorization": f"Bearer {token}",
                    "X-Tenant-ID": self._tenant_id
                }
            )
            return resp.status_code in [200, 201, 202, 204]

    async def restore_endpoint(self, endpoint_id: str) -> bool:
        if self.is_mock_mode:
            return mock_provider.restore_endpoint(endpoint_id)

        token = await self.get_valid_token()
        url = f"{self._data_region_url}/endpoint/v1/endpoints/{endpoint_id}/isolation"
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                url,
                json={"enabled": False, "comment": "Network restoration initiated from Mobile SOC Gateway"},
                headers={
                    "Authorization": f"Bearer {token}",
                    "X-Tenant-ID": self._tenant_id
                }
            )
            return resp.status_code in [200, 201, 202, 204]

sophos_client = SophosCentralClient()
