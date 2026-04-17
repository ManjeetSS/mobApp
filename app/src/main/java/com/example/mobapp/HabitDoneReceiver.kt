package com.example.mobapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Marks a habit complete when the user taps "Mark done" from the notification. */
class HabitDoneReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DONE = "com.example.mobapp.HABIT_DONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return
        val id = intent.getIntExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1)
        val habit = HabitsStore.get(context, id) ?: return

        HabitsStore.update(context, habit.copy(lastDoneAt = System.currentTimeMillis()))

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(habit.id)

        // Reset the interval so the next reminder is a full interval from now.
        HabitScheduler.cancel(context, habit.id)
        HabitScheduler.schedule(context, HabitsStore.get(context, id) ?: return)
    }
}
