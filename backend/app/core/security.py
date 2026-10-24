from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional, Union
import jwt
import hashlib
from app.core.config import settings

def verify_password(plain_password: str, hashed_password: str) -> bool:
    # Deterministic SHA256 + salt fallback if bcrypt library has platform issues
    if hashed_password.startswith("pbkdf2_sha256$") or hashed_password.startswith("sha256$"):
        parts = hashed_password.split("$")
        salt = parts[1]
        expected_hash = parts[2]
        test_hash = hashlib.sha256((salt + plain_password).encode()).hexdigest()
        return test_hash == expected_hash
    # Simple hash for dev accounts
    return hashlib.sha256(plain_password.encode()).hexdigest() == hashed_password

def get_password_hash(password: str) -> str:
    salt = "sophos_secure_salt"
    h = hashlib.sha256((salt + password).encode()).hexdigest()
    return f"sha256${salt}${h}"

def create_access_token(
    subject: Union[str, Any],
    role: str,
    tenant_id: str,
    expires_delta: Optional[timedelta] = None
) -> str:
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode: Dict[str, Any] = {
        "sub": str(subject),
        "role": role,
        "tenant_id": tenant_id,
        "exp": expire,
        "iat": datetime.now(timezone.utc)
    }
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)

def decode_token(token: str) -> Optional[Dict[str, Any]]:
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        return payload
    except (jwt.PyJWTError, Exception):
        return None
