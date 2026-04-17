package com.example.mobapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service that tracks continuous screen-on time and posts an alert
 * after the user-configured threshold (see [Prefs.getThresholdMs]) elapses while
 * the screen is on.
 *
 * Behavior:
 *  - On ACTION_SCREEN_ON: schedule an alert in [Prefs.getThresholdMs].
 *  - On ACTION_SCREEN_OFF: cancel any pending alert (timer resets).
 *  - When the main activity changes the threshold, it sends [ACTION_RELOAD] so
 *    the service re-reads prefs and resets the timer immediately.
 */
class ScreenTimeService : Service() {

    companion object {
        const val ONGOING_CHANNEL = "screen_time_ongoing"
        const val ALERT_CHANNEL = "screen_time_alert"
        const val ONGOING_NOTIF_ID = 1
        const val ALERT_NOTIF_ID = 2
        const val ACTION_RELOAD = "com.example.mobapp.ACTION_RELOAD"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val alertRunnable = Runnable { fireAlert() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> startTimer()
                Intent.ACTION_SCREEN_OFF -> cancelTimer()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(ONGOING_NOTIF_ID, buildOngoingNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        if (isScreenOn()) startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RELOAD && isScreenOn()) {
            // Threshold changed — reset the timer with the new value.
            startTimer()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cancelTimer()
        runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isScreenOn(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    private fun startTimer() {
        handler.removeCallbacks(alertRunnable)
        handler.postDelayed(alertRunnable, Prefs.getThresholdMs(this))
    }

    private fun cancelTimer() {
        handler.removeCallbacks(alertRunnable)
    }

    private fun fireAlert() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alertSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationText = DurationFormat.format(this, Prefs.getThresholdValue(this), Prefs.getThresholdUnit(this))

        val notif = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.alert_title))
            .setContentText(getString(R.string.alert_text, durationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(alertSound)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .addAction(
                android.R.drawable.ic_lock_power_off,
                getString(R.string.turn_off),
                disablePendingIntent()
            )
            .build()

        nm.notify(ALERT_NOTIF_ID, notif)
        vibrate()
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 500, 250, 500, 250, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        }
    }

    private fun buildOngoingNotification(): android.app.Notification {
        val tapIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ONGOING_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(getString(R.string.ongoing_title))
            .setContentText(getString(R.string.ongoing_text))
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_lock_power_off,
                getString(R.string.turn_off),
                disablePendingIntent()
            )
            .build()
    }

    private fun disablePendingIntent(): PendingIntent {
        val i = Intent(this, DisableReceiver::class.java).apply {
            action = DisableReceiver.ACTION_DISABLE
            `package` = packageName
        }
        return PendingIntent.getBroadcast(
            this, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val ongoing = NotificationChannel(
            ONGOING_CHANNEL,
            "Screen-time tracker",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(ongoing)

        val alert = NotificationChannel(
            ALERT_CHANNEL,
            "Usage alerts",
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
        }
        nm.createNotificationChannel(alert)
    }
}
