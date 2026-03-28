package com.disastermesh.connect.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.g2link.connect.R  // ✅ FIX: namespace is com.g2link.connect, not com.disastermesh.connect
import com.disastermesh.connect.mesh.MeshEvent
import com.disastermesh.connect.mesh.OfflineMeshManager
import com.disastermesh.connect.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * MeshForegroundService — Keeps mesh networking alive in background.
 * Required for continuous peer discovery and message relay.
 * Uses FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE.
 */
@AndroidEntryPoint
class MeshForegroundService : Service() {

    @Inject lateinit var meshManager: OfflineMeshManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "mesh_service_channel"
        const val CHANNEL_NAME = "DisasterMesh Background"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "action_start_mesh"
        const val ACTION_STOP  = "action_stop_mesh"

        fun start(context: Context) {
            val intent = Intent(context, MeshForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MeshForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting mesh..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMesh()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startMesh()
            }
        }
        return START_STICKY // Restart if killed
    }

    private fun startMesh() {
        scope.launch {
            meshManager.initialize()
            meshManager.startMesh()

            // Observe connection status for notification updates
            meshManager.connectionStatus.collect { status ->
                val statusText = when {
                    meshManager.getActivePeerCount() > 0 ->
                        "Connected to ${meshManager.getActivePeerCount()} peer(s)"
                    else -> "Searching for nearby devices..."
                }
                updateNotification(statusText)
            }
        }

        // Observe mesh events for notifications
        scope.launch {
            meshManager.meshEvents.collect { event ->
                when (event) {
                    is MeshEvent.MessageReceived -> {
                        if (!event.packet.isBroadcast) {
                            showMessageNotification(
                                from = event.packet.senderName,
                                content = event.packet.content
                            )
                        } else {
                            showBroadcastNotification(
                                from = event.packet.senderName,
                                content = event.packet.content
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun stopMesh() {
        meshManager.stopMesh()
        scope.cancel()
    }

    override fun onDestroy() {
        stopMesh()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ═══════════════════════════════════════════════════════
    // NOTIFICATION MANAGEMENT
    // ═══════════════════════════════════════════════════════

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps DisasterMesh active for offline messaging"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)

            // High importance channel for incoming messages
            val msgChannel = NotificationChannel(
                "mesh_messages",
                "Incoming Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(msgChannel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MeshForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DisasterMesh Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_mesh_notification)  // ✅ Resolves now that R is correct
            .setContentIntent(pendingIntent)                 // ✅ Resolves as chain is unbroken
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun showMessageNotification(from: String, content: String) {
        val intent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        val intent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "mesh_messages")
            .setContentTitle("🚨 Emergency Broadcast from $from")
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

// ═══════════════════════════════════════════════════════════════
// BOOT RECEIVER — Restart service after device reboot
// ═══════════════════════════════════════════════════════════════
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MeshForegroundService.start(context)
        }
    }
}
