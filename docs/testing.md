# Testing Guide

## 1. Backend Automated Testing

The backend includes a comprehensive pytest suite covering:
- Authentication & JWT token issuance/expiry
- Role-based authorization matrix (Viewer vs Analyst vs Responder vs Admin)
- Sophos OAuth2 token acquisition & regional `/whoami` discovery
- Endpoint inventory filtering, search, and pagination
- Detections and Cases queries
- Idempotency & step-up verification for response actions
- Rate limiting and audit trail creation

### Running Backend Tests
```bash
cd backend
python -m pytest tests/ -v --cov=app --cov-report=term-missing
```

## 2. Android Unit & Instrumentation Testing

The Android application includes:
- **ViewModel Unit Tests**: Coroutines, Flow state emission, search debouncing, and filter state transitions.
- **Repository Contract Tests**: Retrofit mock web server tests checking parsing of backend responses.
- **Compose UI Tests**: Verification of loading skeletons, error screens, empty state guidance, and confirmation dialogs.

### Running Android Tests
```bash
cd android
./gradlew testDebugUnitTest
```
