package com.g2link.connect.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g2link.connect.service.MeshForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MeshForegroundService.start(context)
        }
    }
}
