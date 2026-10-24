package com.sophos.security.mobile.feature.detections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sophos.security.mobile.core.designsystem.components.*
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.core.model.*
import com.sophos.security.mobile.core.network.MockSecurityRepository

@Composable
fun DetectionsListScreen(
    onNavigateToDetectionDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val detections by MockSecurityRepository.getDetections().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filtered = detections.filter {
        searchQuery.isEmpty() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.targetEndpoint.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Detections Stream",
                subtitle = "GraphQL Sensor Telemetry",
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
            SearchBarComponent(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search detections by technique, process, endpoint..."
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                EmptyStateView(
                    title = "No Detections Found",
                    message = "No active XDR threat detections recorded in this tenant."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { det ->
                        DetectionCard(detection = det, onClick = { onNavigateToDetectionDetail(det.id) })
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun DetectionDetailScreen(
    detectionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEndpoint: (String) -> Unit
) {
    val detections by MockSecurityRepository.getDetections().collectAsState(initial = emptyList())
    val detection = detections.find { it.id == detectionId } ?: detections.firstOrNull()

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Detection Analysis",
                subtitle = "ID: $detectionId",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (detection == null) {
            ErrorStateView(message = "Detection not found.", onRetry = onNavigateBack)
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SeverityBadge(severity = detection.severity)
                                Text("Risk Score: ${detection.riskScore}/100", style = MaterialTheme.typography.labelSmall, color = SeverityCritical)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(detection.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Category: ${detection.category}", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
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
                            Text("RAW EVIDENCE & ATT&CK TECHNIQUE", style = MaterialTheme.typography.labelMedium, color = DarkOnSurfaceMuted)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Target Host: ${detection.targetEndpoint}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("CommandLine:", style = MaterialTheme.typography.labelSmall, color = DarkOnSurfaceMuted)
                            Text(detection.processPath, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = SecurityGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("SHA256: ${detection.sha256}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = DarkOnSurfaceMuted)
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onNavigateToEndpoint(detection.endpointId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("INSPECT TARGET ENDPOINT", color = Color.White)
                    }
                }
            }
        }
    }
}
