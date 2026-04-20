package com.example.mobapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Schedules the water reminder using [AlarmManager.setAlarmClock] — the most reliable
 * AlarmManager API. Bypasses doze mode, surfaces an alarm icon in the status bar, and
 * does not require the SCHEDULE_EXACT_ALARM permission.
 */
object WaterScheduler {
    private const val REQ_FIRE = 9001
    private const val REQ_SHOW = 9002

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMs = WaterPrefs.getIntervalMinutes(ctx).toLong() * 60L * 1000L
        val triggerAt = System.currentTimeMillis() + intervalMs
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(ctx))
        am.setAlarmClock(info, fireIntent(ctx))
        WaterPrefs.setNextAlarmAt(ctx, triggerAt)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(fireIntent(ctx))
        WaterPrefs.setNextAlarmAt(ctx, 0L)
    }

    private fun fireIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_FIRE
            `package` = ctx.packageName
        }
        return PendingIntent.getBroadcast(
            ctx, REQ_FIRE, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(ctx: Context): PendingIntent {
        // Tapped when user touches the alarm icon in the status bar.
        val i = Intent(ctx, MainActivity::class.java)
        return PendingIntent.getActivity(
            ctx, REQ_SHOW, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
