# System Architecture — Sophos Security Mobile

## 1. Architectural Overview

Sophos Security Mobile follows an enterprise gateway architecture designed for zero trust:
The mobile Android client interacts strictly with an authorized **FastAPI Backend Gateway** over mutual TLS / HTTPS. The mobile application **never** holds Sophos Central Client Secrets, API credentials, or direct network connections to vendor endpoints.

```
+-------------------------------------------------------------+
|                 Android Mobile Application                  |
|  - Jetpack Compose (Material 3 Dark/Light)                  |
|  - Clean MVVM + Kotlin Coroutines & StateFlow               |
|  - Retrofit / OkHttp + App Authentication Session           |
|  - Encrypted Keystore for App Sessions & Biometric Re-auth  |
+------------------------------+------------------------------+
                               |
                               | HTTPS / REST + JSON
                               | Bearer JWT Session Token
                               v
+-------------------------------------------------------------+
|              Sophos Mobile Gateway (FastAPI)                |
|  - OAuth 2.0 / JWT Auth & Granular RBAC (4 Roles)           |
|  - Tenant Scoping & Multi-Region URL Resolution             |
|  - Request Rate Limiting & Correlation ID Tracking          |
|  - Immutable Security Event & Action Auditing               |
|  - Response Action Authorization & Step-Up Verification     |
|  - Caching & Synchronization Layer                          |
+---------------+-----------------------------+---------------+
                |                             |
      SQLAlchemy Async (PostgreSQL / SQLite)  |
      +---------v---------+                   |
      |   Application DB  |                   |
      |  - User Accounts  |                   |
      |  - Tenant Config  |                   |
      |  - Audit Log      |                   |
      |  - Cached Telemetry                   |
      |  - Action Journal |                   |
      +-------------------+                   v
                          +-----------------------------------+
                          |     Sophos Central Cloud APIs     |
                          |  - OAuth2 Token / WhoAmI v1       |
                          |  - Endpoint Management v1         |
                          |  - Alerts API Common v1           |
                          |  - Detections GraphQL API         |
                          |  - Cases GraphQL API              |
                          |  - Live Discover & Data Lake XDR  |
                          +-----------------------------------+
```

## 2. Core Security Boundaries

1. **Client Isolation**: The Android client is treated as an untrusted edge node. All access decisions, tenant queries, and response actions are evaluated and authorized on the backend.
2. **Credential Bastion**: Sophos Central API Client IDs and Secrets are stored solely on the server in environment variables or hardware security modules (HSM) / cloud secret managers.
3. **Audit Trail**: Every request that initiates an investigation, alters case status, or triggers an endpoint action (such as device isolation) is written to an immutable append-only audit log before external execution.
4. **Resilience & Fallback**: When external connectivity to Sophos Central is interrupted or credentials are in configuration/onboarding mode, the gateway operates seamlessly with an enterprise mock provider clearly marked with data freshness headers (`X-Data-Source: Mock-Sandbox`).
