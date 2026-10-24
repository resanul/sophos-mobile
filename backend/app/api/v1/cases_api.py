from typing import List
from fastapi import APIRouter, Depends, HTTPException
from app.schemas.schemas import CaseSchema
from app.api.v1.dependencies import get_current_user
from app.services.sophos_client import sophos_client

router = APIRouter(prefix="/cases", tags=["XDR Cases"])

@router.get("", response_model=List[CaseSchema])
async def list_cases(user: dict = Depends(get_current_user)):
    cases = await sophos_client.get_cases()
    return [CaseSchema(**c) for c in cases]

@router.get("/{case_id}", response_model=CaseSchema)
async def get_case_detail(case_id: str, user: dict = Depends(get_current_user)):
    cases = await sophos_client.get_cases()
    for c in cases:
        if c["id"] == case_id:
            return CaseSchema(**c)
    raise HTTPException(status_code=404, detail=f"Case {case_id} not found")
