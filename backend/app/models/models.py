from datetime import datetime, timezone
import uuid
from sqlalchemy import Column, String, Integer, Boolean, DateTime, Text, JSON
from app.core.database import Base

def gen_uuid() -> str:
    return str(uuid.uuid4())

class User(Base):
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=gen_uuid)
    email = Column(String(255), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(255), nullable=False)
    role = Column(String(50), default="analyst", nullable=False) # viewer, analyst, responder, admin
    is_active = Column(Boolean, default=True)
    tenant_id = Column(String(100), default="tenant-corp-prod", nullable=False)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

class EndpointSnapshot(Base):
    __tablename__ = "endpoints"

    id = Column(String(36), primary_key=True, default=gen_uuid)
    endpoint_id = Column(String(100), unique=True, index=True, nullable=False)
    hostname = Column(String(255), index=True, nullable=False)
    ip_addresses = Column(JSON, default=list)
    os = Column(String(255), nullable=False)
    health_state = Column(String(50), default="GOOD") # GOOD, SUSPICIOUS, BAD
    isolated = Column(Boolean, default=False)
    tamper_protection = Column(Boolean, default=True)
    last_seen = Column(String(100))
    is_server = Column(Boolean, default=False)
    agent_version = Column(String(50), default="2026.2.0")
    group_name = Column(String(100), default="Default Group")
    tenant_id = Column(String(100), index=True, nullable=False)

class AlertRecord(Base):
    __tablename__ = "alerts"

    id = Column(String(36), primary_key=True, default=gen_uuid)
    alert_id = Column(String(100), unique=True, index=True, nullable=False)
    severity = Column(String(20), index=True, nullable=False) # CRITICAL, HIGH, MEDIUM, LOW, INFO
    category = Column(String(100), nullable=False)
    description = Column(Text, nullable=False)
    endpoint_hostname = Column(String(255), nullable=False)
    endpoint_id = Column(String(100), index=True, nullable=False)
    detected_at = Column(String(100), nullable=False)
    status = Column(String(50), default="OPEN") # OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED
    product = Column(String(100), default="Sophos Intercept X")
    threat_name = Column(String(255), default="")
    path = Column(Text, default="")
    tenant_id = Column(String(100), index=True, nullable=False)

class DetectionRecord(Base):
    __tablename__ = "detections"

    id = Column(String(36), primary_key=True, default=gen_uuid)
    detection_id = Column(String(100), unique=True, index=True, nullable=False)
    severity = Column(String(20), index=True, nullable=False)
    title = Column(String(255), nullable=False)
    category = Column(String(100), nullable=False)
    target_endpoint = Column(String(255), nullable=False)
    endpoint_id = Column(String(100), index=True, nullable=False)
    risk_score = Column(Integer, default=50)
    mitre_techniques = Column(JSON, default=list)
    detected_at = Column(String(100), nullable=False)
    status = Column(String(50), default="Active")
    process_path = Column(Text, default="")
    sha256 = Column(String(64), default="")
    tenant_id = Column(String(100), index=True, nullable=False)

class CaseRecord(Base):
    __tablename__ = "cases"

    id = Column(String(36), primary_key=True, default=gen_uuid)
    case_id = Column(String(100), unique=True, index=True, nullable=False)
    case_number = Column(String(50), nullable=False)
    title = Column(String(255), nullable=False)
    status = Column(String(50), default="NEW") # NEW, IN_PROGRESS, CONTAINED, CLOSED
    priority = Column(String(20), default="HIGH") # URGENT, HIGH, MEDIUM, LOW
    assignee = Column(String(255), default="Unassigned")
    detection_count = Column(Integer, default=1)
    summary = Column(Text, default="")
    impacted_endpoints = Column(JSON, default=list)
    tenant_id = Column(String(100), index=True, nullable=False)

class ResponseActionRecord(Base):
    __tablename__ = "response_actions"

    id = Column(String(36), primary_key=True, default=gen_uuid)
    action_id = Column(String(100), unique=True, index=True, nullable=False)
    idempotency_key = Column(String(100), unique=True, index=True, nullable=False)
    action_type = Column(String(100), nullable=False)
    target_id = Column(String(100), nullable=False)
    target_name = Column(String(255), nullable=False)
    initiated_by = Column(String(255), nullable=False)
    status = Column(String(50), default="PENDING") # PENDING, SUCCESS, FAILED
    note = Column(Text, default="")
    response_payload = Column(JSON, default=dict)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))
    tenant_id = Column(String(100), index=True, nullable=False)

class AuditEventRecord(Base):
    __tablename__ = "audit_events"

    id = Column(String(36), primary_key=True, default=gen_uuid)
    event_id = Column(String(100), unique=True, index=True, nullable=False)
    action = Column(String(100), nullable=False)
    performed_by = Column(String(255), nullable=False)
    role = Column(String(50), nullable=False)
    target = Column(String(255), nullable=False)
    status = Column(String(50), nullable=False)
    client_ip = Column(String(100), default="127.0.0.1")
    timestamp = Column(DateTime, default=lambda: datetime.now(timezone.utc))
    details = Column(Text, default="")
    tenant_id = Column(String(100), index=True, nullable=False)
