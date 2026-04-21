package com.example.mobapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/** Shows the "did you complete X?" notification when a habit's alarm fires. */
class HabitReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE = "com.example.mobapp.HABIT_FIRE"
        const val EXTRA_HABIT_ID = "habit_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getIntExtra(EXTRA_HABIT_ID, -1)
        val habit = HabitsStore.get(context, id) ?: return
        if (!habit.enabled) return

        NotifChannels.ensureAll(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_HABITS)
        }
        // Habit PendingIntents are per-habit-id (so each habit's notification keeps
        // its own tap target); offset by 3000 to avoid colliding with the water/screen
        // request codes.
        val tapPi = PendingIntent.getActivity(
            context, 3000 + id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val donePi = PendingIntent.getBroadcast(
            context, id,
            Intent(context, HabitDoneReceiver::class.java).apply {
                action = HabitDoneReceiver.ACTION_DONE
                `package` = context.packageName
                putExtra(EXTRA_HABIT_ID, id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, NotifChannels.HABIT)
            .setSmallIcon(R.drawable.ic_tab_habits)
            .setContentTitle(context.getString(R.string.habit_notif_title, habit.name))
            .setContentText(context.getString(R.string.habit_notif_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.habit_notif_action),
                donePi
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(habit.id, notif)

        // Chain the next alarm so reminders keep coming until disabled / completed.
        HabitScheduler.schedule(context, habit)
    }
}
