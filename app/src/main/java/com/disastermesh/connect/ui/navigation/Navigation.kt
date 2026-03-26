package com.disastermesh.connect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.disastermesh.connect.ui.screen.*
import com.disastermesh.connect.ui.viewmodel.OnboardingViewModel

// ─── Route Constants ──────────────────────────────────────
object Routes {
    const val SPLASH          = "splash"
    const val ONBOARDING      = "onboarding"
    const val CHAT_LIST       = "chat_list"
    const val CHAT            = "chat/{deviceId}"
    const val BROADCAST       = "broadcast"
    const val SETTINGS        = "settings"
    const val QR_SHOW         = "qr_show"
    const val QR_SCAN         = "qr_scan"
    const val CONTACTS        = "contacts"
    const val BROADCAST_LIST  = "broadcast_list"

    fun chatRoute(deviceId: String) = "chat/$deviceId"
}

@Composable
fun MeshNavHost(navController: NavHostController) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isDone by onboardingViewModel.isOnboardingDone.collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = if (isDone) Routes.CHAT_LIST else Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.CHAT_LIST) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onNavigateToChat = { deviceId ->
                    navController.navigate(Routes.chatRoute(deviceId))
                },
                onNavigateToBroadcast = {
                    navController.navigate(Routes.BROADCAST)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToContacts = {
                    navController.navigate(Routes.CONTACTS)
                },
                onNavigateToQrShow = {
                    navController.navigate(Routes.QR_SHOW)
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStack ->
            val deviceId = backStack.arguments?.getString("deviceId") ?: return@composable
            ChatScreen(
                contactDeviceId = deviceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BROADCAST) {
            BroadcastScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToQrShow = { navController.navigate(Routes.QR_SHOW) },
                onNavigateToQrScan = { navController.navigate(Routes.QR_SCAN) }
            )
        }

        composable(Routes.QR_SHOW) {
            QrShowScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QR_SCAN) {
            QrScanScreen(
                onBack = { navController.popBackStack() },
                onContactAdded = { navController.popBackStack() }
            )
        }

        composable(Routes.CONTACTS) {
            ContactsScreen(
                onNavigateToChat = { deviceId ->
                    navController.navigate(Routes.chatRoute(deviceId))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
