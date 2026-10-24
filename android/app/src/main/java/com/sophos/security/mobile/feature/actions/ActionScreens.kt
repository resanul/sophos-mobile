package com.sophos.security.mobile.feature.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.sophos.security.mobile.core.network.MockSecurityRepository
import java.util.UUID

@Composable
fun ActionConfirmScreen(
    targetId: String,
    actionType: String,
    onNavigateToResult: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var justificationNote by remember { mutableStateOf("Suspected malware containment per SOC playbooks") }
    var stepUpConfirmed by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }

    val isDestructive = actionType.contains("ISOLATE")

    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Confirm Response Action",
                subtitle = "Security Safeguards & Verification",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDestructive) SeverityCriticalContainer else DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(if (isDestructive) SeverityCritical else DarkBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDestructive) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDestructive) SeverityCritical else SecurityGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDestructive) "HIGH-RISK CONTAINMENT OPERATION" else "OPERATIONAL COMMAND",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Target Device ID: $targetId",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Action: $actionType",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecurityGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isDestructive)
                            "Warning: Network isolation immediately cuts off all outbound/inbound traffic on the target host except for essential communication with Sophos Central."
                        else
                            "This action sends an authenticated administrative directive to the endpoint agent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceMuted
                    )
                }
            }

            OutlinedTextField(
                value = justificationNote,
                onValueChange = { justificationNote = it },
                label = { Text("Operational Justification (Required for Audit)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = stepUpConfirmed,
                    onCheckedChange = { stepUpConfirmed = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I verify that I hold authorized SOC Responder role and accept accountability for this response action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isExecuting = true
                    val actionId = "act-" + UUID.randomUUID().toString().take(8)
                    if (actionType == "ISOLATE_ENDPOINT") {
                        MockSecurityRepository.isolateEndpoint(targetId, justificationNote)
                    } else if (actionType == "RESTORE_ENDPOINT") {
                        MockSecurityRepository.restoreEndpoint(targetId, justificationNote)
                    }
                    onNavigateToResult(actionId)
                },
                enabled = stepUpConfirmed && justificationNote.isNotEmpty() && !isExecuting,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDestructive) SeverityCritical else SecurityGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text(
                        text = "AUTHORIZE & EXECUTE ACTION",
                        fontWeight = FontWeight.Bold,
                        color = if (isDestructive) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ActionResultScreen(
    actionId: String,
    onNavigateToHome: () -> Unit
) {
    Scaffold(
        topBar = {
            SecurityTopBar(
                title = "Action Execution Status",
                subtitle = "Ref: $actionId"
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(SecurityGreenContainer, RoundedCornerShape(36.dp))
                    .border(1.dp, SecurityGreen, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Command Submitted & Acknowledged",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The directive was verified, signed, logged in the immutable audit trail, and dispatched to Sophos Central API.",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnSurfaceMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateToHome,
                colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RETURN TO DASHBOARD", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
