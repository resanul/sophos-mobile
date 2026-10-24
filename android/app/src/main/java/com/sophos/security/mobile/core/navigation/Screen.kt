package com.sophos.security.mobile.core.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Mfa : Screen("mfa")
    data object Biometric : Screen("biometric")

    // Main 5 Bottom Nav Routes
    data object Dashboard : Screen("dashboard")
    data object Alerts : Screen("alerts")
    data object Endpoints : Screen("endpoints")
    data object Cases : Screen("cases")
    data object More : Screen("more")

    // Details & Advanced
    data object AlertDetail : Screen("alert_detail/{alertId}") {
        fun createRoute(alertId: String) = "alert_detail/$alertId"
    }
    data object EndpointDetail : Screen("endpoint_detail/{endpointId}") {
        fun createRoute(endpointId: String) = "endpoint_detail/$endpointId"
    }
    data object Detections : Screen("detections")
    data object DetectionDetail : Screen("detection_detail/{detectionId}") {
        fun createRoute(detectionId: String) = "detection_detail/$detectionId"
    }
    data object CaseDetail : Screen("case_detail/{caseId}") {
        fun createRoute(caseId: String) = "case_detail/$caseId"
    }
    data object Investigation : Screen("investigation/{jobId}") {
        fun createRoute(jobId: String) = "investigation/$jobId"
    }
    data object ThreatHunting : Screen("threat_hunting")
    data object ActionConfirm : Screen("action_confirm/{targetId}/{actionType}") {
        fun createRoute(targetId: String, actionType: String) = "action_confirm/$targetId/$actionType"
    }
    data object ActionResult : Screen("action_result/{actionId}") {
        fun createRoute(actionId: String) = "action_result/$actionId"
    }
    data object Notifications : Screen("notifications")
    data object GlobalSearch : Screen("global_search")
    data object SavedViews : Screen("saved_views")
    data object AuditHistory : Screen("audit_history")
    data object ServiceCatalog : Screen("service_catalog")
    data object UserProfile : Screen("user_profile")
    data object Settings : Screen("settings")
    data object Diagnostics : Screen("diagnostics")
    data object RecoveryState : Screen("recovery_state")
}
