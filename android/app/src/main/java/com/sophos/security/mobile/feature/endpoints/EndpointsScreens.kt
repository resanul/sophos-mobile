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
import androidx.compose.material.icons.automirrored.filled.Sort
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

enum class EndpointSortField(val label: String) {
    HOSTNAME("Hostname"),
    ONLINE("Active / Online"),
    HEALTH("Health State"),
    LAST_SEEN("Last Seen")
}

@Composable
fun EndpointsListScreen(
    onNavigateToEndpointDetail: (String) -> Unit
) {
    val defaultFilter by MockSecurityRepository.endpointFilter.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(defaultFilter) } // ALL, ONLINE, SERVERS, WORKSTATIONS, ISOLATED
    var onlineSubFilter by remember { mutableStateOf("ALL_ONLINE") } // ALL_ONLINE, ONLINE_SERVERS, ONLINE_WORKSTATIONS
    var sortField by remember { mutableStateOf(EndpointSortField.HOSTNAME) }
    var isAscending by remember { mutableStateOf(true) }
    var sortDropdownOpen by remember { mutableStateOf(false) }

    LaunchedEffect(defaultFilter) {
        filterType = defaultFilter
    }

    val endpoints by MockSecurityRepository.getEndpoints().collectAsState(initial = emptyList())
    val isSyncing by MockSecurityRepository.isSyncing.collectAsState(initial = false)

    val allCount = endpoints.size
    val onlineCount = remember(endpoints) { endpoints.count { it.online } }
    val serverCount = remember(endpoints) { endpoints.count { it.isServer } }
    val workstationCount = remember(endpoints) { endpoints.count { !it.isServer } }
    val isolatedCount = remember(endpoints) { endpoints.count { it.isolated } }

    val activeServerCount = remember(endpoints) { endpoints.count { it.online && it.isServer } }
    val activeWorkstationCount = remember(endpoints) { endpoints.count { it.online && !it.isServer } }

    val filteredEndpoints = endpoints.filter { ep ->
        val matchesQuery = searchQuery.isEmpty() ||
                ep.hostname.contains(searchQuery, ignoreCase = true) ||
                ep.ipAddresses.any { it.contains(searchQuery) } ||
                ep.os.contains(searchQuery, ignoreCase = true)
        val matchesType = when (filterType) {
            "ONLINE" -> {
                when (onlineSubFilter) {
                    "ONLINE_SERVERS" -> ep.online && ep.isServer
                    "ONLINE_WORKSTATIONS" -> ep.online && !ep.isServer
                    else -> ep.online
                }
            }
            "SERVERS" -> ep.isServer
            "WORKSTATIONS" -> !ep.isServer
            "ISOLATED" -> ep.isolated
            else -> true
        }
        matchesQuery && matchesType
    }

    val sortedEndpoints = remember(filteredEndpoints, sortField, isAscending) {
        val comparator: Comparator<Endpoint> = when (sortField) {
            EndpointSortField.HOSTNAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.hostname }
            EndpointSortField.ONLINE -> compareByDescending { it.online }
            EndpointSortField.HEALTH -> compareBy {
                when (it.health) {
                    HealthState.BAD -> 0
                    HealthState.SUSPICIOUS -> 1
                    HealthState.UNKNOWN -> 2
                    HealthState.GOOD -> 3
                }
            }
            EndpointSortField.LAST_SEEN -> compareByDescending { it.lastSeenAt }
        }
        if (isAscending) {
            filteredEndpoints.sortedWith(comparator)
        } else {
            filteredEndpoints.sortedWith(comparator.reversed())
        }
    }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Endpoint Inventory",
                subtitle = if (isSyncing) "${sortedEndpoints.size} devices (fetching live...)" else "${sortedEndpoints.size} managed devices"
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
                    onClick = {
                        filterType = "ALL"
                        MockSecurityRepository.setEndpointFilter("ALL")
                    },
                    label = { Text("ALL ($allCount)") }
                )
                FilterChip(
                    selected = filterType == "ONLINE",
                    onClick = {
                        filterType = "ONLINE"
                        MockSecurityRepository.setEndpointFilter("ONLINE")
                    },
                    label = { Text("ONLINE ($onlineCount)") }
                )
                FilterChip(
                    selected = filterType == "SERVERS",
                    onClick = {
                        filterType = "SERVERS"
                        MockSecurityRepository.setEndpointFilter("SERVERS")
                    },
                    label = { Text("SERVERS ($serverCount)") }
                )
                FilterChip(
                    selected = filterType == "WORKSTATIONS",
                    onClick = {
                        filterType = "WORKSTATIONS"
                        MockSecurityRepository.setEndpointFilter("WORKSTATIONS")
                    },
                    label = { Text("WORKSTATIONS ($workstationCount)") }
                )
                FilterChip(
                    selected = filterType == "ISOLATED",
                    onClick = {
                        filterType = "ISOLATED"
                        MockSecurityRepository.setEndpointFilter("ISOLATED")
                    },
                    label = { Text("ISOLATED ($isolatedCount)") }
                )
            }

            // Sub-filter for Active & Online Devices (All Active vs Active Servers vs Active Workstations)
            if (filterType == "ONLINE") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { onlineSubFilter = "ALL_ONLINE" },
                        label = { Text("All Active ($onlineCount)") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (onlineSubFilter == "ALL_ONLINE") SecurityGreenContainer else DarkSurfaceVariant,
                            labelColor = if (onlineSubFilter == "ALL_ONLINE") SecurityGreen else DarkOnSurfaceMuted
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (onlineSubFilter == "ALL_ONLINE") SecurityGreen else DarkBorder
                        )
                    )
                    AssistChip(
                        onClick = { onlineSubFilter = "ONLINE_SERVERS" },
                        label = { Text("Active Servers ($activeServerCount)") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (onlineSubFilter == "ONLINE_SERVERS") SecurityGreenContainer else DarkSurfaceVariant,
                            labelColor = if (onlineSubFilter == "ONLINE_SERVERS") SecurityGreen else DarkOnSurfaceMuted
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (onlineSubFilter == "ONLINE_SERVERS") SecurityGreen else DarkBorder
                        )
                    )
                    AssistChip(
                        onClick = { onlineSubFilter = "ONLINE_WORKSTATIONS" },
                        label = { Text("Active Workstations ($activeWorkstationCount)") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (onlineSubFilter == "ONLINE_WORKSTATIONS") SecurityGreenContainer else DarkSurfaceVariant,
                            labelColor = if (onlineSubFilter == "ONLINE_WORKSTATIONS") SecurityGreen else DarkOnSurfaceMuted
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (onlineSubFilter == "ONLINE_WORKSTATIONS") SecurityGreen else DarkBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort & Order Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${sortedEndpoints.size} endpoints listed",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkOnSurfaceMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        OutlinedButton(
                            onClick = { sortDropdownOpen = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort By",
                                tint = SecurityGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sortField.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = sortDropdownOpen,
                            onDismissRequest = { sortDropdownOpen = false }
                        ) {
                            EndpointSortField.values().forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.label) },
                                    leadingIcon = if (sortField == field) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = SecurityGreen) }
                                    } else null,
                                    onClick = {
                                        sortField = field
                                        sortDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { isAscending = !isAscending },
                        modifier = Modifier
                            .size(34.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = if (isAscending) "Ascending (Tap for Descending)" else "Descending (Tap for Ascending)",
                            tint = SecurityGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (sortedEndpoints.isEmpty()) {
                EmptyStateView(
                    title = "No Endpoints Matching Filter",
                    message = "Adjust search terms or clear filters to view devices."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedEndpoints, key = { it.id }) { ep ->
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
