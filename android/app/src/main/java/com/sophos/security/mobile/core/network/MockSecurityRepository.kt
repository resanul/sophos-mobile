package com.sophos.security.mobile.core.network

import android.content.Context
import android.util.Log
import com.sophos.security.mobile.core.model.*
import com.sophos.security.mobile.core.security.EncryptedStorageManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MockSecurityRepository {

    private const val TAG = "SophosDirectAPI"

    // Dynamic Sophos Central Service Principal Credentials (session in-memory only)
    var clientId: String = ""
    var clientSecret: String = ""
    var tenantId: String = ""
    var dataRegionUrl: String = ""

    private var cachedToken: String? = null
    private var tokenExpiryMs: Long = 0L

    var isLoggedIn: Boolean = false
        private set

    var lastSyncStatus: String = "Awaiting credentials..."
        private set
    var isLiveConnected: Boolean = false
        private set

    private val isSyncingFlow = MutableStateFlow(false)
    val isSyncing: Flow<Boolean> = isSyncingFlow.asStateFlow()

    val endpointFilter = MutableStateFlow("ALL")
    fun setEndpointFilter(filter: String) {
        endpointFilter.value = filter
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Initial endpoints state
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
                groupName = "Domain Controllers",
                online = true
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
                groupName = "Finance Workstations",
                online = false
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
                groupName = "Engineering Workstations",
                online = true
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
                groupName = "Kubernetes Cluster Nodes",
                online = true
            )
        )
    )

    // Initial alerts state
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

    private val dashboardSummary = MutableStateFlow(
        DashboardSummary(
            totalEndpoints = 4,
            activeEndpoints = 3,
            healthyEndpoints = 2,
            suspiciousEndpoints = 1,
            badEndpoints = 1,
            isolatedEndpoints = 1,
            activeCriticalAlerts = 1,
            activeHighAlerts = 1,
            openCases = 2,
            recentDetectionsCount = 2,
            syncFreshnessTimestamp = "Awaiting login...",
            isLiveDataSource = false,
            tenantName = "Enterprise Security Operations Tenant",
            dataRegion = "Sophos Central Cloud"
        )
    )

    init {
        if (clientId.isNotBlank() && clientSecret.isNotBlank()) {
            refresh()
        }
    }

    fun getDashboardSummary(): DashboardSummary = dashboardSummary.value
    fun getDashboardSummaryFlow(): Flow<DashboardSummary> = dashboardSummary.asStateFlow()

    fun getEndpoints(): Flow<List<Endpoint>> = endpoints.asStateFlow()
    fun getAlerts(): Flow<List<Alert>> = alerts.asStateFlow()
    fun getDetections(): Flow<List<Detection>> = detections.asStateFlow()
    fun getCases(): Flow<List<XdrCase>> = cases.asStateFlow()
    fun getHuntTemplates(): List<ThreatHuntTemplate> = threatHuntTemplates

    fun initStorage(context: Context) {
        EncryptedStorageManager.init(context)
        loadEncryptedCache()
    }

    fun loadEncryptedCache() {
        try {
            val decryptedJson = EncryptedStorageManager.readAndDecrypt() ?: return
            val cached = json.decodeFromString<SophosCachedData>(decryptedJson)
            if (cached.endpoints.isNotEmpty()) {
                endpoints.value = cached.endpoints
            }
            if (cached.alerts.isNotEmpty()) {
                alerts.value = cached.alerts
            }
            if (cached.summary != null) {
                val activeCount = if (cached.summary.activeEndpoints > 0) cached.summary.activeEndpoints else cached.endpoints.count { it.online }
                dashboardSummary.value = cached.summary.copy(
                    activeEndpoints = activeCount,
                    syncFreshnessTimestamp = "Decrypted Local Cache",
                    isLiveDataSource = true
                )
            }
            if (cached.tenantId.isNotEmpty()) tenantId = cached.tenantId
            if (cached.dataRegionUrl.isNotEmpty()) dataRegionUrl = cached.dataRegionUrl
            isLiveConnected = cached.endpoints.isNotEmpty()
            lastSyncStatus = "Decrypted Local Cache (${endpoints.value.size} Endpoints, ${alerts.value.size} Alerts)"
            Log.i(TAG, "Decrypted local cache loaded: ${endpoints.value.size} endpoints, ${alerts.value.size} alerts")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load decrypted local cache: ${e.message}")
        }
    }

    fun saveEncryptedCache() {
        try {
            val cached = SophosCachedData(
                endpoints = endpoints.value,
                alerts = alerts.value,
                summary = dashboardSummary.value,
                tenantId = tenantId,
                dataRegionUrl = dataRegionUrl,
                cachedAtTimestamp = System.currentTimeMillis()
            )
            val jsonString = json.encodeToString(SophosCachedData.serializer(), cached)
            EncryptedStorageManager.encryptAndSave(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save encrypted cache: ${e.message}", e)
        }
    }

    suspend fun login(userId: String, secret: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedUser = userId.trim()
        val trimmedSecret = secret.trim()

        if (trimmedUser.isBlank() || trimmedSecret.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter both User ID and Secret"))
        }

        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", trimmedUser)
                .add("client_secret", trimmedSecret)
                .add("scope", "token")
                .build()

            val req = Request.Builder()
                .url("https://id.sophos.com/api/v2/oauth2/token")
                .post(formBody)
                .build()

            val token: String
            val expiresIn: Int

            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string() ?: ""
                    Log.e(TAG, "OAuth failed HTTP ${resp.code}: $errBody")
                    val msg = if (resp.code == 401 || resp.code == 400) {
                        "Invalid User ID or Secret. Please verify your credentials."
                    } else {
                        "Authentication error (HTTP ${resp.code}). Please check your connection."
                    }
                    return@withContext Result.failure(Exception(msg))
                }

                val bodyStr = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty authentication response"))
                val root = json.parseToJsonElement(bodyStr).jsonObject
                token = root["access_token"]?.jsonPrimitive?.contentOrNull
                    ?: return@withContext Result.failure(Exception("Failed to obtain access token from Sophos Central"))
                expiresIn = root["expires_in"]?.jsonPrimitive?.intOrNull ?: 3600
            }

            // Save in-memory credentials and session state
            clientId = trimmedUser
            clientSecret = trimmedSecret
            cachedToken = token
            tokenExpiryMs = System.currentTimeMillis() + ((expiresIn - 60).coerceAtLeast(60) * 1000L)
            isLoggedIn = true

            // Auto-discover tenant ID and data region URL via /whoami
            try {
                val whoamiReq = Request.Builder()
                    .url("https://api.central.sophos.com/whoami/v1")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
                okHttpClient.newCall(whoamiReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: "{}"
                        val whoamiObj = json.parseToJsonElement(body).jsonObject
                        val discoveredTenant = whoamiObj["id"]?.jsonPrimitive?.contentOrNull
                        val discoveredRegion = whoamiObj["apiHosts"]?.jsonObject?.get("dataRegion")?.jsonPrimitive?.contentOrNull
                        if (!discoveredTenant.isNullOrEmpty()) tenantId = discoveredTenant
                        if (!discoveredRegion.isNullOrEmpty()) dataRegionUrl = discoveredRegion
                        Log.i(TAG, "Discovered tenant: $tenantId, region: $dataRegionUrl")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Whoami discovery error: ${e.message}")
            }

            // Ensure local encrypted cache is in place so previous data displays instantly
            loadEncryptedCache()

            // Trigger full background sync for all 2,688 endpoints and alerts
            repositoryScope.launch {
                syncDirectFromSophosCentral()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Network connection error during login: ${e.message}", e)
            Result.failure(Exception("Network connection failed: ${e.localizedMessage ?: "Unable to connect to id.sophos.com"}"))
        }
    }

    fun logout() {
        clientId = ""
        clientSecret = ""
        tenantId = ""
        dataRegionUrl = ""
        cachedToken = null
        tokenExpiryMs = 0L
        isLoggedIn = false
        isLiveConnected = false
        lastSyncStatus = "Logged out"
    }

    fun refresh() {
        if (clientId.isBlank() || clientSecret.isBlank()) return
        repositoryScope.launch {
            syncDirectFromSophosCentral()
        }
    }

    private suspend fun getValidOAuthToken(): String? = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return@withContext null
        }

        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryMs) {
            return@withContext cachedToken
        }

        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("scope", "token")
                .build()

            val req = Request.Builder()
                .url("https://id.sophos.com/api/v2/oauth2/token")
                .post(formBody)
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string() ?: ""
                    Log.e(TAG, "OAuth failed HTTP ${resp.code}: $err")
                    lastSyncStatus = "OAuth auth failed (HTTP ${resp.code})"
                    return@withContext null
                }

                val bodyStr = resp.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val token = root["access_token"]?.jsonPrimitive?.contentOrNull
                val expiresIn = root["expires_in"]?.jsonPrimitive?.intOrNull ?: 3600

                if (token != null) {
                    cachedToken = token
                    tokenExpiryMs = System.currentTimeMillis() + ((expiresIn - 60).coerceAtLeast(60) * 1000L)
                    return@withContext token
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching OAuth token: ${e.message}", e)
            lastSyncStatus = "Network error connecting to id.sophos.com: ${e.message}"
        }
        return@withContext null
    }

    private suspend fun syncDirectFromSophosCentral() = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || clientSecret.isBlank()) return@withContext
        if (isSyncingFlow.value) return@withContext
        isSyncingFlow.value = true
        lastSyncStatus = "Authenticating with Sophos Central..."

        val token = getValidOAuthToken()
        if (token == null) {
            isSyncingFlow.value = false
            return@withContext
        }

        // Auto-discover tenant and region if needed
        if (tenantId.isEmpty() || dataRegionUrl.isEmpty()) {
            try {
                val whoamiReq = Request.Builder()
                    .url("https://api.central.sophos.com/whoami/v1")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
                okHttpClient.newCall(whoamiReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: "{}"
                        val whoamiObj = json.parseToJsonElement(body).jsonObject
                        val discoveredTenant = whoamiObj["id"]?.jsonPrimitive?.contentOrNull
                        val discoveredRegion = whoamiObj["apiHosts"]?.jsonObject?.get("dataRegion")?.jsonPrimitive?.contentOrNull
                        if (!discoveredTenant.isNullOrEmpty()) tenantId = discoveredTenant
                        if (!discoveredRegion.isNullOrEmpty()) dataRegionUrl = discoveredRegion
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Whoami check skipped: ${e.message}")
            }
        }

        if (dataRegionUrl.isEmpty() || tenantId.isEmpty()) {
            Log.e(TAG, "Missing dataRegionUrl or tenantId")
            isSyncingFlow.value = false
            return@withContext
        }

        var fetchedEndpoints: List<Endpoint>? = null
        var fetchedAlerts: List<Alert>? = null

        // 1. Direct Endpoints Fetch with full pagination
        try {
            var nextKey: String? = null
            val allEndpoints = mutableListOf<Endpoint>()
            var pageIndex = 1
            val maxPages = 50

            do {
                lastSyncStatus = "Loading endpoints (page $pageIndex, ${allEndpoints.size} loaded)..."
                val urlBuilder = StringBuilder("$dataRegionUrl/endpoint/v1/endpoints?pageSize=500&view=full&pageTotal=true")
                if (!nextKey.isNullOrEmpty()) {
                    val encodedKey = URLEncoder.encode(nextKey, "UTF-8")
                    urlBuilder.append("&pageFromKey=$encodedKey")
                }

                val epReq = Request.Builder()
                    .url(urlBuilder.toString())
                    .header("Authorization", "Bearer $token")
                    .header("X-Tenant-ID", tenantId)
                    .get()
                    .build()

                var pageNextKey: String? = null

                okHttpClient.newCall(epReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: "{}"
                        val root = json.parseToJsonElement(body).jsonObject
                        val items = root["items"]?.jsonArray ?: JsonArray(emptyList())

                        for (elem in items) {
                            val epObj = elem.jsonObject
                            val id = epObj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                            val hostname = epObj["hostname"]?.jsonPrimitive?.contentOrNull ?: "Host-${id.take(6)}"

                            val ips = mutableListOf<String>()
                            epObj["ipv4Addresses"]?.jsonArray?.forEach { ipElem ->
                                ipElem.jsonPrimitive.contentOrNull?.let { ips.add(it) }
                            }
                            if (ips.isEmpty()) {
                                epObj["assignedIps"]?.jsonArray?.forEach { ipElem ->
                                    ipElem.jsonPrimitive.contentOrNull?.let { ips.add(it) }
                                }
                            }

                            val osName = epObj["os"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: "Windows"
                            val overallHealth = epObj["health"]?.jsonObject?.get("overall")?.jsonPrimitive?.contentOrNull?.uppercase() ?: "GOOD"
                            val health = when (overallHealth) {
                                "GOOD" -> HealthState.GOOD
                                "SUSPICIOUS" -> HealthState.SUSPICIOUS
                                "BAD" -> HealthState.BAD
                                else -> HealthState.GOOD
                            }

                            val isolated = epObj["isolation"]?.jsonObject?.get("isolated")?.jsonPrimitive?.booleanOrNull
                                ?: (epObj["isolation"]?.jsonObject?.get("status")?.jsonPrimitive?.contentOrNull == "isolated")

                            val tamperProtection = epObj["tamperProtectionEnabled"]?.jsonPrimitive?.booleanOrNull
                                ?: epObj["tamperProtection"]?.jsonObject?.get("enabled")?.jsonPrimitive?.booleanOrNull
                                ?: true

                            val lastSeen = epObj["lastSeenAt"]?.jsonPrimitive?.contentOrNull ?: "Recently"
                            val isServer = epObj["type"]?.jsonPrimitive?.contentOrNull?.equals("server", ignoreCase = true)
                                ?: (epObj["os"]?.jsonObject?.get("isServer")?.jsonPrimitive?.booleanOrNull ?: false)
                            val agentVersion = epObj["coreAgentVersion"]?.jsonPrimitive?.contentOrNull ?: "2026.2"
                            val groupName = epObj["group"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: "Enterprise Core"
                            val online = epObj["online"]?.jsonPrimitive?.booleanOrNull ?: false

                            allEndpoints.add(
                                Endpoint(
                                    id = id,
                                    hostname = hostname,
                                    ipAddresses = ips,
                                    os = osName,
                                    health = health,
                                    isolated = isolated,
                                    tamperProtection = tamperProtection,
                                    lastSeenAt = lastSeen,
                                    isServer = isServer,
                                    agentVersion = agentVersion,
                                    groupName = groupName,
                                    online = online
                                )
                            )
                        }

                        pageNextKey = root["pages"]?.jsonObject?.get("nextKey")?.jsonPrimitive?.contentOrNull
                        Log.i(TAG, "Endpoints page $pageIndex: fetched ${items.size} (total: ${allEndpoints.size}), nextKey: ${!pageNextKey.isNullOrEmpty()}")

                        if (allEndpoints.isNotEmpty()) {
                            endpoints.value = allEndpoints.toList()
                        }
                    } else {
                        Log.e(TAG, "Endpoints call failed page $pageIndex HTTP ${resp.code}: ${resp.body?.string()}")
                        pageNextKey = null
                    }
                }

                nextKey = pageNextKey
                pageIndex++
            } while (!nextKey.isNullOrEmpty() && pageIndex <= maxPages)

            if (allEndpoints.isNotEmpty()) {
                fetchedEndpoints = allEndpoints
                endpoints.value = allEndpoints
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching endpoints: ${e.message}", e)
        }

        // 2. Direct Alerts Fetch with full pagination
        try {
            var nextKey: String? = null
            val allAlerts = mutableListOf<Alert>()
            var pageIndex = 1
            val maxPages = 20

            do {
                lastSyncStatus = "Loading alerts (page $pageIndex, ${allAlerts.size} loaded)..."
                val urlBuilder = StringBuilder("$dataRegionUrl/common/v1/alerts?limit=100")
                if (!nextKey.isNullOrEmpty()) {
                    val encodedKey = URLEncoder.encode(nextKey, "UTF-8")
                    urlBuilder.append("&pageFromKey=$encodedKey")
                }

                val alertReq = Request.Builder()
                    .url(urlBuilder.toString())
                    .header("Authorization", "Bearer $token")
                    .header("X-Tenant-ID", tenantId)
                    .get()
                    .build()

                var pageNextKey: String? = null

                okHttpClient.newCall(alertReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: "{}"
                        val root = json.parseToJsonElement(body).jsonObject
                        val items = root["items"]?.jsonArray ?: JsonArray(emptyList())

                        for (elem in items) {
                            val aObj = elem.jsonObject
                            val id = aObj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                            val sevStr = aObj["severity"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "MEDIUM"
                            val sev = when (sevStr) {
                                "CRITICAL" -> Severity.CRITICAL
                                "HIGH" -> Severity.HIGH
                                "LOW" -> Severity.LOW
                                "INFO" -> Severity.INFO
                                else -> Severity.MEDIUM
                            }

                            val category = aObj["category"]?.jsonPrimitive?.contentOrNull ?: "Security Alert"
                            val description = aObj["description"]?.jsonPrimitive?.contentOrNull ?: "Sophos Security Event"

                            val endpointHostname = aObj["managedAgent"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                                ?: aObj["endpoint"]?.jsonObject?.get("hostname")?.jsonPrimitive?.contentOrNull
                                ?: "Managed Device"

                            val endpointId = aObj["managedAgent"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                                ?: aObj["endpoint"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                                ?: ""

                            val detectedAt = aObj["raisedAt"]?.jsonPrimitive?.contentOrNull ?: "Recently"
                            val product = aObj["product"]?.jsonPrimitive?.contentOrNull ?: "Sophos Central"
                            val threatName = aObj["threatName"]?.jsonPrimitive?.contentOrNull ?: ""
                            val path = aObj["location"]?.jsonPrimitive?.contentOrNull ?: ""

                            allAlerts.add(
                                Alert(
                                    id = id,
                                    severity = sev,
                                    category = category,
                                    description = description,
                                    endpointHostname = endpointHostname,
                                    endpointId = endpointId,
                                    detectedAt = detectedAt,
                                    status = AlertStatus.OPEN,
                                    product = product,
                                    threatName = threatName,
                                    path = path
                                )
                            )
                        }

                        pageNextKey = root["pages"]?.jsonObject?.get("nextKey")?.jsonPrimitive?.contentOrNull
                        Log.i(TAG, "Alerts page $pageIndex: fetched ${items.size} (total: ${allAlerts.size}), nextKey: ${!pageNextKey.isNullOrEmpty()}")

                        if (allAlerts.isNotEmpty()) {
                            alerts.value = allAlerts.toList()
                        }
                    } else {
                        Log.e(TAG, "Alerts call failed page $pageIndex HTTP ${resp.code}: ${resp.body?.string()}")
                        pageNextKey = null
                    }
                }

                nextKey = pageNextKey
                pageIndex++
            } while (!nextKey.isNullOrEmpty() && pageIndex <= maxPages)

            if (allAlerts.isNotEmpty()) {
                fetchedAlerts = allAlerts
                alerts.value = allAlerts
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching alerts: ${e.message}", e)
        }

        // 3. Update Dashboard Summary
        val epList = endpoints.value
        val altList = alerts.value
        val caseList = cases.value
        val detList = detections.value

        val healthyCount = epList.count { it.health == HealthState.GOOD && !it.isolated }
        val suspCount = epList.count { it.health == HealthState.SUSPICIOUS }
        val badCount = epList.count { it.health == HealthState.BAD }
        val isoCount = epList.count { it.isolated }
        val activeCount = epList.count { it.online }

        val critAlerts = altList.count { it.severity == Severity.CRITICAL && it.status == AlertStatus.OPEN }
        val highAlerts = altList.count { it.severity == Severity.HIGH && it.status == AlertStatus.OPEN }

        val success = fetchedEndpoints != null || fetchedAlerts != null
        isLiveConnected = success

        dashboardSummary.value = DashboardSummary(
            totalEndpoints = epList.size,
            activeEndpoints = activeCount,
            healthyEndpoints = healthyCount,
            suspiciousEndpoints = suspCount,
            badEndpoints = badCount,
            isolatedEndpoints = isoCount,
            activeCriticalAlerts = critAlerts,
            activeHighAlerts = highAlerts,
            openCases = caseList.count { it.status != CaseStatus.CLOSED },
            recentDetectionsCount = detList.size,
            syncFreshnessTimestamp = "Just now",
            isLiveDataSource = success,
            tenantName = "Sophos Central Production Tenant",
            dataRegion = if (dataRegionUrl.isNotEmpty()) dataRegionUrl.replace("https://", "") else "Sophos Central Cloud"
        )

        lastSyncStatus = if (success) {
            "Direct Cloud Connected (${epList.size} Endpoints, ${altList.size} Alerts)"
        } else {
            "Direct connection finished with fallback"
        }

        if (success) {
            saveEncryptedCache()
        }

        isSyncingFlow.value = false
    }

    fun isolateEndpoint(id: String, note: String): Boolean {
        endpoints.value = endpoints.value.map {
            if (it.id == id) it.copy(isolated = true, health = HealthState.BAD) else it
        }
        dispatchDirectIsolation(id, true, note)
        return true
    }

    fun restoreEndpoint(id: String, note: String): Boolean {
        endpoints.value = endpoints.value.map {
            if (it.id == id) it.copy(isolated = false, health = HealthState.GOOD) else it
        }
        dispatchDirectIsolation(id, false, note)
        return true
    }

    private fun dispatchDirectIsolation(endpointId: String, enableIsolation: Boolean, comment: String) {
        repositoryScope.launch {
            try {
                val token = getValidOAuthToken() ?: return@launch
                val jsonPayload = """
                    {
                        "enabled": $enableIsolation,
                        "comment": "${comment.replace("\"", "\\\"")}"
                    }
                """.trimIndent()

                val req = Request.Builder()
                    .url("$dataRegionUrl/endpoint/v1/endpoints/$endpointId/isolation")
                    .header("Authorization", "Bearer $token")
                    .header("X-Tenant-ID", tenantId)
                    .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(req).execute().use { resp ->
                    Log.i(TAG, "Direct isolation command code: ${resp.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed direct isolation dispatch: ${e.message}")
            }
        }
    }

    fun updateCaseStatus(id: String, newStatus: CaseStatus): Boolean {
        cases.value = cases.value.map {
            if (it.id == id) it.copy(status = newStatus) else it
        }
        return true
    }
}
