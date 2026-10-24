# Application Security Model & Safeguards

## 1. Authentication & Role-Based Access Control (RBAC)

The application implements four discrete security roles:

| Role | Permissions |
|---|---|
| **Viewer** | Read-only access to dashboards, endpoint inventories, alerts, detections, and cases. |
| **Analyst** | All Viewer permissions, plus running parameterized threat-hunting queries and updating case investigation notes. |
| **Responder** | All Analyst permissions, plus executing approved containment actions (endpoint isolation/restoration, triggering scans, changing alert status). |
| **Administrator** | Full system control: user provisioning, role assignments, audit log review, tenant settings, and emergency freeze controls. |

## 2. Controlled Response Safeguards

Endpoint isolation and remediation are high-impact operations. The application enforces a multi-layer defense:

1. **Server-Side Role Validation**: Client claims are verified against the database session on every action.
2. **Idempotency Keys**: Every response action submission requires an `idempotency_key` (UUID v4) preventing duplicate submissions caused by network retries or double taps.
3. **Target Confirmation & Impact Analysis**: The client explicitly confirms device hostnames, IP addresses, and containment effects prior to dispatch.
4. **Step-Up Verification**: Destructive containment operations require the user to re-enter their credentials or biometric authentication token.
5. **Emergency Freeze (Kill Switch)**: Administrators can toggle `EMERGENCY_RESPONSE_ACTIONS_FROZEN=true`, instantly rejecting any containment actions across the enterprise.
6. **Immutable Audit Trail**: All response actions (attempted, succeeded, or failed) are permanently recorded with timestamp, operator ID, target ID, client IP, and justification note.
