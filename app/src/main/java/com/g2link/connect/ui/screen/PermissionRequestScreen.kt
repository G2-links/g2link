package com.g2link.connect.ui.screen // UPDATED

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    onPermissionsGranted: () -> Unit
) {
    // Standard permissions needed for Bluetooth/Wi-Fi Mesh
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissionsToRequest)

    if (permissionState.allPermissionsGranted) {
        onPermissionsGranted()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "G2-Link requires permissions to connect to nearby devices without internet.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}
