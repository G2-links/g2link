package com.g2link.connect.mesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MeshDiagnostics(
    val bluetoothAvailable: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val wifiAvailable: Boolean = false,
    val wifiEnabled: Boolean = false,
    val locationPermission: Boolean = false,
    val bluetoothPermission: Boolean = false,
    val notificationPermission: Boolean = false,
    val googlePlayServicesOk: Boolean = false,
    val overallReady: Boolean = false,
    val issues: List<String> = emptyList()
)

/**
 * MeshDiagnosticsManager — checks everything needed for mesh to work
 * and reports exactly what is missing or broken.
 */
@Singleton
class MeshDiagnosticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object { private const val TAG = "MeshDiagnostics" }

    private val _diagnostics = MutableStateFlow(MeshDiagnostics())
    val diagnostics: StateFlow<MeshDiagnostics> = _diagnostics.asStateFlow()

    fun runDiagnostics() {
        val issues = mutableListOf<String>()

        // ── Bluetooth ──────────────────────────────────────
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        val btAvailable = bluetoothAdapter != null
        val btEnabled = bluetoothAdapter?.isEnabled == true

        if (!btAvailable) issues.add("Bluetooth hardware not found on this device")
        else if (!btEnabled) issues.add("Bluetooth is turned OFF — please enable it in Settings")

        // ── WiFi ───────────────────────────────────────────
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiAvailable = wifiManager != null
        val wifiEnabled = wifiManager?.isWifiEnabled == true
        if (!wifiEnabled) issues.add("Wi-Fi is OFF — turn on Wi-Fi for better mesh range (no data used)")

        // ── Permissions ────────────────────────────────────
        val locationOk = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val btPermOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }

        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!locationOk) issues.add("Location permission denied — required for Bluetooth scanning on Android")
        if (!btPermOk) issues.add("Bluetooth permission denied — required to find nearby devices")

        // ── Google Play Services ───────────────────────────
        val gpsOk = try {
            val result = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            result == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "GPS check failed: ${e.message}")
            false
        }
        if (!gpsOk) issues.add("Google Play Services unavailable — Nearby Connections API requires it")

        val overallReady = btAvailable && btEnabled && locationOk && btPermOk && gpsOk

        _diagnostics.value = MeshDiagnostics(
            bluetoothAvailable = btAvailable,
            bluetoothEnabled = btEnabled,
            wifiAvailable = wifiAvailable,
            wifiEnabled = wifiEnabled,
            locationPermission = locationOk,
            bluetoothPermission = btPermOk,
            notificationPermission = notifOk,
            googlePlayServicesOk = gpsOk,
            overallReady = overallReady,
            issues = issues
        )

        Log.d(TAG, "Diagnostics: ready=$overallReady issues=${issues.size}")
        issues.forEach { Log.w(TAG, "Issue: $it") }
    }
}
