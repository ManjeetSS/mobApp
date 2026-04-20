package com.example.mobapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

/**
 * Centralised notification-channel management.
 *
 * Channel sound + importance are immutable after creation on Android O+, so to "change"
 * either we delete the channel and recreate it. Call [recreateScreenAlertChannel] /
 * [recreateWaterChannel] from the UI when the user picks a new sound.
 */
object NotifChannels {
    const val SCREEN_ONGOING = "screen_time_ongoing"
    const val SCREEN_ALERT = "screen_time_alert_v2"
    const val WATER = "water_reminder_v2"
    const val HABIT = "habit_reminder"

    private val ALARM_AUDIO = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val VIBRATION = longArrayOf(0, 500, 250, 500, 250, 500)

    fun ensureAll(ctx: Context) {
        val nm = nm(ctx)

        // Persistent foreground-service notification for screen-time tracking.
        nm.createNotificationChannel(NotificationChannel(
            SCREEN_ONGOING, "Screen-time tracker",
            NotificationManager.IMPORTANCE_LOW
        ))

        // Habit reminders (default importance is fine — non-disruptive nudges).
        nm.createNotificationChannel(NotificationChannel(
            HABIT, "Habit reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for your custom habits."
            enableVibration(true)
        })

        // Alarm-style channels — created with the user's chosen sound (or default alarm).
        ensureScreenAlertChannel(ctx)
        ensureWaterChannel(ctx)
    }

    private fun ensureScreenAlertChannel(ctx: Context) {
        val nm = nm(ctx)
        if (nm.getNotificationChannel(SCREEN_ALERT) != null) return
        nm.createNotificationChannel(buildAlarmChannel(
            id = SCREEN_ALERT,
            name = "Screen-time alerts",
            description = "Alerts you after the configured period of continuous screen use.",
            sound = Prefs.getSoundUri(ctx) ?: defaultAlarm()
        ))
    }

    private fun ensureWaterChannel(ctx: Context) {
        val nm = nm(ctx)
        if (nm.getNotificationChannel(WATER) != null) return
        nm.createNotificationChannel(buildAlarmChannel(
            id = WATER,
            name = "Water reminders",
            description = "Reminds you to drink water at your chosen interval.",
            sound = WaterPrefs.getSoundUri(ctx) ?: defaultAlarm()
        ))
    }

    fun recreateScreenAlertChannel(ctx: Context) {
        val nm = nm(ctx)
        nm.deleteNotificationChannel(SCREEN_ALERT)
        ensureScreenAlertChannel(ctx)
    }

    fun recreateWaterChannel(ctx: Context) {
        val nm = nm(ctx)
        nm.deleteNotificationChannel(WATER)
        ensureWaterChannel(ctx)
    }

    private fun buildAlarmChannel(
        id: String, name: String, description: String, sound: Uri
    ): NotificationChannel = NotificationChannel(
        id, name, NotificationManager.IMPORTANCE_HIGH
    ).apply {
        this.description = description
        enableVibration(true)
        vibrationPattern = VIBRATION
        setSound(sound, ALARM_AUDIO)
        setBypassDnd(false)
        lockscreenVisibility = NotificationManager.VISIBILITY_PUBLIC
    }

    fun defaultAlarm(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun nm(ctx: Context): NotificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
