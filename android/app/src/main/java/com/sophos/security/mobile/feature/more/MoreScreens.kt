package com.sophos.security.mobile.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sophos.security.mobile.core.designsystem.components.*
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.core.model.*
import com.sophos.security.mobile.core.network.MockSecurityRepository
import com.sophos.security.mobile.core.security.BiometricAuthManager
import com.sophos.security.mobile.core.security.findFragmentActivity

@Composable
fun MoreMenuScreen(
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSavedViews: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Operations & Admin",
                subtitle = "Console Settings & Subsystems"
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("OPERATIONAL TOOLS", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
            }
            item {
                MenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notification Inbox",
                    subtitle = "Alert notifications and critical updates",
                    onClick = onNavigateToNotifications
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.Search,
                    title = "Global Security Search",
                    subtitle = "Unified search across alerts, endpoints, detections",
                    onClick = onNavigateToSearch
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.Bookmarks,
                    title = "Saved Filters & Views",
                    subtitle = "Custom SOC query filters and pinboards",
                    onClick = onNavigateToSavedViews
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("GOVERNANCE & CATALOG", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
            }
            item {
                MenuItem(
                    icon = Icons.Default.History,
                    title = "Audit History",
                    subtitle = "Immutable record of all analyst response actions",
                    onClick = onNavigateToAudit
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.Apps,
                    title = "Licensed Service Catalogue",
                    subtitle = "Sophos Central entitlement and product modules",
                    onClick = onNavigateToCatalog
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("SYSTEM & DIAGNOSTICS", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
            }
            item {
                MenuItem(
                    icon = Icons.Default.Person,
                    title = "Analyst Profile & Credentials",
                    subtitle = "Active role: Lead SOC Responder",
                    onClick = onNavigateToProfile
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.Security,
                    title = "Security & Biometric Settings",
                    subtitle = "Fingerprint login, dark theme, containment re-auth",
                    onClick = onNavigateToSettings
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.Dns,
                    title = "API Connectivity Diagnostics",
                    subtitle = "Live check for Sophos Central & Gateway health",
                    onClick = onNavigateToDiagnostics
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.Build,
                    title = "Recovery & State Testing",
                    subtitle = "Inspect empty, error, and offline fallback views",
                    onClick = onNavigateToRecovery
                )
            }
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = SeverityCritical)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("END ANALYST SESSION (LOGOUT)", color = SeverityCritical)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DarkOnSurfaceMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun NotificationsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { SecurityTopBar("Notification Inbox", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            SeverityBadge(severity = Severity.CRITICAL)
                            Text("10m ago", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("High-Priority Alert on FIN-WS-4091", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text("CryptoGuard triggered isolation policy.", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchScreen(onNavigateBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Scaffold(
        topBar = { SecurityTopBar("Global Security Search", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SearchBarComponent(query = query, onQueryChange = { query = it }, placeholder = "Search across hostnames, IPs, hashes, alert IDs...")
            Spacer(modifier = Modifier.height(16.dp))
            if (query.isEmpty()) {
                EmptyStateView("Global Index Ready", "Type any search query to search all synchronized Sophos Central datasets.")
            } else {
                Text("Search Results for '$query':", style = MaterialTheme.typography.labelSmall, color = SecurityGreen)
            }
        }
    }
}

@Composable
fun SavedViewsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { SecurityTopBar("Saved Filter Views", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Production Domain Controllers", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text("Filter: isServer = true AND group = 'Domain Controllers'", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Uncontained Ransomware", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text("Filter: severity = CRITICAL AND category LIKE 'Ransom%'", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditHistoryScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { SecurityTopBar("Immutable Audit Trail", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("ISOLATE_ENDPOINT", style = MaterialTheme.typography.labelSmall, color = SeverityCritical)
                            Text("2026-10-24 06:14Z", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
                        }
                        Text("Target: FIN-WS-4091 (ep-us-prod-02)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("Operator: soc-responder@enterprise.local (Role: Responder)", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                        Text("Result: SUCCESS (IdempotencyKey: 81fb39a0)", style = MaterialTheme.typography.labelSmall, color = SecurityGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCatalogScreen(onNavigateBack: () -> Unit) {
    val modules = listOf(
        Triple("Sophos Intercept X with XDR", "ACTIVE / LICENSED", "Full endpoint detection, isolation, and Live Discover query engine."),
        Triple("Sophos Firewall Central Mgmt", "LICENSED", "Perimeter security, synchronous heartbeat, and automated network containment."),
        Triple("Sophos Central Email Security", "NOT ENTITLED", "Advanced cloud email quarantine and mailbox protection."),
        Triple("Sophos Cloud Optix", "NOT ENTITLED", "Multi-cloud posture management and IAM anomaly detection.")
    )

    Scaffold(
        topBar = { SecurityTopBar("Service Catalog & Modules", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(modules) { (name, status, desc) ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(status, style = MaterialTheme.typography.labelSmall, color = if (status.contains("ACTIVE")) SecurityGreen else DarkOnSurfaceMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { SecurityTopBar("Analyst Profile", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("soc-responder@enterprise.local", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Role: Senior SOC Responder", style = MaterialTheme.typography.bodySmall, color = SecurityGreen)
                    Text("Session ID: sess-9a081bc24", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = DarkOnSurfaceMuted)
                    Text("Token Expiry: 48 minutes remaining", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var darkMode by remember { mutableStateOf(true) }
    var biometricEnforced by remember { mutableStateOf(true) }
    var isBiometricEnrolled by remember { mutableStateOf(BiometricAuthManager.isBiometricEnrolled()) }
    var biometricStatusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { SecurityTopBar("Security & Preferences", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BIOMETRIC & FINGERPRINT SECURITY
            Text(
                text = "BIOMETRIC & FINGERPRINT SECURITY",
                style = MaterialTheme.typography.labelMedium,
                color = DarkOnSurfaceMuted
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (isBiometricEnrolled) SecurityGreen else DarkOnSurfaceMuted,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Fingerprint Quick Login",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isBiometricEnrolled) "Enrolled & Active for Fast Unlock" else "Log in with sensor instead of entering secret",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isBiometricEnrolled) SecurityGreen else DarkOnSurfaceMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isBiometricEnrolled) {
                        val creds = BiometricAuthManager.getEnrolledCredentials()
                        val maskedUser = creds?.first?.take(10)?.plus("...") ?: "Active User"
                        Text(
                            text = "Linked to User ID: $maskedUser",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkOnSurfaceMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                BiometricAuthManager.disableBiometric()
                                isBiometricEnrolled = false
                                biometricStatusMessage = "Fingerprint login disabled."
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SeverityCritical),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SeverityCritical.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DISABLE FINGERPRINT LOGIN", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val currentId = MockSecurityRepository.clientId
                                val currentSec = MockSecurityRepository.clientSecret
                                if (currentId.isNotBlank() && currentSec.isNotBlank()) {
                                    val activity = context.findFragmentActivity()
                                    if (activity != null) {
                                        BiometricAuthManager.authenticate(
                                            activity = activity,
                                            title = "Enroll Fingerprint Login",
                                            subtitle = "Touch sensor to register fingerprint for this account",
                                            onSuccess = {
                                                BiometricAuthManager.enrollCredentials(currentId, currentSec)
                                                isBiometricEnrolled = true
                                                biometricStatusMessage = "Fingerprint login successfully registered!"
                                            },
                                            onError = { err ->
                                                biometricStatusMessage = "Fingerprint enrollment: $err"
                                            }
                                        )
                                    } else {
                                        biometricStatusMessage = "Biometric prompt not available in current window."
                                    }
                                } else {
                                    biometricStatusMessage = "Please log in with User ID and Secret first to bind fingerprint."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ENROLL FINGERPRINT FOR LOGIN", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    if (biometricStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = biometricStatusMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBiometricEnrolled) SecurityGreen else SeverityHigh
                        )
                    }
                }
            }

            // SOC PREFERENCES
            Text(
                text = "SOC PREFERENCES",
                style = MaterialTheme.typography.labelMedium,
                color = DarkOnSurfaceMuted
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme (SOC Mode)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("High contrast dark interface", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                        }
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                    }
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Biometric Re-auth on Containment", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("Require fingerprint/face to isolate hosts", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                        }
                        Switch(checked = biometricEnforced, onCheckedChange = { biometricEnforced = it })
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(onNavigateBack: () -> Unit) {
    val summary by com.sophos.security.mobile.core.network.MockSecurityRepository.getDashboardSummaryFlow().collectAsState(initial = com.sophos.security.mobile.core.network.MockSecurityRepository.getDashboardSummary())
    val isLive = summary.isLiveDataSource
    val activeUrl = com.sophos.security.mobile.core.network.MockSecurityRepository.dataRegionUrl
    val syncStatus = com.sophos.security.mobile.core.network.MockSecurityRepository.lastSyncStatus
    val isSyncing by com.sophos.security.mobile.core.network.MockSecurityRepository.isSyncing.collectAsState(initial = false)

    Scaffold(
        topBar = { SecurityTopBar("Sophos Central Cloud Status", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isLive) "SOPHOS CENTRAL: DIRECT CONNECTED" else "SOPHOS CENTRAL: SYNCING",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isLive) SecurityGreen else SeverityHigh
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cloud Endpoint: $activeUrl", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Connection Status: $syncStatus", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                    Text("Tenant: ${summary.tenantName}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Live Endpoints: ${summary.totalEndpoints} managed devices", style = MaterialTheme.typography.bodySmall, color = SecurityGreen)
                    Text("Active Alerts: ${summary.activeHighAlerts + summary.activeCriticalAlerts} security events", style = MaterialTheme.typography.bodySmall, color = SeverityHigh)
                }
            }

            Button(
                onClick = { com.sophos.security.mobile.core.network.MockSecurityRepository.refresh() },
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing Sophos Cloud...", color = DarkBackground, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Force Refresh Sophos Central", color = DarkBackground, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecoveryStateScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { SecurityTopBar("Recovery State Preview", canNavigateBack = true, onNavigateBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ErrorStateView(message = "Simulation: Connection to Sophos Central Gateway timed out (504 Gateway Timeout). Automatic exponential backoff in progress.", onRetry = {})
        }
    }
}
