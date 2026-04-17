package com.example.mobapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/** Creates every notification channel the app uses. Safe to call repeatedly. */
object NotifChannels {
    const val SCREEN_ONGOING = "screen_time_ongoing"
    const val SCREEN_ALERT = "screen_time_alert"
    const val WATER = "water_reminder"
    const val HABIT = "habit_reminder"

    fun ensureAll(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            SCREEN_ONGOING, "Screen-time tracker",
            NotificationManager.IMPORTANCE_LOW
        ))

        nm.createNotificationChannel(NotificationChannel(
            SCREEN_ALERT, "Screen-time alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts you after the configured period of continuous screen use."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        })

        nm.createNotificationChannel(NotificationChannel(
            WATER, "Water reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gentle reminders to drink water."
            enableVibration(true)
        })

        nm.createNotificationChannel(NotificationChannel(
            HABIT, "Habit reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for your custom habits."
            enableVibration(true)
        })
    }
}
