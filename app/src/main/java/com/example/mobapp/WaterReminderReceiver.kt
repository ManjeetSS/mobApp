package com.example.mobapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class WaterReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE = "com.example.mobapp.WATER_FIRE"
        const val NOTIF_ID = 100
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        NotifChannels.ensureAll(context)

        val intervalMin = WaterPrefs.getIntervalMinutes(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_WATER)
        }
        val tapPi = PendingIntent.getActivity(
            context, 2001, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val donePi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, WaterDoneReceiver::class.java).apply {
                action = WaterDoneReceiver.ACTION_DONE
                `package` = context.packageName
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, NotifChannels.WATER)
            .setSmallIcon(R.drawable.ic_tab_water)
            .setContentTitle(context.getString(R.string.water_notif_title))
            .setContentText(context.getString(R.string.water_notif_text, intervalMin))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.water_notif_action),
                donePi
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notif)

        if (WaterPrefs.isEnabled(context)) WaterScheduler.schedule(context)
    }
}
