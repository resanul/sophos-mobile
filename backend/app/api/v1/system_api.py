from fastapi import APIRouter
from app.schemas.schemas import HealthStatusSchema
from app.core.config import settings
from app.services.sophos_client import sophos_client

router = APIRouter(prefix="/system", tags=["System"])

@router.get("/health", response_model=HealthStatusSchema)
async def system_health():
    return HealthStatusSchema(
        status="HEALTHY",
        version="1.0.0",
        environment=settings.APP_ENV,
        sophos_mode="MOCK_SANDBOX" if sophos_client.is_mock_mode else "LIVE_SOPHOS_CENTRAL",
        data_region="US-EAST-01",
        database_connected=True
    )

@router.get("/capabilities")
async def system_capabilities():
    return {
        "endpoint_management": True,
        "endpoint_isolation": True,
        "endpoint_scan": True,
        "alerts_management": True,
        "detections_graphql": True,
        "cases_graphql": True,
        "data_lake_hunting": True,
        "firewall_management": False,
        "email_security": False
    }
