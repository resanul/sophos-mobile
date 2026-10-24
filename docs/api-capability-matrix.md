# Sophos Central API Capability Matrix

This matrix documents official Sophos Central API endpoints, required permissions, licensing prerequisites, and application implementation status.

| Domain | Official Sophos API | Method / Protocol | Required Scope / License | Application Feature Status | Notes |
|---|---|---|---|---|---|
| **Identity & Discovery** | `https://id.sophos.com/api/v2/oauth2/token` | `POST` (OAuth 2) | Service Principal | ✅ Supported | Client Credentials grant returning JWT bearer |
| **Tenant WhoAmI** | `https://api.central.sophos.com/whoami/v1` | `GET` | Service Principal | ✅ Supported | Returns tenant ID, dataRegion, and API host URL |
| **Endpoints Inventory** | `/endpoint/v1/endpoints` | `GET` REST | `token` + Tenant Header | ✅ Supported | Full pagination, OS, health, tampering status |
| **Endpoint Details** | `/endpoint/v1/endpoints/{endpointId}` | `GET` REST | `token` + Tenant Header | ✅ Supported | Hardware details, IP/MAC, protection versions |
| **Endpoint Isolation** | `/endpoint/v1/endpoints/{endpointId}/isolation` | `POST / PATCH` | `admin` role + Intercept X | ✅ Supported | Requires Step-Up Auth + dual confirmation |
| **Endpoint Antivirus Scan**| `/endpoint/v1/endpoints/{endpointId}/scans` | `POST` REST | `admin` / `user` role | ✅ Supported | Triggers on-demand background full scan |
| **Security Alerts** | `/common/v1/alerts` | `GET` REST | Service Principal | ✅ Supported | Filterable by severity, product, time range |
| **Alert Actions** | `/common/v1/alerts/{alertId}/actions` | `POST` REST | Service Principal | ✅ Supported | Acknowledge, dismiss, or clear active alerts |
| **Detections GraphQL** | `/detections/graphql` | `POST` GraphQL | Intercept X Advanced with XDR | ✅ Supported | Official modern GraphQL API for detections |
| **Cases GraphQL** | `/cases/graphql` | `POST` GraphQL | Sophos XDR / MDR | ✅ Supported | Query cases, status, priority, detections |
| **Case Updates** | `/cases/graphql` | `POST` Mutation | Sophos XDR / MDR | ✅ Supported | Update status, severity, assignees, notes |
| **Live Discover / XDR** | `/xdr-query/v1/queries` | `POST` REST | Sophos XDR / Data Lake | ✅ Supported | Parameterized safe query templates |
| **Firewall Management** | `/firewall/v1` | REST | Sophos Firewall License | ⚠️ Modular Catalog | Indicated in capability catalogue |
| **Email Security** | `/email/v1` | REST | Sophos Central Email | ⚠️ Modular Catalog | Indicated in capability catalogue |
| **Mobile Management** | `/mobile/v1` | REST | Sophos Mobile License | ⚠️ Modular Catalog | Indicated in capability catalogue |

### API Version Compliance Note
- Detections utilize the current official **GraphQL** service rather than legacy deprecated REST classic detections endpoints.
- Regional endpoints are dynamically discovered via `/whoami/v1` (e.g. `api-us01.central.sophos.com`, `api-eu01.central.sophos.com`, `api-apj01.central.sophos.com`).
