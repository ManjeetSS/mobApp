package com.example.mobapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build

/**
 * Single place that knows how to talk to [AlarmManager] across API levels + permission states.
 *
 * On API 31+ (Android 12), exact alarm APIs (`setAlarmClock`, `setExact*`) require the
 * `SCHEDULE_EXACT_ALARM` permission. This app doesn't request that permission (it's
 * user-revocable and reserved for genuine alarm-clock apps under Google Play policy),
 * so [scheduleBest] checks [AlarmManager.canScheduleExactAlarms] and falls back to
 * [AlarmManager.setAndAllowWhileIdle] — inexact but doze-safe and permission-free —
 * which is perfectly adequate for interval-based reminders.
 *
 * A try/catch on [SecurityException] is kept as a second safety net for OEMs that return
 * `true` from `canScheduleExactAlarms()` yet still deny the call.
 */
internal object AlarmSchedulingCompat {

    /**
     * Schedule [fireIntent] to be broadcast at [triggerAt]. If exact alarms are allowed,
     * uses [AlarmManager.setAlarmClock] with [showIntent] so the alarm icon appears in
     * the status bar; otherwise uses the inexact fallback.
     */
    fun scheduleBest(
        am: AlarmManager,
        triggerAt: Long,
        fireIntent: PendingIntent,
        showIntent: PendingIntent
    ) {
        if (canUseExact(am)) {
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), fireIntent)
                return
            } catch (_: SecurityException) {
                // Fall through to inexact.
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fireIntent)
    }

    private fun canUseExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
}
