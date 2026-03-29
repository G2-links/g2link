package com.g2link.connect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.g2link.connect.ui.screen.*
import com.g2link.connect.ui.viewmodel.OnboardingViewModel

object Routes {
    const val ONBOARDING     = "onboarding"
    const val CHAT_LIST      = "chat_list"
    const val CHAT           = "chat/{deviceId}"
    const val BROADCAST      = "broadcast"
    const val SETTINGS       = "settings"
    const val QR_SHOW        = "qr_show"
    const val QR_SCAN        = "qr_scan"
    const val CONTACTS       = "contacts"
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
            OnboardingScreen(onComplete = {
                navController.navigate(Routes.CHAT_LIST) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onNavigateToChat = { navController.navigate(Routes.chatRoute(it)) },
                onNavigateToBroadcast = { navController.navigate(Routes.BROADCAST) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToContacts = { navController.navigate(Routes.CONTACTS) },
                onNavigateToQrShow = { navController.navigate(Routes.QR_SHOW) }
            )
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("deviceId") { type = NavType.StringType })) { backStack ->
            val deviceId = backStack.arguments?.getString("deviceId") ?: return@composable
            ChatScreen(contactDeviceId = deviceId, onBack = { navController.popBackStack() })
        }
        composable(Routes.BROADCAST) {
            BroadcastScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToQrShow = { navController.navigate(Routes.QR_SHOW) },
                onNavigateToQrScan = { navController.navigate(Routes.QR_SCAN) }
            )
        }
        composable(Routes.QR_SHOW) { QrShowScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.QR_SCAN) {
            QrScanScreen(
                onBack = { navController.popBackStack() },
                onContactAdded = { navController.popBackStack() }
            )
        }
        composable(Routes.CONTACTS) {
            ContactsScreen(
                onNavigateToChat = { navController.navigate(Routes.chatRoute(it)) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
