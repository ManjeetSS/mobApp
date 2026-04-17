package com.example.mobapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restores all recurring work after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Screen-time monitor
        if (Prefs.isEnabled(context)) {
            ContextCompat.startForegroundService(
                context, Intent(context, ScreenTimeService::class.java)
            )
        }

        // Water reminder
        if (WaterPrefs.isEnabled(context)) {
            WaterScheduler.schedule(context)
        }

        // Habit reminders
        HabitsStore.getAll(context).filter { it.enabled }.forEach {
            HabitScheduler.schedule(context, it)
        }
    }
}
