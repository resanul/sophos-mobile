# Sophos Central API Setup Guide

This document guides administrators on configuring API credentials in the Sophos Central Admin console for use with this gateway.

## Step 1: Create a Service Principal in Sophos Central

1. Log in to [Sophos Central Admin](https://central.sophos.com/) with a Super Admin account.
2. In the navigation sidebar, navigate to **Global Settings** > **API Credentials Management**.
3. Click **Add Credential** in the top-right corner.
4. Enter a descriptive Credential Name, for example: `Sophos-Security-Mobile-Gateway`.
5. Select the **Role**:
   - For read-only access (dashboard, alerts, inventory monitoring), choose **Service Principal Read-Only**.
   - For full SOC capabilities (including endpoint isolation, on-demand scans, and case management), choose **Service Principal Super Admin**.
6. Click **Save**.

## Step 2: Record Client ID and Client Secret

1. Sophos Central will display the **Client ID** and **Client Secret**.
2. **Important**: The Client Secret is only displayed once. Store it securely in your enterprise secret management vault (e.g. HashiCorp Vault, AWS Secrets Manager, or Azure Key Vault).
3. Do **not** distribute this secret to mobile devices or commit it into source code.

## Step 3: Configure Environment Variables

In your backend gateway server environment (or `.env` file):

```bash
SOPHOS_MOCK_MODE=false
SOPHOS_CLIENT_ID="<YOUR_CLIENT_ID>"
SOPHOS_CLIENT_SECRET="<YOUR_CLIENT_SECRET>"
```

The gateway automatically calls `https://id.sophos.com/api/v2/oauth2/token` to exchange these credentials for a JWT access token, and subsequently calls `https://api.central.sophos.com/whoami/v1` to discover the tenant ID and regional API base URL.
