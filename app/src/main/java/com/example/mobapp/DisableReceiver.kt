package com.example.mobapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the "Turn off" action from the ongoing and alert notifications. */
class DisableReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DISABLE = "com.example.mobapp.ACTION_DISABLE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISABLE) return

        Prefs.setEnabled(context, false)
        context.stopService(Intent(context, ScreenTimeService::class.java))

        // Dismiss the alert notification if it's showing.
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ScreenTimeService.ALERT_NOTIF_ID)
    }
}
