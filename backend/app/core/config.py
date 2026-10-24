from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    APP_ENV: str = "development"
    DEBUG: bool = True
    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = 8000
    API_V1_PREFIX: str = "/api/v1"

    SECRET_KEY: str = "09d25e094faa6ca2556c818166b7a9563b93f7099f6f0f4caa6cf63b88e8d3e7"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    CORS_ORIGINS: str = "http://localhost:8000,http://127.0.0.1:8000,http://10.0.2.2:8000"

    DATABASE_URL: str = "sqlite+aiosqlite:///./sophos_mobile.db"

    # Sophos Central API Config
    SOPHOS_MOCK_MODE: bool = True
    SOPHOS_CLIENT_ID: str = ""
    SOPHOS_CLIENT_SECRET: str = ""
    SOPHOS_TENANT_ID: str = ""
    SOPHOS_DATA_REGION_URL: str = "https://api-us01.central.sophos.com"

    # Rate Limiting & Safety Controls
    RATE_LIMIT_PER_MINUTE: int = 120
    REQUIRE_STEP_UP_AUTH_FOR_ACTIONS: bool = True
    EMERGENCY_RESPONSE_ACTIONS_FROZEN: bool = False

    model_config = SettingsConfigDict(
        env_file=(".env", "../.env"),
        env_file_encoding="utf-8",
        extra="ignore"
    )

    @property
    def cors_origins_list(self) -> List[str]:
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]

settings = Settings()
