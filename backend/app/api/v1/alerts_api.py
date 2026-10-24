from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from app.schemas.schemas import AlertSchema
from app.api.v1.dependencies import get_current_user
from app.services.sophos_client import sophos_client

router = APIRouter(prefix="/alerts", tags=["Alerts"])

@router.get("", response_model=List[AlertSchema])
async def list_alerts(
    severity: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    user: dict = Depends(get_current_user)
):
    alerts = await sophos_client.get_alerts()
    results = []
    for a in alerts:
        if severity and a.get("severity") != severity.upper():
            continue
        if status and a.get("status") != status.upper():
            continue
        results.append(AlertSchema(**a))
    return results

@router.get("/{alert_id}", response_model=AlertSchema)
async def get_alert_detail(alert_id: str, user: dict = Depends(get_current_user)):
    alerts = await sophos_client.get_alerts()
    for a in alerts:
        if a["id"] == alert_id:
            return AlertSchema(**a)
    raise HTTPException(status_code=404, detail=f"Alert {alert_id} not found")
