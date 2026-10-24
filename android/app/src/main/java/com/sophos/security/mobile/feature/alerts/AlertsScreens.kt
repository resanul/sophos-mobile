package com.sophos.security.mobile.feature.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sophos.security.mobile.core.designsystem.components.*
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.core.model.*
import com.sophos.security.mobile.core.network.MockSecurityRepository

@Composable
fun AlertsListScreen(
    onNavigateToAlertDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf<Severity?>(null) }
    val allAlerts by MockSecurityRepository.getAlerts().collectAsState(initial = emptyList())
    val isSyncing by MockSecurityRepository.isSyncing.collectAsState(initial = false)

    val filteredAlerts = allAlerts.filter { alert ->
        val matchesQuery = searchQuery.isEmpty() ||
                alert.description.contains(searchQuery, ignoreCase = true) ||
                alert.endpointHostname.contains(searchQuery, ignoreCase = true) ||
                alert.threatName.contains(searchQuery, ignoreCase = true)
        val matchesSeverity = selectedSeverity == null || alert.severity == selectedSeverity
        matchesQuery && matchesSeverity
    }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Security Alerts",
                subtitle = if (isSyncing) "${filteredAlerts.size} events (fetching live...)" else "${filteredAlerts.size} events matching criteria",
                canNavigateBack = false
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    color = SecurityGreen,
                    trackColor = DarkSurfaceVariant
                )
            }
            SearchBarComponent(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Filter alerts by description, host, threat..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Severity Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSeverity == null,
                    onClick = { selectedSeverity = null },
                    label = { Text("ALL") }
                )
                FilterChip(
                    selected = selectedSeverity == Severity.CRITICAL,
                    onClick = { selectedSeverity = if (selectedSeverity == Severity.CRITICAL) null else Severity.CRITICAL },
                    label = { Text("CRITICAL") }
                )
                FilterChip(
                    selected = selectedSeverity == Severity.HIGH,
                    onClick = { selectedSeverity = if (selectedSeverity == Severity.HIGH) null else Severity.HIGH },
                    label = { Text("HIGH") }
                )
                FilterChip(
                    selected = selectedSeverity == Severity.MEDIUM,
                    onClick = { selectedSeverity = if (selectedSeverity == Severity.MEDIUM) null else Severity.MEDIUM },
                    label = { Text("MEDIUM") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredAlerts.isEmpty()) {
                EmptyStateView(
                    title = "No Alerts Found",
                    message = "No security events match the current filter criteria."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAlerts, key = { it.id }) { alert ->
                        AlertCard(alert = alert, onClick = { onNavigateToAlertDetail(alert.id) })
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun AlertDetailScreen(
    alertId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEndpoint: (String) -> Unit
) {
    val alerts by MockSecurityRepository.getAlerts().collectAsState(initial = emptyList())
    val alert = alerts.find { it.id == alertId } ?: alerts.firstOrNull()
    var isAcknowledged by remember { mutableStateOf(alert?.status == AlertStatus.ACKNOWLEDGED) }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Alert Inspection",
                subtitle = alertId,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (alert == null) {
            ErrorStateView(message = "Alert $alertId not found.", onRetry = onNavigateBack)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SeverityBadge(severity = alert.severity)
                                Text(
                                    text = alert.detectedAt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkOnSurfaceMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = alert.description,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Category: ${alert.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecurityGreen
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "TARGET DEVICE",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkOnSurfaceMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = alert.endpointHostname,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "ID: ${alert.endpointId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkOnSurfaceMuted
                                    )
                                }
                                Button(
                                    onClick = { onNavigateToEndpoint(alert.endpointId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Text("Open Device", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "TECHNICAL FORENSIC EVIDENCE",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkOnSurfaceMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Threat Name: ${alert.threatName.ifEmpty { "Generic Suspicious Heuristic" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Process / Path: ${alert.path.ifEmpty { "C:\\Windows\\System32\\svchost.exe" }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = DarkOnSurfaceMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Protection Product: ${alert.product}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceMuted
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isAcknowledged = !isAcknowledged },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isAcknowledged) "ACKNOWLEDGED" else "ACKNOWLEDGE")
                        }
                        Button(
                            onClick = { onNavigateToEndpoint(alert.endpointId) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SeverityCritical),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CONTAIN THREAT", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
