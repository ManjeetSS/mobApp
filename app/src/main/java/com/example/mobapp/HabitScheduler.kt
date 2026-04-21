package com.example.mobapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Per-habit scheduling using AlarmManager.setAlarmClock — habit ID = request code. */
object HabitScheduler {

    fun schedule(ctx: Context, habit: Habit) {
        if (!habit.enabled) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + habit.intervalMinutes.toLong() * 60L * 1000L
        AlarmSchedulingCompat.scheduleBest(
            am, triggerAt, reminderPi(ctx, habit.id), showIntent(ctx, habit.id)
        )
    }

    fun cancel(ctx: Context, habitId: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(reminderPi(ctx, habitId))
    }

    fun reminderPi(ctx: Context, habitId: Int): PendingIntent {
        val i = Intent(ctx, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_FIRE
            `package` = ctx.packageName
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            ctx, habitId, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(ctx: Context, habitId: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
        return PendingIntent.getActivity(
            ctx, 100_000 + habitId, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
