from contextlib import asynccontextmanager
import time
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.core.config import settings
from app.api.v1.auth import router as auth_router
from app.api.v1.dashboard import router as dashboard_router
from app.api.v1.endpoints_api import router as endpoints_router
from app.api.v1.alerts_api import router as alerts_router
from app.api.v1.detections_api import router as detections_router
from app.api.v1.cases_api import router as cases_router
from app.api.v1.actions_api import router as actions_router
from app.api.v1.system_api import router as system_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup tasks
    yield
    # Shutdown tasks

app = FastAPI(
    title="Sophos Security Mobile Gateway",
    description="Enterprise Gateway connecting the Sophos Security Mobile Android Console to Sophos Central APIs.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Security Headers & Correlation ID Middleware
@app.middleware("http")
async def security_middleware(request: Request, call_next):
    correlation_id = request.headers.get("X-Correlation-ID", f"req-{int(time.time()*1000)}")
    response = await call_next(request)
    response.headers["X-Correlation-ID"] = correlation_id
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
    return response

# Register API v1 Routers
app.include_router(auth_router, prefix=settings.API_V1_PREFIX)
app.include_router(dashboard_router, prefix=settings.API_V1_PREFIX)
app.include_router(endpoints_router, prefix=settings.API_V1_PREFIX)
app.include_router(alerts_router, prefix=settings.API_V1_PREFIX)
app.include_router(detections_router, prefix=settings.API_V1_PREFIX)
app.include_router(cases_router, prefix=settings.API_V1_PREFIX)
app.include_router(actions_router, prefix=settings.API_V1_PREFIX)
app.include_router(system_router, prefix=settings.API_V1_PREFIX)

@app.get("/")
async def root():
    return {
        "service": "Sophos Security Mobile Gateway",
        "status": "ONLINE",
        "documentation": "/docs"
    }
