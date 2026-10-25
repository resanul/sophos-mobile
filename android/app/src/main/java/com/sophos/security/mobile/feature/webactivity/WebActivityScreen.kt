package com.sophos.security.mobile.feature.webactivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sophos.security.mobile.core.designsystem.components.*
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.core.model.WebAction
import com.sophos.security.mobile.core.model.WebActivityLog
import com.sophos.security.mobile.core.network.MockSecurityRepository

@Composable
fun WebActivityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEndpoint: (String) -> Unit
) {
    val webLogs by MockSecurityRepository.getWebLogs().collectAsState(initial = emptyList())
    val isSyncing by MockSecurityRepository.isSyncing.collectAsState(initial = false)
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, BLOCKED, WARNED, ALLOWED, SOCIAL, STREAMING, CLOUD, AI
    var selectedTimeRange by remember { mutableStateOf("7D") } // 7D, 24H, ALL
    var selectedLogForDetail by remember { mutableStateOf<WebActivityLog?>(null) }

    val timeFilteredLogs = remember(webLogs, selectedTimeRange) {
        when (selectedTimeRange) {
            "24H" -> webLogs.filter {
                it.timestamp.contains("mins ago", ignoreCase = true) ||
                it.timestamp.contains("hours ago", ignoreCase = true) ||
                it.timestamp.equals("Just now", ignoreCase = true) ||
                it.timestamp.equals("Recently", ignoreCase = true)
            }
            "7D" -> webLogs // In Sophos SIEM & Data Lake, 7D covers all ingested logs up to 7 days
            else -> webLogs
        }
    }

    val blockedCount = remember(timeFilteredLogs) { timeFilteredLogs.count { it.action == WebAction.BLOCKED } }
    val warnedCount = remember(timeFilteredLogs) { timeFilteredLogs.count { it.action == WebAction.WARNED } }
    val allowedCount = remember(timeFilteredLogs) { timeFilteredLogs.count { it.action == WebAction.ALLOWED || it.action == WebAction.AUDITED } }
    val uniqueUsersCount = remember(timeFilteredLogs) { timeFilteredLogs.map { it.username }.distinct().size }

    val filteredLogs = remember(timeFilteredLogs, searchQuery, selectedFilter) {
        timeFilteredLogs.filter { log ->
            val matchesQuery = searchQuery.isEmpty() ||
                    log.domain.contains(searchQuery, ignoreCase = true) ||
                    log.fullUrl.contains(searchQuery, ignoreCase = true) ||
                    log.username.contains(searchQuery, ignoreCase = true) ||
                    log.endpointHostname.contains(searchQuery, ignoreCase = true) ||
                    log.category.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "BLOCKED" -> log.action == WebAction.BLOCKED
                "WARNED" -> log.action == WebAction.WARNED
                "ALLOWED" -> log.action == WebAction.ALLOWED || log.action == WebAction.AUDITED
                "SOCIAL" -> log.category.contains("Social", ignoreCase = true)
                "STREAMING" -> log.category.contains("Streaming", ignoreCase = true)
                "CLOUD" -> log.category.contains("Cloud", ignoreCase = true) || log.category.contains("File", ignoreCase = true)
                "AI" -> log.category.contains("AI", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Web Browsing & Control Logs",
                subtitle = "${filteredLogs.size} events in $selectedTimeRange window",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
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
            Spacer(modifier = Modifier.height(8.dp))

            // Time Range & Live Discover Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedTimeRange == "7D",
                            onClick = { selectedTimeRange = "7D" },
                            label = { Text("Last 7 Days", fontSize = 11.sp, fontWeight = if (selectedTimeRange == "7D") FontWeight.Bold else FontWeight.Normal) }
                        )
                        FilterChip(
                            selected = selectedTimeRange == "24H",
                            onClick = { selectedTimeRange = "24H" },
                            label = { Text("24 Hours", fontSize = 11.sp, fontWeight = if (selectedTimeRange == "24H") FontWeight.Bold else FontWeight.Normal) }
                        )
                        FilterChip(
                            selected = selectedTimeRange == "ALL",
                            onClick = { selectedTimeRange = "ALL" },
                            label = { Text("All", fontSize = 11.sp, fontWeight = if (selectedTimeRange == "ALL") FontWeight.Bold else FontWeight.Normal) }
                        )
                    }

                    FilledTonalButton(
                        onClick = { MockSecurityRepository.refreshWebLogs7Days() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = SecurityGreen.copy(alpha = 0.15f), contentColor = SecurityGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SecurityGreen, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSyncing) "SYNCING..." else "SCAN 7D", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            SearchBarComponent(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search by user, domain, URL, workstation..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WebMiniMetricCard(
                    title = "Total Events",
                    value = timeFilteredLogs.size.toString(),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                WebMiniMetricCard(
                    title = "Blocked",
                    value = blockedCount.toString(),
                    color = SeverityCritical,
                    modifier = Modifier.weight(1f)
                )
                WebMiniMetricCard(
                    title = "Warned / Audit",
                    value = warnedCount.toString(),
                    color = SeverityHigh,
                    modifier = Modifier.weight(1f)
                )
                WebMiniMetricCard(
                    title = "Active Users",
                    value = uniqueUsersCount.toString(),
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("ALL (${timeFilteredLogs.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "BLOCKED",
                    onClick = { selectedFilter = "BLOCKED" },
                    label = { Text("BLOCKED ($blockedCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "WARNED",
                    onClick = { selectedFilter = "WARNED" },
                    label = { Text("WARNED ($warnedCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "ALLOWED",
                    onClick = { selectedFilter = "ALLOWED" },
                    label = { Text("ALLOWED ($allowedCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "SOCIAL",
                    onClick = { selectedFilter = "SOCIAL" },
                    label = { Text("SOCIAL") }
                )
                FilterChip(
                    selected = selectedFilter == "STREAMING",
                    onClick = { selectedFilter = "STREAMING" },
                    label = { Text("STREAMING") }
                )
                FilterChip(
                    selected = selectedFilter == "CLOUD",
                    onClick = { selectedFilter = "CLOUD" },
                    label = { Text("CLOUD STORAGE") }
                )
                FilterChip(
                    selected = selectedFilter == "AI",
                    onClick = { selectedFilter = "AI" },
                    label = { Text("AI TOOLS") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLogs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SecurityGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Web Activity Logged (Last 7 Days)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sophos Central only forwards web activity to the cloud when an endpoint hits a Blocked or Warned category (e.g. Social Media, Gambling, Phishing) or through scheduled Data Lake (XDR) queries. Normal allowed browsing is not uploaded by default for privacy compliance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { MockSecurityRepository.refreshWebLogs7Days() },
                            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN 7-DAY DATA LAKE SCAN", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        WebActivityLogCard(
                            log = log,
                            onClick = { selectedLogForDetail = log },
                            onInspectEndpoint = { onNavigateToEndpoint(log.endpointId) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Detail Dialog
    selectedLogForDetail?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = SecurityGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Web Browsing Detail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailField(label = "User Account", value = log.username)
                    DetailField(label = "Endpoint / Machine", value = "${log.endpointHostname} (${if (log.isServer) "Server" else "Workstation"})")
                    DetailField(label = "Client IP", value = log.clientIp)
                    DetailField(label = "Visited Domain", value = log.domain)
                    if (log.fullUrl.isNotEmpty()) {
                        DetailField(label = "Target URL", value = log.fullUrl)
                    }
                    DetailField(label = "Web Category", value = log.category)
                    DetailField(label = "Action Enforced", value = log.action.name)
                    DetailField(label = "Browser / User-Agent", value = log.browser)
                    DetailField(label = "Data Transferred", value = log.dataTransferred)
                    DetailField(label = "Security Policy", value = log.policyRule)
                    DetailField(label = "Logged At", value = log.timestamp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val epId = log.endpointId
                        selectedLogForDetail = null
                        onNavigateToEndpoint(epId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
                ) {
                    Text("INSPECT ENDPOINT", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLogForDetail = null }) {
                    Text("CLOSE", color = DarkOnSurfaceMuted)
                }
            }
        )
    }
}

@Composable
private fun WebMiniMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted, maxLines = 1, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WebActivityLogCard(
    log: WebActivityLog,
    onClick: () -> Unit,
    onInspectEndpoint: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: User & Endpoint & Action Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SecurityGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (log.isServer) Color(0xFF1E293B) else Color(0xFF0F2942),
                        border = BorderStroke(0.5.dp, if (log.isServer) Color(0xFF64748B) else Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = log.endpointHostname,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (log.isServer) Color(0xFF94A3B8) else Color(0xFF7DD3FC),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action Badge
                WebActionBadge(action = log.action)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visited Website Domain & Full URL
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = if (log.action == WebAction.BLOCKED) SeverityCritical else SecurityGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category & Policy Rule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = log.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = log.dataTransferred,
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkOnSurfaceMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Browser, IP, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = DarkOnSurfaceMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${log.browser} • ${log.clientIp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceMuted,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkOnSurfaceMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun WebActionBadge(action: WebAction) {
    val (bgColor, textColor, label) = when (action) {
        WebAction.BLOCKED -> Triple(SeverityCritical.copy(alpha = 0.2f), SeverityCritical, "BLOCKED")
        WebAction.WARNED -> Triple(SeverityHigh.copy(alpha = 0.2f), SeverityHigh, "WARNED")
        WebAction.ALLOWED -> Triple(SecurityGreen.copy(alpha = 0.2f), SecurityGreen, "ALLOWED")
        WebAction.AUDITED -> Triple(Color(0xFF38BDF8).copy(alpha = 0.2f), Color(0xFF38BDF8), "AUDITED")
        WebAction.PROCEEDED -> Triple(SeverityMedium.copy(alpha = 0.2f), SeverityMedium, "PROCEEDED")
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
