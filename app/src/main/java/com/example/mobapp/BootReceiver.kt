package com.example.mobapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restarts the monitoring service after device reboot if the user had it enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.isEnabled(context)) return
        ContextCompat.startForegroundService(
            context, Intent(context, ScreenTimeService::class.java)
        )
    }
}
