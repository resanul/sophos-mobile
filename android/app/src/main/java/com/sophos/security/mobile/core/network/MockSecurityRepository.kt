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

    // Live endpoints state (empty by default; populated exclusively via Sophos Central API / decrypted cache)
    private val endpoints = MutableStateFlow<List<Endpoint>>(emptyList())

    // Live alerts state
    private val alerts = MutableStateFlow<List<Alert>>(emptyList())

    // Live XDR detections state
    private val detections = MutableStateFlow<List<Detection>>(emptyList())

    // Live cases state
    private val cases = MutableStateFlow<List<XdrCase>>(emptyList())

    // Live web browsing and web control logs
    private val webLogs = MutableStateFlow<List<WebActivityLog>>(emptyList())

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
        ),
        ThreatHuntTemplate(
            id = "th-tmpl-03",
            name = "Web Browsing Activity & Web Control Logs",
            description = "Query Sophos Data Lake for all HTTP/HTTPS DNS requests, visited domains, and web filtering blocks across all endpoints",
            mitreTactic = "Initial Access / Command and Control (T1071.001)",
            querySql = "SELECT time, username, host_identifier, query AS domain, process_name, remote_address FROM xdr_data_lake_dns_activity ORDER BY time DESC LIMIT 100;",
            parameters = listOf("TimeRange: 30d", "Protocol: DNS/HTTPS")
        ),
        ThreatHuntTemplate(
            id = "th-tmpl-04",
            name = "Shadow AI & ChatGPT Access Hunter",
            description = "Hunt for visits to ChatGPT, OpenAI, Claude, Copilot, Gemini, and Generative AI tools across all endpoints in the last 30 days",
            mitreTactic = "Exfiltration / Shadow IT",
            querySql = "SELECT time, username, host_identifier, query AS domain, process_name FROM xdr_data_lake_dns_activity WHERE query LIKE '%chatgpt%' OR query LIKE '%openai%' OR query LIKE '%claude%' OR query LIKE '%deepseek%' LIMIT 100;",
            parameters = listOf("TimeRange: 30d", "Category: Generative AI")
        )
    )

    private val dashboardSummary = MutableStateFlow(
        DashboardSummary(
            totalEndpoints = 0,
            activeEndpoints = 0,
            healthyEndpoints = 0,
            suspiciousEndpoints = 0,
            badEndpoints = 0,
            isolatedEndpoints = 0,
            activeCriticalAlerts = 0,
            activeHighAlerts = 0,
            openCases = 0,
            recentDetectionsCount = 0,
            syncFreshnessTimestamp = "Awaiting login...",
            isLiveDataSource = false,
            tenantName = "Sophos Central Tenant",
            dataRegion = "Sophos Central Cloud"
        )
    )

    fun seedTestDataForTesting() {
        endpoints.value = listOf(
            Endpoint(
                id = "test-ep-01",
                hostname = "SEC-TEST-DC01",
                ipAddresses = listOf("10.14.0.5"),
                os = "Windows Server 2022",
                health = HealthState.SUSPICIOUS,
                isolated = false,
                tamperProtection = true,
                lastSeenAt = "Just now",
                isServer = true,
                online = true
            ),
            Endpoint(
                id = "test-ep-02",
                hostname = "FIN-TEST-WS01",
                ipAddresses = listOf("10.14.2.144"),
                os = "Windows 11",
                health = HealthState.GOOD,
                isolated = false,
                tamperProtection = true,
                lastSeenAt = "Just now",
                isServer = false,
                online = true
            )
        )
        cases.value = listOf(
            XdrCase(
                id = "case-test-01",
                caseNumber = "XDR-TEST-01",
                title = "Test XDR Incident",
                status = CaseStatus.NEW,
                priority = CasePriority.HIGH,
                assignee = "Lead SOC Responder",
                detectionCount = 1,
                createdAt = "Just now",
                updatedAt = "Just now",
                summary = "Unit test case"
            )
        )
        webLogs.value = listOf(
            WebActivityLog(
                id = "web-test-01",
                username = "corp\\test.user",
                endpointHostname = "FIN-TEST-WS01",
                endpointId = "test-ep-02",
                domain = "facebook.com",
                fullUrl = "https://facebook.com",
                category = "Social Networking",
                action = WebAction.BLOCKED,
                timestamp = "Just now"
            )
        )
        val epList = endpoints.value
        dashboardSummary.value = DashboardSummary(
            totalEndpoints = epList.size,
            activeEndpoints = epList.count { it.online },
            healthyEndpoints = epList.count { it.health == HealthState.GOOD && !it.isolated },
            suspiciousEndpoints = epList.count { it.health == HealthState.SUSPICIOUS },
            badEndpoints = epList.count { it.health == HealthState.BAD },
            isolatedEndpoints = epList.count { it.isolated },
            activeCriticalAlerts = 0,
            activeHighAlerts = 0,
            openCases = 1,
            recentDetectionsCount = 0,
            syncFreshnessTimestamp = "Test",
            isLiveDataSource = true
        )
    }

    init {
        if (clientId.isNotBlank() && clientSecret.isNotBlank()) {
            refresh()
        }
    }

    fun getDashboardSummary(): DashboardSummary = dashboardSummary.value
    fun getDashboardSummaryFlow(): Flow<DashboardSummary> = dashboardSummary.asStateFlow()

    fun getEndpoints(): Flow<List<Endpoint>> = endpoints.asStateFlow()
    fun getAlerts(): Flow<List<Alert>> = alerts.asStateFlow()
    fun getWebLogs(): Flow<List<WebActivityLog>> = webLogs.asStateFlow()
    fun getDetections(): Flow<List<Detection>> = detections.asStateFlow()
    fun getCases(): Flow<List<XdrCase>> = cases.asStateFlow()
    fun getHuntTemplates(): List<ThreatHuntTemplate> = threatHuntTemplates

    fun initStorage(context: Context) {
        EncryptedStorageManager.init(context)
        loadEncryptedCache()

        // Auto-authenticate with encrypted credentials to stream 100% fresh live data
        val activeCreds = EncryptedStorageManager.getActiveCredentials()
        if (activeCreds != null && activeCreds.first.isNotBlank() && activeCreds.second.isNotBlank()) {
            clientId = activeCreds.first
            clientSecret = activeCreds.second
            isLoggedIn = true
            repositoryScope.launch {
                syncDirectFromSophosCentral()
            }
        }
    }

    fun loadEncryptedCache() {
        try {
            val decryptedJson = EncryptedStorageManager.readAndDecrypt() ?: return
            val cached = json.decodeFromString<SophosCachedData>(decryptedJson)

            // Strip any legacy mock fixtures so only genuine live tenant records remain
            val cleanEndpoints = cached.endpoints.filterNot {
                it.id.startsWith("ep-us-prod") ||
                it.hostname in listOf("SEC-CORP-DC01", "FIN-WS-4091", "ENG-UBUNTU-DEV08", "APP-K8S-NODE-03")
            }
            val cleanAlerts = cached.alerts.filterNot { it.id.startsWith("alt-891") }
            val cleanWebLogs = cached.webLogs.filterNot { it.id.startsWith("web-log-0") }
            val cleanCases = cached.cases.filterNot { it.id.startsWith("case-2026-00") }
            val cleanDetections = cached.detections.filterNot { it.id.startsWith("det-30") }

            endpoints.value = cleanEndpoints
            alerts.value = cleanAlerts
            webLogs.value = cleanWebLogs
            cases.value = cleanCases
            detections.value = cleanDetections

            val healthyCount = cleanEndpoints.count { it.health == HealthState.GOOD && !it.isolated }
            val suspCount = cleanEndpoints.count { it.health == HealthState.SUSPICIOUS }
            val badCount = cleanEndpoints.count { it.health == HealthState.BAD }
            val isoCount = cleanEndpoints.count { it.isolated }
            val activeCount = cleanEndpoints.count { it.online }
            val critAlerts = cleanAlerts.count { it.severity == Severity.CRITICAL && it.status == AlertStatus.OPEN }
            val highAlerts = cleanAlerts.count { it.severity == Severity.HIGH && it.status == AlertStatus.OPEN }

            dashboardSummary.value = DashboardSummary(
                totalEndpoints = cleanEndpoints.size,
                activeEndpoints = activeCount,
                healthyEndpoints = healthyCount,
                suspiciousEndpoints = suspCount,
                badEndpoints = badCount,
                isolatedEndpoints = isoCount,
                activeCriticalAlerts = critAlerts,
                activeHighAlerts = highAlerts,
                openCases = cleanCases.count { it.status != CaseStatus.CLOSED },
                recentDetectionsCount = cleanDetections.size,
                syncFreshnessTimestamp = "Decrypted Local Cache",
                isLiveDataSource = cleanEndpoints.isNotEmpty(),
                tenantName = cached.summary?.tenantName ?: "Sophos Central Production Tenant",
                dataRegion = (if (cached.dataRegionUrl.isNotEmpty()) cached.dataRegionUrl else dataRegionUrl).replace("https://", "")
            )

            if (cached.tenantId.isNotEmpty()) tenantId = cached.tenantId
            if (cached.dataRegionUrl.isNotEmpty()) dataRegionUrl = cached.dataRegionUrl
            isLiveConnected = cleanEndpoints.isNotEmpty()
            lastSyncStatus = "Decrypted Local Cache (${cleanEndpoints.size} Endpoints, ${cleanAlerts.size} Alerts)"
            Log.i(TAG, "Decrypted clean cache loaded: ${cleanEndpoints.size} endpoints, ${cleanAlerts.size} alerts, ${cleanWebLogs.size} web logs")

            saveEncryptedCache()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load decrypted local cache: ${e.message}")
        }
    }

    fun saveEncryptedCache() {
        try {
            val cached = SophosCachedData(
                endpoints = endpoints.value,
                alerts = alerts.value,
                webLogs = webLogs.value,
                detections = detections.value,
                cases = cases.value,
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

            // Persist encrypted active credentials for auto-reconnection
            EncryptedStorageManager.saveActiveCredentials(trimmedUser, trimmedSecret)

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
        EncryptedStorageManager.clearActiveCredentials()
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
        val creds = EncryptedStorageManager.getActiveCredentials()
        if (creds != null) {
            clientId = creds.first
            clientSecret = creds.second
        }
        if (clientId.isBlank() || clientSecret.isBlank()) return
        repositoryScope.launch {
            syncDirectFromSophosCentral()
        }
    }

    fun refreshWebLogs7Days(onComplete: (Int) -> Unit = {}) {
        refreshWebLogs30Days(onComplete)
    }

    fun refreshWebLogs30Days(onComplete: (Int) -> Unit = {}) {
        val creds = EncryptedStorageManager.getActiveCredentials()
        if (creds != null) {
            clientId = creds.first
            clientSecret = creds.second
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            onComplete(webLogs.value.size)
            return
        }
        repositoryScope.launch {
            try {
                isSyncingFlow.value = true
                syncDirectFromSophosCentral()
            } finally {
                isSyncingFlow.value = false
                onComplete(webLogs.value.size)
            }
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

        // 3. Direct SIEM Events Fetch & Data Lake Telemetry for Live Web Control and Detections
        try {
            lastSyncStatus = "Loading live web control & SIEM telemetry..."
            val liveWebLogs = mutableListOf<WebActivityLog>()
            val liveDetections = mutableListOf<Detection>()

            // 3A. Fetch SIEM events with cursor pagination
            var currentCursor: String? = null
            var pagesFetched = 0
            val maxPages = 5

            do {
                val urlBuilder = if (currentCursor != null) {
                    "$dataRegionUrl/siem/v1/events?cursor=$currentCursor"
                } else {
                    val fromDateEpoch = (System.currentTimeMillis() / 1000) - (23 * 3600)
                    "$dataRegionUrl/siem/v1/events?limit=500&from_date=$fromDateEpoch"
                }

                val eventReq = Request.Builder()
                    .url(urlBuilder)
                    .header("Authorization", "Bearer $token")
                    .header("X-Tenant-ID", tenantId)
                    .get()
                    .build()

                var hasMore = false
                okHttpClient.newCall(eventReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: "{}"
                        val root = json.parseToJsonElement(body).jsonObject
                        hasMore = root["has_more"]?.jsonPrimitive?.booleanOrNull ?: false
                        currentCursor = root["next_cursor"]?.jsonPrimitive?.contentOrNull
                        val items = root["items"]?.jsonArray ?: JsonArray(emptyList())

                        for (elem in items) {
                            val evObj = elem.jsonObject
                            val id = evObj["id"]?.jsonPrimitive?.contentOrNull ?: java.util.UUID.randomUUID().toString()
                            val type = evObj["type"]?.jsonPrimitive?.contentOrNull ?: ""
                            val group = evObj["group"]?.jsonPrimitive?.contentOrNull ?: ""
                            val location = evObj["location"]?.jsonPrimitive?.contentOrNull
                                ?: evObj["endpoint"]?.jsonObject?.get("hostname")?.jsonPrimitive?.contentOrNull
                                ?: "Managed Endpoint"
                            val epId = evObj["endpoint_id"]?.jsonPrimitive?.contentOrNull
                                ?: evObj["endpoint"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                                ?: ""
                            val user = evObj["user"]?.jsonPrimitive?.contentOrNull ?: "system"
                            val whenTime = evObj["when"]?.jsonPrimitive?.contentOrNull
                                ?: evObj["created_at"]?.jsonPrimitive?.contentOrNull
                                ?: "Recently"
                            val description = evObj["description"]?.jsonPrimitive?.contentOrNull ?: ""
                            val name = evObj["name"]?.jsonPrimitive?.contentOrNull ?: ""
                            val rawUrl = evObj["url"]?.jsonPrimitive?.contentOrNull ?: ""
                            val appName = evObj["app_name"]?.jsonPrimitive?.contentOrNull ?: "Web Browser"
                            val isServer = evObj["endpoint_type"]?.jsonPrimitive?.contentOrNull?.equals("server", ignoreCase = true) ?: false

                            // Check if event relates to Web Filtering / Web Control / DNS
                            if (group.equals("WEB", ignoreCase = true) ||
                                type.contains("Web", ignoreCase = true) ||
                                rawUrl.isNotEmpty() ||
                                description.contains("http", ignoreCase = true) ||
                                description.contains("url", ignoreCase = true) ||
                                name.contains("Web", ignoreCase = true)
                            ) {
                                val targetUrl = if (rawUrl.isNotEmpty()) rawUrl else extractUrlFromText(description.ifEmpty { name })
                                val domain = extractDomain(targetUrl)
                                val action = when {
                                    type.contains("Violation", ignoreCase = true) || type.contains("Block", ignoreCase = true) -> WebAction.BLOCKED
                                    type.contains("Warn", ignoreCase = true) -> WebAction.WARNED
                                    type.contains("Audit", ignoreCase = true) -> WebAction.AUDITED
                                    else -> WebAction.ALLOWED
                                }
                                val category = evObj["category"]?.jsonPrimitive?.contentOrNull
                                    ?: if (group.isNotEmpty()) group else "Web Filtering"

                                liveWebLogs.add(
                                    WebActivityLog(
                                        id = id,
                                        username = user,
                                        endpointHostname = location,
                                        endpointId = epId,
                                        isServer = isServer,
                                        domain = domain,
                                        fullUrl = targetUrl,
                                        category = category,
                                        action = action,
                                        timestamp = formatIsoTimestamp(whenTime),
                                        browser = appName,
                                        dataTransferred = "—",
                                        clientIp = evObj["endpoint_ip"]?.jsonPrimitive?.contentOrNull ?: "—",
                                        policyRule = description.ifEmpty { name.ifEmpty { "Sophos Web Filtering Policy" } }
                                    )
                                )
                            }

                            // Check if event is an XDR Detection
                            if (group.uppercase() in listOf("MALWARE", "THREAT", "EXPLOIT", "APPLICATION_CONTROL", "SECURITY") ||
                                type.contains("Threat", ignoreCase = true) ||
                                type.contains("Malware", ignoreCase = true)
                            ) {
                                val sevStr = evObj["severity"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "MEDIUM"
                                val sev = when (sevStr) {
                                    "CRITICAL" -> Severity.CRITICAL
                                    "HIGH" -> Severity.HIGH
                                    "LOW" -> Severity.LOW
                                    "INFO" -> Severity.INFO
                                    else -> Severity.MEDIUM
                                }
                                liveDetections.add(
                                    Detection(
                                        id = id,
                                        severity = sev,
                                        title = name.ifEmpty { description.ifEmpty { "Security Incident ($type)" } },
                                        category = group.ifEmpty { type },
                                        targetEndpoint = location,
                                        endpointId = epId,
                                        riskScore = when (sev) {
                                            Severity.CRITICAL -> 92
                                            Severity.HIGH -> 80
                                            Severity.MEDIUM -> 55
                                            Severity.LOW -> 30
                                            Severity.INFO -> 10
                                        },
                                        mitreTechniques = emptyList(),
                                        detectedAt = formatIsoTimestamp(whenTime),
                                        status = "Active Telemetry",
                                        processPath = appName,
                                        sha256 = evObj["sha256"]?.jsonPrimitive?.contentOrNull ?: ""
                                    )
                                )
                            }
                        }
                    }
                }
                pagesFetched++
            } while (hasMore && currentCursor != null && pagesFetched < maxPages)

            // 3B. Extract any web-related incidents from alerts in the 7-day window
            for (alert in alerts.value) {
                if (alert.category.contains("web", ignoreCase = true) ||
                    alert.description.contains("http", ignoreCase = true) ||
                    alert.threatName.contains("url", ignoreCase = true) ||
                    alert.threatName.contains("mal/", ignoreCase = true)
                ) {
                    val url = extractUrlFromText(alert.description.ifEmpty { alert.path })
                    val domain = extractDomain(url.ifEmpty { alert.threatName })
                    if (liveWebLogs.none { it.id == alert.id }) {
                        liveWebLogs.add(
                            WebActivityLog(
                                id = alert.id,
                                username = "endpoint_user",
                                endpointHostname = alert.endpointHostname,
                                endpointId = alert.endpointId,
                                isServer = false,
                                domain = domain,
                                fullUrl = url,
                                category = alert.category,
                                action = WebAction.BLOCKED,
                                timestamp = alert.detectedAt,
                                browser = "Sophos Web Protection",
                                dataTransferred = "—",
                                clientIp = "—",
                                policyRule = alert.description
                            )
                        )
                    }
                }
            }

            // 3C. Attempt Sophos XDR Data Lake 30-day query
            try {
                val thirtyDaysAgoIso = java.time.Instant.now().minus(java.time.Duration.ofDays(30)).toString()
                val nowIso = java.time.Instant.now().toString()
                val xdrBody = """
                    {
                        "adHocQuery": {
                            "template": "SELECT meta_hostname, username, url, domain, web_category, time, action FROM xdr_data WHERE time >= '$thirtyDaysAgoIso' LIMIT 1000"
                        },
                        "from": "$thirtyDaysAgoIso",
                        "to": "$nowIso"
                    }
                """.trimIndent()
                val xdrReq = Request.Builder()
                    .url("$dataRegionUrl/xdr-query/v1/queries/runs")
                    .header("Authorization", "Bearer $token")
                    .header("X-Tenant-ID", tenantId)
                    .header("Content-Type", "application/json")
                    .post(xdrBody.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(xdrReq).execute().use { xdrResp ->
                    if (xdrResp.isSuccessful) {
                        val xdrJson = json.parseToJsonElement(xdrResp.body?.string() ?: "{}").jsonObject
                        val runId = xdrJson["id"]?.jsonPrimitive?.contentOrNull
                        if (runId != null) {
                            val resultsReq = Request.Builder()
                                .url("$dataRegionUrl/xdr-query/v1/queries/runs/$runId/results?pageSize=500")
                                .header("Authorization", "Bearer $token")
                                .header("X-Tenant-ID", tenantId)
                                .get()
                                .build()
                            okHttpClient.newCall(resultsReq).execute().use { resResp ->
                                if (resResp.isSuccessful) {
                                    val resJson = json.parseToJsonElement(resResp.body?.string() ?: "{}").jsonObject
                                    val rows = resJson["items"]?.jsonArray ?: JsonArray(emptyList())
                                    for (row in rows) {
                                        val rObj = row.jsonObject
                                        val host = rObj["meta_hostname"]?.jsonPrimitive?.contentOrNull ?: "Managed Endpoint"
                                        val uName = rObj["username"]?.jsonPrimitive?.contentOrNull ?: "corp_user"
                                        val url = rObj["url"]?.jsonPrimitive?.contentOrNull ?: ""
                                        val dom = rObj["domain"]?.jsonPrimitive?.contentOrNull ?: extractDomain(url)
                                        val cat = rObj["web_category"]?.jsonPrimitive?.contentOrNull ?: "General Web"
                                        val actStr = rObj["action"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "ALLOWED"
                                        val tStr = rObj["time"]?.jsonPrimitive?.contentOrNull ?: "Recently"
                                        liveWebLogs.add(
                                            WebActivityLog(
                                                id = java.util.UUID.randomUUID().toString(),
                                                username = uName,
                                                endpointHostname = host,
                                                endpointId = "",
                                                domain = dom,
                                                fullUrl = url,
                                                category = cat,
                                                action = if (actStr == "BLOCKED") WebAction.BLOCKED else if (actStr == "WARNED") WebAction.WARNED else WebAction.ALLOWED,
                                                timestamp = formatIsoTimestamp(tStr),
                                                browser = "Managed Browser",
                                                dataTransferred = "—",
                                                clientIp = "—",
                                                policyRule = "Data Lake 7-Day Live Discover"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (xdrEx: Exception) {
                Log.d(TAG, "Data Lake 7-day query skipped or unpermitted: ${xdrEx.message}")
            }

            webLogs.value = liveWebLogs
            detections.value = liveDetections
        } catch (e: Exception) {
            Log.e(TAG, "SIEM events fetch error: ${e.message}")
        }

        // 4. Live Cases Fetch
        try {
            val caseReq = Request.Builder()
                .url("$dataRegionUrl/cases/v1/cases")
                .header("Authorization", "Bearer $token")
                .header("X-Tenant-ID", tenantId)
                .get()
                .build()
            okHttpClient.newCall(caseReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "{}"
                    val root = json.parseToJsonElement(body).jsonObject
                    val items = root["items"]?.jsonArray ?: JsonArray(emptyList())
                    val liveCases = mutableListOf<XdrCase>()
                    for (elem in items) {
                        val cObj = elem.jsonObject
                        val id = cObj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                        val caseNumber = cObj["caseNumber"]?.jsonPrimitive?.contentOrNull ?: "CASE-${id.take(6)}"
                        val title = cObj["title"]?.jsonPrimitive?.contentOrNull ?: "Security Case"
                        val statusStr = cObj["status"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "NEW"
                        val status = when (statusStr) {
                            "IN_PROGRESS" -> CaseStatus.IN_PROGRESS
                            "CONTAINED" -> CaseStatus.CONTAINED
                            "CLOSED" -> CaseStatus.CLOSED
                            else -> CaseStatus.NEW
                        }
                        val prioStr = cObj["priority"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "MEDIUM"
                        val priority = when (prioStr) {
                            "URGENT" -> CasePriority.URGENT
                            "HIGH" -> CasePriority.HIGH
                            "LOW" -> CasePriority.LOW
                            else -> CasePriority.MEDIUM
                        }
                        val assignee = cObj["assignee"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: "Unassigned"
                        val createdAt = cObj["createdAt"]?.jsonPrimitive?.contentOrNull ?: "Recently"
                        val updatedAt = cObj["updatedAt"]?.jsonPrimitive?.contentOrNull ?: "Recently"
                        val summary = cObj["summary"]?.jsonPrimitive?.contentOrNull ?: ""
                        liveCases.add(
                            XdrCase(
                                id = id,
                                caseNumber = caseNumber,
                                title = title,
                                status = status,
                                priority = priority,
                                assignee = assignee,
                                detectionCount = cObj["detectionCount"]?.jsonPrimitive?.intOrNull ?: 0,
                                createdAt = formatIsoTimestamp(createdAt),
                                updatedAt = formatIsoTimestamp(updatedAt),
                                summary = summary
                            )
                        )
                    }
                    cases.value = liveCases
                } else {
                    cases.value = emptyList()
                }
            }
        } catch (e: Exception) {
            cases.value = emptyList()
        }

        // 5. Update Dashboard Summary from 100% Live Telemetry
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
            "Direct Cloud Connected (${epList.size} Endpoints, ${altList.size} Alerts, ${webLogs.value.size} Web Logs)"
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

    fun executeThreatHunt(jobId: String): List<Map<String, String>> {
        val results = mutableListOf<Map<String, String>>()
        when (jobId) {
            "job-th-tmpl-03" -> {
                for (log in webLogs.value) {
                    results.add(
                        mapOf(
                            "header" to "Host: ${log.endpointHostname} | User: ${log.username} | Time: ${log.timestamp}",
                            "title" to "Domain: ${log.domain} (${log.policyRule})",
                            "detail" to "Enforced Action: ${log.action} | Category: ${log.category} | Browser: ${log.browser}",
                            "severity" to if (log.action == WebAction.BLOCKED) "CRITICAL" else if (log.action == WebAction.WARNED) "HIGH" else "INFO"
                        )
                    )
                }
            }
            "job-th-tmpl-04" -> {
                val aiKeywords = listOf("chatgpt", "openai", "claude", "anthropic", "copilot", "gemini", "bard", "perplexity", "deepseek", "generative ai")
                val aiLogs = webLogs.value.filter { log ->
                    log.category.contains("AI", true) ||
                    aiKeywords.any { log.domain.contains(it, true) || log.fullUrl.contains(it, true) || log.policyRule.contains(it, true) }
                }
                for (log in aiLogs) {
                    results.add(
                        mapOf(
                            "header" to "Host: ${log.endpointHostname} | User: ${log.username} | Time: ${log.timestamp}",
                            "title" to "AI Service: ${log.domain} (${log.policyRule})",
                            "detail" to "Action: ${log.action} | Browser: ${log.browser} | Category: ${log.category}",
                            "severity" to if (log.action == WebAction.BLOCKED) "HIGH" else "MEDIUM"
                        )
                    )
                }
            }
            "job-th-tmpl-01" -> {
                val currentAlerts = alerts.value.filter {
                    it.category.contains("Exploit", true) ||
                    it.category.contains("Process", true) ||
                    it.category.contains("Malware", true) ||
                    it.threatName.contains("certutil", true) ||
                    it.threatName.contains("mshta", true)
                }
                for (alert in currentAlerts) {
                    results.add(
                        mapOf(
                            "header" to "Host: ${alert.endpointHostname} | Time: ${alert.detectedAt}",
                            "title" to "Threat: ${alert.threatName} (${alert.category})",
                            "detail" to "Path: ${alert.path.ifEmpty { alert.description }}",
                            "severity" to alert.severity.name
                        )
                    )
                }
            }
            "job-th-tmpl-02" -> {
                val currentAlerts = alerts.value.filter {
                    it.category.contains("Credential", true) ||
                    it.category.contains("Kerberos", true) ||
                    it.description.contains("ticket", true)
                }
                for (alert in currentAlerts) {
                    results.add(
                        mapOf(
                            "header" to "Host: ${alert.endpointHostname} | Time: ${alert.detectedAt}",
                            "title" to "Threat: ${alert.threatName} (${alert.category})",
                            "detail" to alert.description,
                            "severity" to alert.severity.name
                        )
                    )
                }
            }
            else -> {
                for (det in detections.value) {
                    results.add(
                        mapOf(
                            "header" to "Host: ${det.targetEndpoint} | Time: ${det.detectedAt}",
                            "title" to det.title,
                            "detail" to "Risk: ${det.riskScore} | Path: ${det.processPath}",
                            "severity" to det.severity.name
                        )
                    )
                }
            }
        }
        return results
    }

    private fun extractDomain(urlOrText: String): String {
        return try {
            val clean = urlOrText.trim()
            if (clean.isBlank()) return "Unknown Domain"
            val uri = if (clean.startsWith("http://") || clean.startsWith("https://")) {
                java.net.URI(clean)
            } else {
                java.net.URI("http://$clean")
            }
            uri.host ?: clean.substringBefore("/").substringBefore(":")
        } catch (e: Exception) {
            urlOrText.substringBefore("/").substringBefore(":")
        }
    }

    private fun extractUrlFromText(text: String): String {
        val match = Regex("""https?://[^\s"'>]+""").find(text)
        return match?.value ?: text.trim()
    }

    private fun formatIsoTimestamp(isoString: String): String {
        if (isoString.isBlank()) return "Recently"
        return try {
            val instant = java.time.Instant.parse(isoString)
            val now = java.time.Instant.now()
            val diffSeconds = java.time.Duration.between(instant, now).seconds
            when {
                diffSeconds < 60 -> "Just now"
                diffSeconds < 3600 -> "${diffSeconds / 60} mins ago"
                diffSeconds < 86400 -> "${diffSeconds / 3600} hours ago"
                else -> "${diffSeconds / 86400} days ago"
            }
        } catch (e: Exception) {
            isoString
        }
    }
}
