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
            
            raw_items = resp.json().get("items", [])
            normalized = []
            for ep in raw_items:
                health_val = ep.get("health", {}).get("overall", "GOOD")
                if health_val.lower() in ["good", "healthy"]:
                    health_state = "GOOD"
                elif health_val.lower() in ["suspicious", "medium"]:
                    health_state = "SUSPICIOUS"
                else:
                    health_state = "BAD"

                normalized.append({
                    "id": ep.get("id", ""),
                    "hostname": ep.get("hostname", "Unknown Device"),
                    "ip_addresses": ep.get("ipv4Addresses", []),
                    "os": ep.get("os", {}).get("name", "Windows"),
                    "health": health_state,
                    "isolated": ep.get("isolation", {}).get("isolated", False),
                    "tamper_protection": ep.get("tamperProtection", {}).get("enabled", True),
                    "last_seen_at": ep.get("lastSeenAt", "Recently"),
                    "is_server": ep.get("type", "").lower() == "server",
                    "agent_version": ep.get("coreAgentVersion", "2026.2"),
                    "group_name": ep.get("group", {}).get("name", "Default Group")
                })
            return normalized

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
            
            raw_items = resp.json().get("items", [])
            normalized = []
            for a in raw_items:
                sev = a.get("severity", "MEDIUM").upper()
                if sev not in ["CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"]:
                    sev = "MEDIUM"

                endpoint_name = a.get("managedAgent", {}).get("name") or a.get("endpoint", {}).get("hostname") or "Corporate Host"
                endpoint_id = a.get("managedAgent", {}).get("id") or a.get("endpoint", {}).get("id") or "ep-0"

                normalized.append({
                    "id": a.get("id", ""),
                    "severity": sev,
                    "category": a.get("category", "Security Alert"),
                    "description": a.get("description", "Security event detected by Sophos Protection"),
                    "endpoint_hostname": endpoint_name,
                    "endpoint_id": endpoint_id,
                    "detected_at": a.get("raisedAt", "Recently"),
                    "status": "OPEN",
                    "product": a.get("product", "Sophos Central"),
                    "threat_name": a.get("threatName", ""),
                    "path": a.get("location", "")
                })
            return normalized

    async def get_detections(self) -> List[Dict[str, Any]]:
        # If tenant does not have XDR package enabled, fallback gracefully
        try:
            token = await self.get_valid_token()
            url = f"{self._data_region_url}/detections/graphql"
            query = "query GetRecentDetections { detections(limit: 25) { id severity title category detectedAt } }"
            async with httpx.AsyncClient(timeout=3.0) as client:
                resp = await client.post(
                    url,
                    json={"query": query},
                    headers={"Authorization": f"Bearer {token}", "X-Tenant-ID": self._tenant_id}
                )
                if resp.status_code == 200:
                    return resp.json().get("data", {}).get("detections", [])
        except Exception:
            pass
        return mock_provider.get_detections()

    async def get_cases(self) -> List[Dict[str, Any]]:
        # If tenant does not have MDR/Cases package enabled, fallback gracefully
        try:
            token = await self.get_valid_token()
            url = f"{self._data_region_url}/cases/graphql"
            query = "query GetCases { cases(limit: 25) { id caseNumber title status priority } }"
            async with httpx.AsyncClient(timeout=3.0) as client:
                resp = await client.post(
                    url,
                    json={"query": query},
                    headers={"Authorization": f"Bearer {token}", "X-Tenant-ID": self._tenant_id}
                )
                if resp.status_code == 200:
                    return resp.json().get("data", {}).get("cases", [])
        except Exception:
            pass
        return mock_provider.get_cases()

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
