package com.example.mobapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Thin wrapper around AlarmManager for the single water reminder. */
object WaterScheduler {
    private const val REQ_CODE = 9001

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMs = WaterPrefs.getIntervalMinutes(ctx).toLong() * 60L * 1000L
        val triggerAt = System.currentTimeMillis() + intervalMs
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, buildPendingIntent(ctx))
        WaterPrefs.setNextAlarmAt(ctx, triggerAt)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent(ctx))
        WaterPrefs.setNextAlarmAt(ctx, 0L)
    }

    private fun buildPendingIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_FIRE
            `package` = ctx.packageName
        }
        return PendingIntent.getBroadcast(
            ctx, REQ_CODE, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
