package com.sophos.security.mobile.feature.endpoints

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
fun EndpointsListScreen(
    onNavigateToEndpointDetail: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, SERVERS, WORKSTATIONS, ISOLATED
    val endpoints by MockSecurityRepository.getEndpoints().collectAsState(initial = emptyList())
    val isSyncing by MockSecurityRepository.isSyncing.collectAsState(initial = false)

    val filteredEndpoints = endpoints.filter { ep ->
        val matchesQuery = searchQuery.isEmpty() ||
                ep.hostname.contains(searchQuery, ignoreCase = true) ||
                ep.ipAddresses.any { it.contains(searchQuery) } ||
                ep.os.contains(searchQuery, ignoreCase = true)
        val matchesType = when (filterType) {
            "SERVERS" -> ep.isServer
            "WORKSTATIONS" -> !ep.isServer
            "ISOLATED" -> ep.isolated
            else -> true
        }
        matchesQuery && matchesType
    }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Endpoint Inventory",
                subtitle = if (isSyncing) "${filteredEndpoints.size} devices (fetching live...)" else "${filteredEndpoints.size} managed devices"
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
                placeholder = "Search by hostname, IP address, OS..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("ALL (${endpoints.size})") }
                )
                FilterChip(
                    selected = filterType == "SERVERS",
                    onClick = { filterType = "SERVERS" },
                    label = { Text("SERVERS") }
                )
                FilterChip(
                    selected = filterType == "WORKSTATIONS",
                    onClick = { filterType = "WORKSTATIONS" },
                    label = { Text("WORKSTATIONS") }
                )
                FilterChip(
                    selected = filterType == "ISOLATED",
                    onClick = { filterType = "ISOLATED" },
                    label = { Text("ISOLATED") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredEndpoints.isEmpty()) {
                EmptyStateView(
                    title = "No Endpoints Matching Filter",
                    message = "Adjust search terms or clear filters to view devices."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredEndpoints, key = { it.id }) { ep ->
                        EndpointCard(
                            endpoint = ep,
                            onClick = { onNavigateToEndpointDetail(ep.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun EndpointDetailScreen(
    endpointId: String,
    onNavigateBack: () -> Unit,
    onNavigateToActionConfirm: (String, String) -> Unit
) {
    val endpoints by MockSecurityRepository.getEndpoints().collectAsState(initial = emptyList())
    val endpoint = endpoints.find { it.id == endpointId } ?: endpoints.firstOrNull()

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = endpoint?.hostname ?: "Endpoint Details",
                subtitle = "ID: $endpointId",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (endpoint == null) {
            ErrorStateView(message = "Endpoint $endpointId not found.", onRetry = onNavigateBack)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Identity & Status Card
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
                                Text(
                                    text = if (endpoint.isServer) "SERVER HOST" else "WORKSTATION CLIENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecurityGreen
                                )
                                StatusIndicator(health = endpoint.health, isIsolated = endpoint.isolated)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = endpoint.hostname,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Group: ${endpoint.groupName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceMuted
                            )
                        }
                    }
                }

                // System & Protection Spec
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SYSTEM & PROTECTION SPECIFICATIONS",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkOnSurfaceMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(label = "Operating System", value = endpoint.os)
                            DetailRow(label = "IP Addresses", value = endpoint.ipAddresses.joinToString(", "))
                            DetailRow(label = "Agent Version", value = endpoint.agentVersion)
                            DetailRow(label = "Tamper Protection", value = if (endpoint.tamperProtection) "ENABLED" else "DISABLED")
                            DetailRow(label = "Heartbeat / Last Seen", value = endpoint.lastSeenAt)
                        }
                    }
                }

                // Controlled Action Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "CONTROLLED INCIDENT RESPONSE",
                            style = MaterialTheme.typography.labelMedium,
                            color = DarkOnSurfaceMuted
                        )

                        if (!endpoint.isolated) {
                            Button(
                                onClick = { onNavigateToActionConfirm(endpoint.id, "ISOLATE_ENDPOINT") },
                                colors = ButtonDefaults.buttonColors(containerColor = SeverityCritical),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ISOLATE ENDPOINT FROM NETWORK", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { onNavigateToActionConfirm(endpoint.id, "RESTORE_ENDPOINT") },
                                colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("RESTORE NETWORK CONNECTIVITY", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = { onNavigateToActionConfirm(endpoint.id, "SCAN_ENDPOINT") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TRIGGER ON-DEMAND MALWARE SCAN")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}
