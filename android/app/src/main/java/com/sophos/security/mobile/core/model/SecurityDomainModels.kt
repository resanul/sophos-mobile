package com.sophos.security.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Severity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

@Serializable
enum class HealthState {
    GOOD, SUSPICIOUS, BAD, UNKNOWN
}

@Serializable
enum class AlertStatus {
    OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED
}

@Serializable
enum class CaseStatus {
    NEW, IN_PROGRESS, CONTAINED, CLOSED
}

@Serializable
enum class CasePriority {
    URGENT, HIGH, MEDIUM, LOW
}

@Serializable
data class Alert(
    val id: String,
    val severity: Severity,
    val category: String,
    val description: String,
    val endpointHostname: String,
    val endpointId: String,
    val detectedAt: String,
    val status: AlertStatus = AlertStatus.OPEN,
    val product: String = "Sophos Intercept X",
    val threatName: String = "",
    val path: String = ""
)

@Serializable
data class Endpoint(
    val id: String,
    val hostname: String,
    val ipAddresses: List<String> = emptyList(),
    val os: String,
    val health: HealthState,
    val isolated: Boolean = false,
    val tamperProtection: Boolean = true,
    val lastSeenAt: String,
    val isServer: Boolean = false,
    val agentVersion: String = "2026.2.0",
    val groupName: String = "Enterprise Core",
    val online: Boolean = false
)

@Serializable
data class Detection(
    val id: String,
    val severity: Severity,
    val title: String,
    val category: String,
    val targetEndpoint: String,
    val endpointId: String,
    val riskScore: Int,
    val mitreTechniques: List<String> = emptyList(),
    val detectedAt: String,
    val status: String = "Active",
    val processPath: String = "",
    val sha256: String = "",
    val cmdline: String = ""
)

@Serializable
data class XdrCase(
    val id: String,
    val caseNumber: String,
    val title: String,
    val status: CaseStatus,
    val priority: CasePriority,
    val assignee: String,
    val detectionCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val summary: String,
    val impactedEndpoints: List<String> = emptyList()
)

@Serializable
data class ThreatHuntTemplate(
    val id: String,
    val name: String,
    val description: String,
    val mitreTactic: String,
    val querySql: String,
    val parameters: List<String> = emptyList()
)

@Serializable
data class InvestigationJob(
    val id: String,
    val templateName: String,
    val querySql: String,
    val status: String, // SUBMITTED, RUNNING, COMPLETED, FAILED
    val resultRowCount: Int,
    val executionTimeMs: Long,
    val createdAt: String,
    val results: List<Map<String, String>> = emptyList()
)

@Serializable
data class ResponseAction(
    val id: String,
    val actionType: String,
    val targetId: String,
    val targetName: String,
    val initiatedBy: String,
    val status: String, // PENDING, SUCCESS, FAILED
    val timestamp: String,
    val note: String,
    val details: String = ""
)

@Serializable
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val severity: Severity,
    val timestamp: String,
    val read: Boolean = false,
    val deepLinkRoute: String = ""
)

@Serializable
data class AuditRecord(
    val id: String,
    val action: String,
    val performedBy: String,
    val role: String,
    val target: String,
    val status: String,
    val timestamp: String,
    val details: String
)

@Serializable
data class CapabilityService(
    val id: String,
    val name: String,
    val category: String,
    val status: String, // ACTIVE, LICENSED, NOT_ENTITLED
    val description: String,
    val portalUrl: String
)

@Serializable
enum class WebAction {
    BLOCKED, WARNED, ALLOWED, AUDITED, PROCEEDED
}

@Serializable
data class WebActivityLog(
    val id: String,
    val username: String,
    val endpointHostname: String,
    val endpointId: String,
    val isServer: Boolean = false,
    val domain: String,
    val fullUrl: String = "",
    val category: String,
    val action: WebAction,
    val timestamp: String,
    val browser: String = "Google Chrome",
    val dataTransferred: String = "1.2 MB",
    val clientIp: String = "10.14.2.144",
    val policyRule: String = "Enterprise Acceptable Web Use Policy"
)

@Serializable
data class DashboardSummary(
    val totalEndpoints: Int,
    val activeEndpoints: Int = 0,
    val healthyEndpoints: Int,
    val suspiciousEndpoints: Int,
    val badEndpoints: Int,
    val isolatedEndpoints: Int,
    val activeCriticalAlerts: Int,
    val activeHighAlerts: Int,
    val openCases: Int,
    val recentDetectionsCount: Int,
    val syncFreshnessTimestamp: String,
    val isLiveDataSource: Boolean = false,
    val tenantName: String = "Enterprise Security Operations Tenant",
    val dataRegion: String = "us-east-1"
)

@Serializable
data class SophosCachedData(
    val endpoints: List<Endpoint> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val webLogs: List<WebActivityLog> = emptyList(),
    val detections: List<Detection> = emptyList(),
    val cases: List<XdrCase> = emptyList(),
    val summary: DashboardSummary? = null,
    val tenantId: String = "",
    val dataRegionUrl: String = "",
    val cachedAtTimestamp: Long = 0L
)
