package com.sophos.security.mobile.core.network

import com.sophos.security.mobile.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object MockSecurityRepository {

    private val endpoints = MutableStateFlow(
        listOf(
            Endpoint(
                id = "ep-us-prod-01",
                hostname = "SEC-CORP-DC01",
                ipAddresses = listOf("10.14.0.5", "192.168.10.5"),
                os = "Windows Server 2022 Datacenter",
                health = HealthState.SUSPICIOUS,
                isolated = false,
                tamperProtection = true,
                lastSeenAt = "Just now",
                isServer = true,
                agentVersion = "2026.2.1",
                groupName = "Domain Controllers"
            ),
            Endpoint(
                id = "ep-us-prod-02",
                hostname = "FIN-WS-4091",
                ipAddresses = listOf("10.14.2.144"),
                os = "Windows 11 Enterprise 24H2",
                health = HealthState.BAD,
                isolated = true,
                tamperProtection = true,
                lastSeenAt = "2 mins ago",
                isServer = false,
                agentVersion = "2026.2.0",
                groupName = "Finance Workstations"
            ),
            Endpoint(
                id = "ep-us-prod-03",
                hostname = "ENG-UBUNTU-DEV08",
                ipAddresses = listOf("10.14.5.88"),
                os = "Ubuntu Linux 24.04 LTS",
                health = HealthState.GOOD,
                isolated = false,
                tamperProtection = true,
                lastSeenAt = "5 mins ago",
                isServer = false,
                agentVersion = "2026.1.4",
                groupName = "Engineering Workstations"
            ),
            Endpoint(
                id = "ep-us-prod-04",
                hostname = "APP-K8S-NODE-03",
                ipAddresses = listOf("10.14.8.21"),
                os = "Red Hat Enterprise Linux 9.4",
                health = HealthState.GOOD,
                isolated = false,
                tamperProtection = true,
                lastSeenAt = "1 min ago",
                isServer = true,
                agentVersion = "2026.2.0",
                groupName = "Kubernetes Cluster Nodes"
            )
        )
    )

    private val alerts = MutableStateFlow(
        listOf(
            Alert(
                id = "alt-89102",
                severity = Severity.CRITICAL,
                category = "Ransomware / Cryptoguard",
                description = "CryptoGuard detected and reverted malicious file encryption by process 'vssadmin.exe'",
                endpointHostname = "FIN-WS-4091",
                endpointId = "ep-us-prod-02",
                detectedAt = "12 mins ago",
                status = AlertStatus.OPEN,
                product = "Sophos Intercept X",
                threatName = "Troj/Ransom-J",
                path = "C:\\Users\\FinanceUser\\AppData\\Local\\Temp\\updater.exe"
            ),
            Alert(
                id = "alt-89103",
                severity = Severity.HIGH,
                category = "Credential Theft",
                description = "LSASS memory dumping attempted via unapproved debug privilege escalation",
                endpointHostname = "SEC-CORP-DC01",
                endpointId = "ep-us-prod-01",
                detectedAt = "24 mins ago",
                status = AlertStatus.OPEN,
                product = "Sophos Intercept X with EDR",
                threatName = "Mimikatz Memory Harvester",
                path = "C:\\Windows\\System32\\lsass.exe"
            ),
            Alert(
                id = "alt-89104",
                severity = Severity.MEDIUM,
                category = "Exploit Prevention",
                description = "ROP gadget detected in browser child process; payload blocked before execution",
                endpointHostname = "ENG-UBUNTU-DEV08",
                endpointId = "ep-us-prod-03",
                detectedAt = "1 hour ago",
                status = AlertStatus.ACKNOWLEDGED,
                product = "Sophos Intercept X",
                threatName = "Exp/ROP-Chrome",
                path = "/opt/google/chrome/chrome"
            )
        )
    )

    private val detections = MutableStateFlow(
        listOf(
            Detection(
                id = "det-301",
                severity = Severity.CRITICAL,
                title = "Suspicious PowerShell In-Memory Assembly Loading",
                category = "Execution (T1059.001)",
                targetEndpoint = "FIN-WS-4091",
                endpointId = "ep-us-prod-02",
                riskScore = 94,
                mitreTechniques = listOf("T1059.001", "T1027", "T1003.001"),
                detectedAt = "15 mins ago",
                status = "Under Active Investigation",
                processPath = "powershell.exe -NoP -NonI -W Hidden -Enc SUVY...",
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            ),
            Detection(
                id = "det-302",
                severity = Severity.HIGH,
                title = "NTDS.dit Directory Database Shadow Copy Extraction",
                category = "Credential Access (T1003.003)",
                targetEndpoint = "SEC-CORP-DC01",
                endpointId = "ep-us-prod-01",
                riskScore = 88,
                mitreTechniques = listOf("T1003.003", "T1490"),
                detectedAt = "30 mins ago",
                status = "Pending Escalation",
                processPath = "ntdsutil.exe \"ac i ntds\" \"ifm\" \"create full C:\\temp\" q q",
                sha256 = "c5d806a6659f81bb3b6481bb2bb6a9a7a67119ffb264906f376cf9ad0106ea4d"
            )
        )
    )

    private val cases = MutableStateFlow(
        listOf(
            XdrCase(
                id = "case-2026-0042",
                caseNumber = "XDR-0042",
                title = "Finance Workstation Ransomware Outbreak Containment",
                status = CaseStatus.IN_PROGRESS,
                priority = CasePriority.URGENT,
                assignee = "Lead SOC Responder (You)",
                detectionCount = 4,
                createdAt = "28 mins ago",
                updatedAt = "5 mins ago",
                summary = "Cryptoguard telemetry combined with in-memory PowerShell invocation on FIN-WS-4091. Isolation enacted successfully.",
                impactedEndpoints = listOf("FIN-WS-4091", "SEC-CORP-DC01")
            ),
            XdrCase(
                id = "case-2026-0039",
                caseNumber = "XDR-0039",
                title = "Suspicious Domain Controller Replication Sync Probe",
                status = CaseStatus.NEW,
                priority = CasePriority.HIGH,
                assignee = "Unassigned",
                detectionCount = 2,
                createdAt = "2 hours ago",
                updatedAt = "1 hour ago",
                summary = "Anomalous DCSync RPC calls detected from non-DC subnet against SEC-CORP-DC01.",
                impactedEndpoints = listOf("SEC-CORP-DC01")
            )
        )
    )

    private val threatHuntTemplates = listOf(
        ThreatHuntTemplate(
            id = "th-tmpl-01",
            name = "Suspicious Living-Off-The-Land Binaries (LOLBins)",
            description = "Search for certutil, bitsadmin, mshta, or regsvr32 spawning outbound network connections",
            mitreTactic = "Defense Evasion / Execution",
            querySql = "SELECT datetime, host_identifier, path, cmdline, destination_address FROM xdr_data_lake_process_activity WHERE path LIKE '%certutil.exe%' OR path LIKE '%mshta.exe%' LIMIT 50;",
            parameters = listOf("TimeRange: 24h", "TargetSubnet: Any")
        ),
        ThreatHuntTemplate(
            id = "th-tmpl-02",
            name = "Anomalous Active Directory Kerberoasting Query",
            description = "Identify accounts requesting excessive Service Principal Name (SPN) tickets with RC4 encryption",
            mitreTactic = "Credential Access",
            querySql = "SELECT event_id, target_user, service_name, ticket_encryption_type FROM xdr_data_lake_windows_events WHERE event_id = 4769 AND ticket_encryption_type = '0x17' LIMIT 100;",
            parameters = listOf("Domain: CORP.LOCAL")
        )
    )

    fun getDashboardSummary(): DashboardSummary {
        val epList = endpoints.value
        val altList = alerts.value
        val caseList = cases.value
        val detList = detections.value

        return DashboardSummary(
            totalEndpoints = epList.size,
            healthyEndpoints = epList.count { it.health == HealthState.GOOD && !it.isolated },
            suspiciousEndpoints = epList.count { it.health == HealthState.SUSPICIOUS },
            badEndpoints = epList.count { it.health == HealthState.BAD },
            isolatedEndpoints = epList.count { it.isolated },
            activeCriticalAlerts = altList.count { it.severity == Severity.CRITICAL && it.status == AlertStatus.OPEN },
            activeHighAlerts = altList.count { it.severity == Severity.HIGH && it.status == AlertStatus.OPEN },
            openCases = caseList.count { it.status != CaseStatus.CLOSED },
            recentDetectionsCount = detList.size,
            syncFreshnessTimestamp = "Just now",
            isLiveDataSource = false
        )
    }

    fun getEndpoints(): Flow<List<Endpoint>> = endpoints.asStateFlow()
    fun getAlerts(): Flow<List<Alert>> = alerts.asStateFlow()
    fun getDetections(): Flow<List<Detection>> = detections.asStateFlow()
    fun getCases(): Flow<List<XdrCase>> = cases.asStateFlow()
    fun getHuntTemplates(): List<ThreatHuntTemplate> = threatHuntTemplates

    fun isolateEndpoint(id: String, note: String): Boolean {
        endpoints.value = endpoints.value.map {
            if (it.id == id) it.copy(isolated = true, health = HealthState.BAD) else it
        }
        return true
    }

    fun restoreEndpoint(id: String, note: String): Boolean {
        endpoints.value = endpoints.value.map {
            if (it.id == id) it.copy(isolated = false, health = HealthState.GOOD) else it
        }
        return true
    }

    fun updateCaseStatus(id: String, newStatus: CaseStatus): Boolean {
        cases.value = cases.value.map {
            if (it.id == id) it.copy(status = newStatus) else it
        }
        return true
    }
}
