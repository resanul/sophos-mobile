package com.sophos.security.mobile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sophos.security.mobile.core.designsystem.components.*
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.core.model.*
import com.sophos.security.mobile.core.network.MockSecurityRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAlerts: () -> Unit,
    onNavigateToEndpoints: () -> Unit,
    onNavigateToCases: () -> Unit,
    onNavigateToDetections: () -> Unit,
    onNavigateToHunt: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAlertDetail: (String) -> Unit
) {
    val summary by MockSecurityRepository.getDashboardSummaryFlow().collectAsState(initial = MockSecurityRepository.getDashboardSummary())
    val alerts by MockSecurityRepository.getAlerts().collectAsState(initial = emptyList())
    val endpoints by MockSecurityRepository.getEndpoints().collectAsState(initial = emptyList())
    val isSyncing by MockSecurityRepository.isSyncing.collectAsState(initial = false)

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Security Operations",
                subtitle = "Tenant: ${summary.tenantName} (${summary.dataRegion})",
                actions = {
                    IconButton(onClick = {
                        MockSecurityRepository.refresh()
                    }) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = SecurityGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = SecurityGreen
                            )
                        }
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (summary.isLiveDataSource) SecurityGreen else SeverityHigh)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (summary.isLiveDataSource) "Live Sophos Central Active" else "Central Stream Connecting",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (summary.isLiveDataSource) SecurityGreen else SeverityHigh
                        )
                    }
                    Text(
                        text = "Synced: ${summary.syncFreshnessTimestamp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceMuted
                    )
                }
            }

            // Top Primary Metric Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Critical Alerts",
                        value = summary.activeCriticalAlerts.toString(),
                        subtitle = "Active Threats",
                        icon = Icons.Default.Warning,
                        accentColor = SeverityCritical,
                        onClick = onNavigateToAlerts,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Open Cases",
                        value = summary.openCases.toString(),
                        subtitle = "Under Investigation",
                        icon = Icons.Default.Workspaces,
                        accentColor = SeverityHigh,
                        onClick = onNavigateToCases,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Endpoints",
                        value = summary.totalEndpoints.toString(),
                        subtitle = "${summary.activeEndpoints} Active Online • ${summary.badEndpoints} Bad",
                        icon = Icons.Default.Devices,
                        accentColor = SecurityGreen,
                        onClick = onNavigateToEndpoints,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Detections",
                        value = summary.recentDetectionsCount.toString(),
                        subtitle = "XDR Correlation",
                        icon = Icons.Default.Radar,
                        accentColor = Color(0xFF38BDF8),
                        onClick = onNavigateToDetections,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Operational Actions
            item {
                Text(
                    text = "RAPID RESPONSE ACTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = DarkOnSurfaceMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToHunt,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ManageSearch, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hunt XDR", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onNavigateToEndpoints,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SeverityCritical, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Isolation Hub", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Active & Online Devices Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                    onClick = onNavigateToEndpoints
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SecurityGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE & ONLINE DEVICES",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DarkOnSurfaceMuted
                                )
                            }
                            Text(
                                text = "${summary.activeEndpoints} / ${summary.totalEndpoints}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SecurityGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                        ) {
                            val activeRatio = if (summary.totalEndpoints > 0) summary.activeEndpoints.toFloat() / summary.totalEndpoints else 0f
                            val safeActive = activeRatio.coerceIn(0f, 1f)
                            val safeOffline = (1f - safeActive).coerceIn(0f, 1f)

                            if (safeActive > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(safeActive.coerceAtLeast(0.01f))
                                        .background(SecurityGreen)
                                )
                            }
                            if (safeOffline > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(safeOffline.coerceAtLeast(0.01f))
                                        .background(Color.Gray.copy(alpha = 0.35f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${summary.activeEndpoints} Connected & Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecurityGreen
                            )
                            val offlineCount = (summary.totalEndpoints - summary.activeEndpoints).coerceAtLeast(0)
                            Text(
                                text = "$offlineCount Inactive / Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkOnSurfaceMuted
                            )
                        }
                    }
                }
            }

            // Endpoint Health Breakdown Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ENDPOINT HEALTH DISTRIBUTION",
                            style = MaterialTheme.typography.labelMedium,
                            color = DarkOnSurfaceMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            val goodWeight = (summary.healthyEndpoints.toFloat() / summary.totalEndpoints.coerceAtLeast(1))
                            val suspWeight = (summary.suspiciousEndpoints.toFloat() / summary.totalEndpoints.coerceAtLeast(1))
                            val badWeight = (summary.badEndpoints.toFloat() / summary.totalEndpoints.coerceAtLeast(1))

                            if (goodWeight > 0) Box(modifier = Modifier.weight(goodWeight).fillMaxHeight().background(StatusHealthy))
                            if (suspWeight > 0) Box(modifier = Modifier.weight(suspWeight).fillMaxHeight().background(StatusSuspicious))
                            if (badWeight > 0) Box(modifier = Modifier.weight(badWeight).fillMaxHeight().background(StatusBad))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusHealthy))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Healthy (${summary.healthyEndpoints})", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusSuspicious))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Suspicious (${summary.suspiciousEndpoints})", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusBad))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bad (${summary.badEndpoints})", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
                            }
                        }
                    }
                }
            }

            // High Priority Security Incidents
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE CRITICAL ALERTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = DarkOnSurfaceMuted
                    )
                    TextButton(onClick = onNavigateToAlerts) {
                        Text("View All", color = SecurityGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            items(alerts.take(3)) { alert ->
                AlertCard(alert = alert, onClick = { onNavigateToAlertDetail(alert.id) })
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
