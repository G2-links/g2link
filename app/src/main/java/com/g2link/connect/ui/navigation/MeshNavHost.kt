package com.g2link.connect.ui.navigation // UPDATED

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.g2link.connect.ui.screen.PermissionRequestScreen // UPDATED
// Add other screen imports here as you move them (e.g., ChatScreen, SettingsScreen)

@Composable
fun MeshNavHost(
    navController: NavHostController,
    startDestination: String = "permissions"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("permissions") {
            PermissionRequestScreen(
                onPermissionsGranted = {
                    // Navigate to your main mesh/chat screen once permissions are hit
                    // navController.navigate("mesh_home") { popUpTo("permissions") { inclusive = true } }
                }
            )
        }
        
        // Add your other routes here as you move the files from disastermesh
    }
}
