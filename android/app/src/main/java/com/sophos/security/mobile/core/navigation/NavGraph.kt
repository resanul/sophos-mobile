package com.sophos.security.mobile.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.feature.actions.*
import com.sophos.security.mobile.feature.alerts.*
import com.sophos.security.mobile.feature.auth.*
import com.sophos.security.mobile.feature.cases.*
import com.sophos.security.mobile.feature.dashboard.*
import com.sophos.security.mobile.feature.detections.*
import com.sophos.security.mobile.feature.endpoints.*
import com.sophos.security.mobile.feature.hunting.*
import com.sophos.security.mobile.feature.more.*

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Dashboard.route, Icons.Default.Dashboard),
    BottomNavItem("Alerts", Screen.Alerts.route, Icons.Default.Warning),
    BottomNavItem("Endpoints", Screen.Endpoints.route, Icons.Default.Computer),
    BottomNavItem("Cases", Screen.Cases.route, Icons.Default.Workspaces),
    BottomNavItem("More", Screen.More.route, Icons.Default.Menu)
)

@Composable
fun SophosNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) SecurityGreen else DarkOnSurfaceMuted
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) SecurityGreen else DarkOnSurfaceMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SecurityGreenContainer
                            )
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth Flow
            composable(Screen.Splash.route) {
                SplashScreen(onNavigateToLogin = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToMfa = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Mfa.route) {
                MfaScreen(
                    onMfaSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Biometric.route) {
                BiometricPromptScreen(
                    onSuccess = { navController.popBackStack() }
                )
            }

            // 5 Main Bottom Nav Tabs
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToEndpoints = { navController.navigate(Screen.Endpoints.route) },
                    onNavigateToCases = { navController.navigate(Screen.Cases.route) },
                    onNavigateToDetections = { navController.navigate(Screen.Detections.route) },
                    onNavigateToHunt = { navController.navigate(Screen.ThreatHunting.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                    onNavigateToAlertDetail = { alertId ->
                        navController.navigate(Screen.AlertDetail.createRoute(alertId))
                    }
                )
            }
            composable(Screen.Alerts.route) {
                AlertsListScreen(
                    onNavigateToAlertDetail = { alertId ->
                        navController.navigate(Screen.AlertDetail.createRoute(alertId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Endpoints.route) {
                EndpointsListScreen(
                    onNavigateToEndpointDetail = { endpointId ->
                        navController.navigate(Screen.EndpointDetail.createRoute(endpointId))
                    }
                )
            }
            composable(Screen.Cases.route) {
                CasesListScreen(
                    onNavigateToCaseDetail = { caseId ->
                        navController.navigate(Screen.CaseDetail.createRoute(caseId))
                    }
                )
            }
            composable(Screen.More.route) {
                MoreMenuScreen(
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToSearch = { navController.navigate(Screen.GlobalSearch.route) },
                    onNavigateToSavedViews = { navController.navigate(Screen.SavedViews.route) },
                    onNavigateToAudit = { navController.navigate(Screen.AuditHistory.route) },
                    onNavigateToCatalog = { navController.navigate(Screen.ServiceCatalog.route) },
                    onNavigateToProfile = { navController.navigate(Screen.UserProfile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                    onNavigateToRecovery = { navController.navigate(Screen.RecoveryState.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Detail & Advanced Screens
            composable(
                route = Screen.AlertDetail.route,
                arguments = listOf(navArgument("alertId") { type = NavType.StringType })
            ) { backStackEntry ->
                val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
                AlertDetailScreen(
                    alertId = alertId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEndpoint = { epId ->
                        navController.navigate(Screen.EndpointDetail.createRoute(epId))
                    }
                )
            }

            composable(
                route = Screen.EndpointDetail.route,
                arguments = listOf(navArgument("endpointId") { type = NavType.StringType })
            ) { backStackEntry ->
                val endpointId = backStackEntry.arguments?.getString("endpointId") ?: ""
                EndpointDetailScreen(
                    endpointId = endpointId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToActionConfirm = { epId, action ->
                        navController.navigate(Screen.ActionConfirm.createRoute(epId, action))
                    }
                )
            }

            composable(Screen.Detections.route) {
                DetectionsListScreen(
                    onNavigateToDetectionDetail = { detId ->
                        navController.navigate(Screen.DetectionDetail.createRoute(detId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.DetectionDetail.route,
                arguments = listOf(navArgument("detectionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val detectionId = backStackEntry.arguments?.getString("detectionId") ?: ""
                DetectionDetailScreen(
                    detectionId = detectionId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEndpoint = { epId ->
                        navController.navigate(Screen.EndpointDetail.createRoute(epId))
                    }
                )
            }

            composable(
                route = Screen.CaseDetail.route,
                arguments = listOf(navArgument("caseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val caseId = backStackEntry.arguments?.getString("caseId") ?: ""
                CaseDetailScreen(
                    caseId = caseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToInvestigation = { jobId ->
                        navController.navigate(Screen.Investigation.createRoute(jobId))
                    }
                )
            }

            composable(Screen.ThreatHunting.route) {
                ThreatHuntingScreen(
                    onNavigateToInvestigation = { jobId ->
                        navController.navigate(Screen.Investigation.createRoute(jobId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Investigation.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                InvestigationScreen(
                    jobId = jobId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ActionConfirm.route,
                arguments = listOf(
                    navArgument("targetId") { type = NavType.StringType },
                    navArgument("actionType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getString("targetId") ?: ""
                val actionType = backStackEntry.arguments?.getString("actionType") ?: ""
                ActionConfirmScreen(
                    targetId = targetId,
                    actionType = actionType,
                    onNavigateToResult = { actId ->
                        navController.navigate(Screen.ActionResult.createRoute(actId)) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ActionResult.route,
                arguments = listOf(navArgument("actionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val actionId = backStackEntry.arguments?.getString("actionId") ?: ""
                ActionResultScreen(
                    actionId = actionId,
                    onNavigateToHome = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.GlobalSearch.route) {
                GlobalSearchScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SavedViews.route) {
                SavedViewsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.AuditHistory.route) {
                AuditHistoryScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.ServiceCatalog.route) {
                ServiceCatalogScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.UserProfile.route) {
                UserProfileScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.RecoveryState.route) {
                RecoveryStateScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
