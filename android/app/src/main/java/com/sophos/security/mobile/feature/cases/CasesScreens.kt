package com.sophos.security.mobile.feature.cases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sophos.security.mobile.core.designsystem.components.*
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.core.model.*
import com.sophos.security.mobile.core.network.MockSecurityRepository

@Composable
fun CasesListScreen(
    onNavigateToCaseDetail: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val cases by MockSecurityRepository.getCases().collectAsState(initial = emptyList())

    val filteredCases = cases.filter { caseItem ->
        searchQuery.isEmpty() ||
                caseItem.title.contains(searchQuery, ignoreCase = true) ||
                caseItem.caseNumber.contains(searchQuery, ignoreCase = true) ||
                caseItem.assignee.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "XDR Cases",
                subtitle = "${filteredCases.size} security investigations"
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
                placeholder = "Search cases by ID, title, analyst..."
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredCases.isEmpty()) {
                EmptyStateView(
                    title = "No Cases Found",
                    message = "No investigation records match the filter query."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCases) { caseItem ->
                        CaseCard(caseItem = caseItem, onClick = { onNavigateToCaseDetail(caseItem.id) })
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun CaseDetailScreen(
    caseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToInvestigation: (String) -> Unit
) {
    val cases by MockSecurityRepository.getCases().collectAsState(initial = emptyList())
    val caseItem = cases.find { it.id == caseId } ?: cases.firstOrNull()
    var currentStatus by remember { mutableStateOf(caseItem?.status ?: CaseStatus.NEW) }

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = caseItem?.caseNumber ?: "Case Detail",
                subtitle = "Case ID: $caseId",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (caseItem == null) {
            ErrorStateView(message = "Case $caseId not found.", onRetry = onNavigateBack)
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
                                Text(
                                    text = caseItem.priority.name + " PRIORITY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (caseItem.priority == CasePriority.URGENT) SeverityCritical else SeverityMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Status: ${currentStatus.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecurityGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = caseItem.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = caseItem.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceMuted
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
                                text = "CASE METADATA & IMPACT",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkOnSurfaceMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Lead Assignee: ${caseItem.assignee}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Text("Created: ${caseItem.createdAt}", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                            Text("Last Activity: ${caseItem.updatedAt}", style = MaterialTheme.typography.bodySmall, color = DarkOnSurfaceMuted)
                            Text("Impacted Hosts: ${caseItem.impactedEndpoints.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = SecurityGreen)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val next = when (currentStatus) {
                                    CaseStatus.NEW -> CaseStatus.IN_PROGRESS
                                    CaseStatus.IN_PROGRESS -> CaseStatus.CONTAINED
                                    CaseStatus.CONTAINED -> CaseStatus.CLOSED
                                    CaseStatus.CLOSED -> CaseStatus.IN_PROGRESS
                                }
                                currentStatus = next
                                MockSecurityRepository.updateCaseStatus(caseItem.id, next)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
                        ) {
                            Text("ADVANCE: ${currentStatus.name}", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigateToInvestigation("job-xdr-${caseItem.caseNumber}") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Text("QUERY XDR", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
