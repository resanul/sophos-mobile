# Deployment & Infrastructure Guide

## 1. Prerequisites

- **Operating System**: Enterprise Linux (RHEL 9, Rocky Linux 9, Ubuntu 22.04 LTS) or Windows Server
- **Python**: 3.12+
- **Database**: PostgreSQL 15+ (production) or SQLite (evaluations/testing)
- **Container Runtime**: Docker 24+ / Podman 4+
- **Reverse Proxy**: NGINX with TLS 1.3 termination

## 2. Containerized Deployment (Docker Compose)

The repository provides a production-ready `infrastructure/compose.yaml` configuration.

```bash
# Clone the repository
git clone <repo_url> /opt/sophos-mobile-gateway
cd /opt/sophos-mobile-gateway

# Configure production environment variables
cp .env.example .env
chmod 600 .env
nano .env

# Build and start services
docker compose -f infrastructure/compose.yaml up -d --build
```

## 3. Native Service Deployment (Systemd on RHEL)

```ini
# /etc/systemd/system/sophos-mobile-gateway.service
[Unit]
Description=Sophos Security Mobile Gateway
After=network.target postgresql.service

[Service]
Type=simple
User=sophosgateway
Group=sophosgateway
WorkingDirectory=/opt/sophos-mobile-gateway/backend
EnvironmentFile=/opt/sophos-mobile-gateway/.env
ExecStart=/opt/sophos-mobile-gateway/.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```
