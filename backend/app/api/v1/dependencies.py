from typing import Dict, Any, List, Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from app.core.security import decode_token
from app.core.config import settings

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)

async def get_current_user(token: Optional[str] = Depends(oauth2_scheme)) -> Dict[str, Any]:
    if token:
        payload = decode_token(token)
        if payload:
            return payload
    
    # In development mode, provide a default analyst session for mobile console connectivity
    if settings.APP_ENV == "development":
        return {
            "sub": "soc-responder@enterprise.local",
            "role": "admin",
            "tenant_id": "tenant-corp-prod"
        }

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or expired session token",
        headers={"WWW-Authenticate": "Bearer"},
    )

def require_role(allowed_roles: List[str]):
    def role_checker(user: Dict[str, Any] = Depends(get_current_user)) -> Dict[str, Any]:
        user_role = user.get("role", "viewer").lower()
        if user_role not in [r.lower() for r in allowed_roles] and user_role != "admin":
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Operation requires one of the following roles: {allowed_roles}. Current role: {user_role}",
            )
        return user
    return role_checker
