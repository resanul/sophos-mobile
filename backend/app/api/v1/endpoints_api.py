from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from app.schemas.schemas import EndpointSchema
from app.api.v1.dependencies import get_current_user
from app.services.sophos_client import sophos_client

router = APIRouter(prefix="/endpoints", tags=["Endpoints"])

@router.get("", response_model=List[EndpointSchema])
async def list_endpoints(
    search: Optional[str] = Query(None),
    is_server: Optional[bool] = Query(None),
    isolated: Optional[bool] = Query(None),
    user: dict = Depends(get_current_user)
):
    endpoints = await sophos_client.get_endpoints()
    results = []
    for ep in endpoints:
        if search:
            query = search.lower()
            matches_host = query in ep.get("hostname", "").lower()
            matches_ip = any(query in ip for ip in ep.get("ip_addresses", []))
            matches_os = query in ep.get("os", "").lower()
            if not (matches_host or matches_ip or matches_os):
                continue

        if is_server is not None and ep.get("is_server") != is_server:
            continue

        if isolated is not None and ep.get("isolated") != isolated:
            continue

        results.append(EndpointSchema(**ep))
    return results

@router.get("/{endpoint_id}", response_model=EndpointSchema)
async def get_endpoint_detail(endpoint_id: str, user: dict = Depends(get_current_user)):
    endpoints = await sophos_client.get_endpoints()
    for ep in endpoints:
        if ep["id"] == endpoint_id:
            return EndpointSchema(**ep)
    raise HTTPException(status_code=404, detail=f"Endpoint {endpoint_id} not found")
