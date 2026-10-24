from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: str
    tenant_id: str
    full_name: str

class LoginRequest(BaseModel):
    email: str
    password: str

class EndpointSchema(BaseModel):
    id: str
    hostname: str
    ip_addresses: List[str] = []
    os: str
    health: str
    isolated: bool
    tamper_protection: bool
    last_seen_at: str
    is_server: bool
    agent_version: str
    group_name: str

class AlertSchema(BaseModel):
    id: str
    severity: str
    category: str
    description: str
    endpoint_hostname: str
    endpoint_id: str
    detected_at: str
    status: str
    product: str
    threat_name: str
    path: str

class DetectionSchema(BaseModel):
    id: str
    severity: str
    title: str
    category: str
    target_endpoint: str
    endpoint_id: str
    risk_score: int
    mitre_techniques: List[str] = []
    detected_at: str
    status: str
    process_path: str
    sha256: str

class CaseSchema(BaseModel):
    id: str
    case_number: str
    title: str
    status: str
    priority: str
    assignee: str
    detection_count: int
    summary: str
    impacted_endpoints: List[str] = []

class DashboardSummarySchema(BaseModel):
    total_endpoints: int
    healthy_endpoints: int
    suspicious_endpoints: int
    bad_endpoints: int
    isolated_endpoints: int
    active_critical_alerts: int
    active_high_alerts: int
    open_cases: int
    recent_detections_count: int
    sync_freshness_timestamp: str
    is_live_data_source: bool
    tenant_name: str
    data_region: str

class ResponseActionCreate(BaseModel):
    action_type: str = Field(..., description="ISOLATE_ENDPOINT, RESTORE_ENDPOINT, SCAN_ENDPOINT")
    target_id: str
    target_name: str
    idempotency_key: str
    note: str
    step_up_token: Optional[str] = None

class ResponseActionResponse(BaseModel):
    action_id: str
    status: str
    message: str
    target_id: str
    action_type: str

class InvestigationJobCreate(BaseModel):
    template_id: str
    parameters: Dict[str, str] = {}

class AuditEventSchema(BaseModel):
    id: str
    action: str
    performed_by: str
    role: str
    target: str
    status: str
    timestamp: str
    details: str

class HealthStatusSchema(BaseModel):
    status: str
    version: str
    environment: str
    sophos_mode: str
    data_region: str
    database_connected: bool
