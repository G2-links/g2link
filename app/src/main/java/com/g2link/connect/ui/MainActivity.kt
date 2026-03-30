package com.g2link.connect.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.g2link.connect.mesh.MeshDiagnosticsManager
import com.g2link.connect.mesh.MeshDiagnostics
import com.g2link.connect.service.MeshForegroundService
import com.g2link.connect.ui.navigation.MeshNavHost
import com.g2link.connect.ui.theme.G2Colors
import com.g2link.connect.ui.theme.G2LinkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var diagnosticsManager: MeshDiagnosticsManager

    private var permissionsGranted by mutableStateOf(false)
    private var showBluetoothDialog by mutableStateOf(false)
    private var diagnostics by mutableStateOf(MeshDiagnostics())

    private val requiredPermissions: Array<String> get() {
        val base = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            base += listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            base += listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            base += listOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.POST_NOTIFICATIONS)
        }
        return base.toTypedArray()
    }

    // ── Permission launcher ────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationOk = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val btOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            results[Manifest.permission.BLUETOOTH_SCAN] == true
        else results[Manifest.permission.BLUETOOTH] == true

        permissionsGranted = locationOk && btOk
        if (permissionsGranted) {
            checkBluetoothAndStart()
        }
    }

    // ── Bluetooth enable launcher ──────────────────────────
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val btManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val btEnabled = btManager?.adapter?.isEnabled == true
        if (btEnabled) {
            MeshForegroundService.start(this)
            showBluetoothDialog = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionsGranted = checkPermissionsGranted()
        if (permissionsGranted) checkBluetoothAndStart()

        setContent {
            G2LinkTheme {
                val diag by diagnosticsManager.diagnostics.collectAsState()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!permissionsGranted) {
                        PermissionRequestScreen(onRequestPermissions = {
                            permissionLauncher.launch(requiredPermissions)
                        })
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val navController = rememberNavController()
                            MeshNavHost(navController = navController)

                            // ── Floating diagnostics banner ────────────────
                            AnimatedVisibility(
                                visible = !diag.overallReady && diag.issues.isNotEmpty(),
                                enter = slideInVertically(),
                                exit = slideOutVertically(),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                DiagnosticsBanner(
                                    diagnostics = diag,
                                    onFixBluetooth = { requestEnableBluetooth() },
                                    onOpenSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkBluetoothAndStart() {
        val btManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val btEnabled = btManager?.adapter?.isEnabled == true

        if (!btEnabled) {
            showBluetoothDialog = true
            requestEnableBluetooth()
        } else {
            MeshForegroundService.start(this)
        }
        diagnosticsManager.runDiagnostics()
    }

    private fun requestEnableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On Android 12+, can't silently enable — direct to settings
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(intent)
        } else {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(intent)
        }
    }

    private fun checkPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

// ─── Diagnostics Banner ───────────────────────────────────
@Composable
private fun DiagnosticsBanner(
    diagnostics: MeshDiagnostics,
    onFixBluetooth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = Color(0xFF1A0A00),
        border = BorderStroke(1.dp, G2Colors.Emergency.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, null, tint = G2Colors.Emergency, modifier = Modifier.size(20.dp))
                Text("Mesh not active", color = G2Colors.Emergency, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            diagnostics.issues.forEach { issue ->
                Text("• $issue", color = Color(0xFF8BA0BF), fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!diagnostics.bluetoothEnabled) {
                    Button(
                        onClick = onFixBluetooth,
                        colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Brand),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Bluetooth, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enable Bluetooth", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                if (!diagnostics.locationPermission || !diagnostics.bluetoothPermission) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        border = BorderStroke(1.dp, G2Colors.Brand),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Open Settings", fontSize = 12.sp, color = G2Colors.Brand)
                    }
                }
            }
        }
    }
}
