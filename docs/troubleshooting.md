# Troubleshooting & Diagnostics

## Common Issues & Resolutions

### 1. Backend Shows "SOPHOS_MOCK_MODE=true"
- **Cause**: Sophos API credentials are not set in `.env` or `SOPHOS_MOCK_MODE` is explicitly set to `true`.
- **Resolution**: Set `SOPHOS_CLIENT_ID` and `SOPHOS_CLIENT_SECRET` in `.env`, and set `SOPHOS_MOCK_MODE=false`. Verify connectivity using `GET /api/v1/system/health`.

### 2. HTTP 403 Forbidden on Response Actions
- **Cause**: User role is `Viewer` or `Analyst`, which do not possess response privileges.
- **Resolution**: An administrator must assign the `Responder` or `Administrator` role to the user account.

### 3. Android App Cannot Connect to Backend
- **Android Emulator**: When testing via Android Emulator, use `http://10.0.2.2:8000` instead of `localhost`.
- **Physical Device**: Ensure the backend host IP is accessible on the local Wi-Fi or VPN, and configure the base URL in the app's settings screen.
- **Cleartext Traffic**: For production, always use HTTPS. In debug builds, cleartext traffic to internal IP ranges is permitted via `network_security_config.xml`.
