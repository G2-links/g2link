package com.g2link.connect.service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.g2link.connect.R
import com.g2link.connect.mesh.MeshDiagnosticsManager
import com.g2link.connect.mesh.MeshEvent
import com.g2link.connect.mesh.OfflineMeshManager
import com.g2link.connect.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MeshForegroundService : Service() {

    @Inject lateinit var meshManager: OfflineMeshManager
    @Inject lateinit var diagnosticsManager: MeshDiagnosticsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bluetoothStateReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "MeshForegroundService"
        const val CHANNEL_ID = "mesh_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "action_start_mesh"
        const val ACTION_STOP  = "action_stop_mesh"

        fun start(context: Context) {
            val intent = Intent(context, MeshForegroundService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MeshForegroundService::class.java).apply { action = ACTION_STOP })
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildNotification("Starting G2-Link mesh...", false))
        registerBluetoothReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopMesh(); stopSelf(); return START_NOT_STICKY }
        startMesh()
        return START_STICKY
    }

    private fun startMesh() {
        scope.launch {
            // Run diagnostics first
            diagnosticsManager.runDiagnostics()
            val diag = diagnosticsManager.diagnostics.value

            if (!diag.bluetoothEnabled) {
                updateNotification("⚠ Bluetooth is OFF — tap to fix", true)
                Log.w(TAG, "Bluetooth is disabled — mesh cannot start")
                // Keep trying — will auto-start when BT is enabled via receiver
                return@launch
            }

            if (!diag.overallReady) {
                val firstIssue = diag.issues.firstOrNull() ?: "Check permissions"
                updateNotification("⚠ $firstIssue", true)
                Log.w(TAG, "Mesh not ready: ${diag.issues}")
                return@launch
            }

            try {
                meshManager.initialize()
                meshManager.startMesh()
                Log.d(TAG, "Mesh started successfully")

                // Observe peer count for notification updates
                launch {
                    meshManager.connectedPeerCount.collect { count ->
                        val text = if (count > 0) "🟢 Connected to $count peer${if (count != 1) "s" else ""}"
                                   else "🔵 Scanning for nearby devices..."
                        updateNotification(text, false)
                    }
                }

                // Observe events for message notifications
                launch {
                    meshManager.meshEvents.collect { event ->
                        when (event) {
                            is MeshEvent.MessageReceived -> {
                                if (!event.packet.isBroadcast) showMessageNotification(event.packet.senderName, event.packet.content)
                                else showBroadcastNotification(event.packet.senderName, event.packet.content)
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start mesh: ${e.message}")
                updateNotification("⚠ Mesh error — will retry", true)
                // Retry after 10 seconds
                delay(10_000)
                startMesh()
            }
        }
    }

    private fun stopMesh() {
        try { meshManager.stopMesh() } catch (e: Exception) { Log.e(TAG, "Stop error: ${e.message}") }
        scope.cancel()
    }

    // ── Listen for Bluetooth state changes ────────────────
    private fun registerBluetoothReceiver() {
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG, "Bluetooth turned ON — starting mesh")
                            updateNotification("🔵 Bluetooth enabled — starting mesh...", false)
                            startMesh()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            Log.w(TAG, "Bluetooth turned OFF — mesh stopped")
                            meshManager.stopMesh()
                            updateNotification("⚠ Bluetooth is OFF — mesh paused", true)
                        }
                    }
                }
            }
        }
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onDestroy() {
        stopMesh()
        bluetoothStateReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── NOTIFICATIONS ─────────────────────────────────────
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "G2-Link Background", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Keeps G2-Link mesh active"; setShowBadge(false) })
                createNotificationChannel(NotificationChannel(
                    "mesh_messages", "Incoming Messages", NotificationManager.IMPORTANCE_HIGH
                ).apply { enableVibration(true) })
            }
        }
    }

    private fun buildNotification(statusText: String, isWarning: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("G2-Link")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_mesh_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String, isWarning: Boolean) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(statusText, isWarning))
    }

    private fun showMessageNotification(from: String, content: String) {
        val intent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "mesh_messages")
            .setContentTitle("Message from $from")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mesh_notification)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showBroadcastNotification(from: String, content: String) {
        val intent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "mesh_messages")
            .setContentTitle("🚨 Emergency from $from")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mesh_notification)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}

// ── Boot Receiver ─────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MeshForegroundService.start(context)
        }
    }
}
