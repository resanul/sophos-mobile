from fastapi import APIRouter, Depends
from app.schemas.schemas import DashboardSummarySchema
from app.api.v1.dependencies import get_current_user
from app.services.sophos_client import sophos_client

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])

@router.get("/summary", response_model=DashboardSummarySchema)
async def get_dashboard_summary(user: dict = Depends(get_current_user)):
    endpoints = await sophos_client.get_endpoints()
    alerts = await sophos_client.get_alerts()
    detections = await sophos_client.get_detections()
    cases = await sophos_client.get_cases()

    healthy_count = sum(1 for ep in endpoints if ep.get("health") == "GOOD" and not ep.get("isolated", False))
    suspicious_count = sum(1 for ep in endpoints if ep.get("health") == "SUSPICIOUS")
    bad_count = sum(1 for ep in endpoints if ep.get("health") == "BAD")
    isolated_count = sum(1 for ep in endpoints if ep.get("isolated", False))

    critical_alerts = sum(1 for a in alerts if a.get("severity") == "CRITICAL" and a.get("status") == "OPEN")
    high_alerts = sum(1 for a in alerts if a.get("severity") == "HIGH" and a.get("status") == "OPEN")
    open_cases = sum(1 for c in cases if c.get("status") != "CLOSED")

    return DashboardSummarySchema(
        total_endpoints=len(endpoints),
        healthy_endpoints=healthy_count,
        suspicious_endpoints=suspicious_count,
        bad_endpoints=bad_count,
        isolated_endpoints=isolated_count,
        active_critical_alerts=critical_alerts,
        active_high_alerts=high_alerts,
        open_cases=open_cases,
        recent_detections_count=len(detections),
        sync_freshness_timestamp="Just now",
        is_live_data_source=not sophos_client.is_mock_mode,
        tenant_name="Enterprise Security Operations Tenant",
        data_region="us-east-1"
    )
