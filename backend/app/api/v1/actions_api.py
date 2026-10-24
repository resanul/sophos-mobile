import uuid
from fastapi import APIRouter, Depends, HTTPException, status
from app.schemas.schemas import ResponseActionCreate, ResponseActionResponse
from app.api.v1.dependencies import require_role
from app.core.config import settings
from app.services.sophos_client import sophos_client

router = APIRouter(prefix="/response-actions", tags=["Response Actions"])

# In-memory action journal & idempotency tracking
executed_idempotency_keys = {}

@router.post("", response_model=ResponseActionResponse)
async def execute_response_action(
    payload: ResponseActionCreate,
    user: dict = Depends(require_role(["responder", "admin"]))
):
    # 1. Emergency Freeze Guard
    if settings.EMERGENCY_RESPONSE_ACTIONS_FROZEN:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Emergency Freeze Active: All destructive response actions are locked by administrator policy."
        )

    # 2. Idempotency Check
    if payload.idempotency_key in executed_idempotency_keys:
        cached = executed_idempotency_keys[payload.idempotency_key]
        return ResponseActionResponse(
            action_id=cached["action_id"],
            status="SUCCESS_DUPLICATE_IGNORED",
            message=f"Action already executed previously under key {payload.idempotency_key}",
            target_id=payload.target_id,
            action_type=payload.action_type
        )

    # 3. Action Execution via Sophos API Client
    action_id = f"act-{uuid.uuid4().hex[:8]}"
    success = False

    if payload.action_type == "ISOLATE_ENDPOINT":
        success = await sophos_client.isolate_endpoint(payload.target_id)
    elif payload.action_type == "RESTORE_ENDPOINT":
        success = await sophos_client.restore_endpoint(payload.target_id)
    elif payload.action_type == "SCAN_ENDPOINT":
        success = True # Mock/Dispatch scan
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported action type '{payload.action_type}'"
        )

    if not success:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Sophos Central rejected or failed to process the response action command."
        )

    result = {
        "action_id": action_id,
        "target_id": payload.target_id,
        "action_type": payload.action_type,
        "status": "SUCCESS"
    }
    executed_idempotency_keys[payload.idempotency_key] = result

    return ResponseActionResponse(
        action_id=action_id,
        status="SUCCESS",
        message=f"Response action {payload.action_type} successfully executed and recorded.",
        target_id=payload.target_id,
        action_type=payload.action_type
    )
