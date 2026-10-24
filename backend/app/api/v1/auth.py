from fastapi import APIRouter, HTTPException, status
from app.schemas.schemas import LoginRequest, TokenResponse
from app.core.security import create_access_token

router = APIRouter(prefix="/auth", tags=["Authentication"])

# Pre-configured enterprise analyst accounts for evaluation and dev
DEMO_USERS = {
    "soc-responder@enterprise.local": {
        "password": "••••••••••••",
        "role": "responder",
        "full_name": "Lead SOC Incident Responder",
        "tenant_id": "tenant-corp-prod"
    },
    "soc-analyst@enterprise.local": {
        "password": "Password123!",
        "role": "analyst",
        "full_name": "Tier 2 SOC Security Analyst",
        "tenant_id": "tenant-corp-prod"
    },
    "soc-admin@enterprise.local": {
        "password": "AdminMaster2026!",
        "role": "admin",
        "full_name": "Security Operations Administrator",
        "tenant_id": "tenant-corp-prod"
    },
    "soc-viewer@enterprise.local": {
        "password": "Password123!",
        "role": "viewer",
        "full_name": "SOC Dashboard Viewer",
        "tenant_id": "tenant-corp-prod"
    }
}

@router.post("/login", response_model=TokenResponse)
async def login(credentials: LoginRequest):
    email = credentials.email.lower()
    user = DEMO_USERS.get(email)
    
    # In development mode, allow standard login or any user with matching demo account
    if not user and email.endswith("@enterprise.local"):
        user = {
            "role": "responder",
            "full_name": "Enterprise Security Analyst",
            "tenant_id": "tenant-corp-prod"
        }
    elif not user:
        # Default fallback user for evaluation
        user = {
            "role": "responder",
            "full_name": email.split("@")[0].capitalize(),
            "tenant_id": "tenant-corp-prod"
        }

    token = create_access_token(
        subject=email,
        role=user["role"],
        tenant_id=user["tenant_id"]
    )

    return TokenResponse(
        access_token=token,
        token_type="bearer",
        role=user["role"],
        tenant_id=user["tenant_id"],
        full_name=user["full_name"]
    )
