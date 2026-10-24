# Mobile Application User Guide

## 1. Overview & Navigation

Sophos Security Mobile delivers an operational console built specifically for SOC analysts and incident responders on the move:
- **Bottom Navigation**:
  - **Dashboard**: High-level posture, active critical alerts, open cases, health distributions, and quick actions.
  - **Alerts**: Real-time triage feed with severity indicators, status filters, and instant action options.
  - **Endpoints**: Inventory of all workstations and servers, OS breakdown, tamper protection status, and isolation controls.
  - **Cases**: XDR cases, priority assignments, linked detections, and investigation notes.
  - **More**: Threat-hunting queries, audit logs, licensed capability catalogue, diagnostics, and app settings.

## 2. Performing Response Actions Safely

To isolate a compromised endpoint:
1. Navigate to **Endpoints** and select the target device.
2. Tap the **Isolate Endpoint** button.
3. Review the containment impact warning dialog (device IP, hostname, OS).
4. Enter your operational justification comment.
5. Provide step-up biometric or PIN verification.
6. The gateway records the audit log, submits the command to Sophos Central, and updates the device status to *Isolated*.
