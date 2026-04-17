package com.example.mobapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the "I drank it" action on the water reminder notification. */
class WaterDoneReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DONE = "com.example.mobapp.WATER_DONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return

        WaterPrefs.logMl(context, WaterPrefs.GLASS_ML)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(WaterReminderReceiver.NOTIF_ID)

        // Restart the interval so the next reminder is a full interval from now.
        if (WaterPrefs.isEnabled(context)) {
            WaterScheduler.cancel(context)
            WaterScheduler.schedule(context)
        }
    }
}
