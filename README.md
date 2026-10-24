# Sophos Security Mobile — Enterprise Android SOC Application & FastAPI Gateway

[![CI/CD Pipeline](https://github.com/resanul/sophos-mobile/actions/workflows/ci.yml/badge.svg)](https://github.com/resanul/sophos-mobile/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Backend](https://img.shields.io/badge/Backend-FastAPI_0.115+-009688.svg)](https://fastapi.tiangolo.com)

**Sophos Security Mobile** is an enterprise-grade mobile security operations console designed for security analysts, incident responders, and SOC administrators who manage Sophos Central tenants.

The system supports **two flexible deployment modes**:
1. **Direct Cloud Standalone Mode**: The Android application communicates directly with Sophos Central Cloud APIs (`https://id.sophos.com` and `https://api-*.central.sophos.com`) via standard HTTPS, enabling full mobile SOC capabilities anywhere with zero local PC or server dependencies.
2. **Zero-Trust Enterprise Gateway**: An optional high-performance FastAPI backend gateway that acts as a centralized, authenticated, and audited intermediary with Role-Based Access Control (RBAC).

---

## Key Highlights

- **Zero Trust Mobile Gateway**: The Android client never holds or transmits Sophos Central API credentials or client secrets. All operations are authorized via server-side session tokens with Role-Based Access Control (RBAC).
- **Comprehensive Sophos Central Integration**:
  - OAuth 2.0 token management with automatic regional `/whoami` discovery (`api-us01`, `api-eu01`, etc.)
  - Endpoint & Server Inventory Management (`/endpoint/v1/endpoints`)
  - Real-time Security Alerts API (`/common/v1/alerts`)
  - Detections GraphQL (`/detections/graphql`)
  - Cases & Investigations GraphQL (`/cases/graphql`)
  - XDR Threat Hunting & Data Lake queries
- **Strict Response Safeguards**:
  - Network isolation & un-isolation of compromised hosts (`/endpoint/v1/endpoints/{id}/isolation`)
  - Dual confirmation & impact preview
  - Idempotency keys preventing accidental double-isolation
  - Step-up re-authentication
  - Emergency freeze master switch
  - Immutable audit logging for every command
- **Modern UI/UX Design System**:
  - Purpose-built dark mode console adhering to enterprise SOC human factors
  - Restrained security emerald accents (`#00D084`) and high-contrast severity badging (Critical `#EF4444`, High `#F97316`, Medium `#F59E0B`)
  - 26 complete screens covering authentication, dashboards, telemetry, forensics, workflows, and diagnostics

---

## Repository Structure

```
sophos-security-mobile/
  android/
    app/
      src/main/java/com/sophos/security/mobile/
        core/
          designsystem/ (Color, Theme, Typography, Reusable Components)
          model/        (Domain Entities: Alert, Endpoint, Case, etc.)
          navigation/   (Screen routes & Navigation Graph)
          network/      (Mock & live API repositories)
        feature/
          dashboard/    (Executive SOC posture & health distribution)
          alerts/       (Security alert feeds & forensic detail)
          endpoints/    (Device inventory & isolation controls)
          cases/        (XDR case management & progression)
          detections/   (GraphQL detection streams & ATT&CK mapping)
          hunting/      (Parameterized query templates & results)
          actions/      (Response action confirmation & execution)
          more/         (Audit history, catalog, settings, diagnostics)
    build.gradle.kts
    gradlew.bat

  backend/
    app/
      api/v1/       (FastAPI routers for auth, endpoints, alerts, actions, etc.)
      core/         (Config, Security, Database session engine)
      models/       (SQLAlchemy 2.x async models)
      schemas/      (Pydantic v2 schemas)
      services/     (Sophos Central Client & Mock Provider)
      main.py       (Application factory, middleware & headers)
    tests/          (Pytest automated test suite)
    pyproject.toml
    Dockerfile

  infrastructure/
    compose.yaml    (Production Docker deployment)

  docs/
    architecture.md
    api-capability-matrix.md
    api-setup.md
    security-model.md
    deployment.md
    testing.md
    user-guide.md
    troubleshooting.md
```

---

## Quick Start

### 1. Backend Gateway

```bash
cd backend
python -m pip install -e .
cp ../.env.example ../.env
python -m pytest tests/ -v
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Open `http://localhost:8000/docs` in your browser to inspect interactive OpenAPI documentation.

### 2. Android Mobile App

```bash
cd android
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The compiled APK will be generated at `android/app/build/outputs/apk/debug/app-debug.apk`.
