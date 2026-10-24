from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException
from app.schemas.schemas import DetectionSchema
from app.api.v1.dependencies import get_current_user
from app.services.sophos_client import sophos_client

router = APIRouter(prefix="/detections", tags=["Detections"])

@router.get("", response_model=List[DetectionSchema])
async def list_detections(user: dict = Depends(get_current_user)):
    detections = await sophos_client.get_detections()
    return [DetectionSchema(**d) for d in detections]

@router.get("/{detection_id}", response_model=DetectionSchema)
async def get_detection_detail(detection_id: str, user: dict = Depends(get_current_user)):
    detections = await sophos_client.get_detections()
    for d in detections:
        if d["id"] == detection_id:
            return DetectionSchema(**d)
    raise HTTPException(status_code=404, detail=f"Detection {detection_id} not found")
