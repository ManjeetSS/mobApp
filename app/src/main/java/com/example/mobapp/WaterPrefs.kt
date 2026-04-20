package com.example.mobapp

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WaterPrefs {
    private const val FILE = "water_prefs"
    private const val KEY_INTERVAL = "interval_min"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DATE = "intake_date"
    private const val KEY_ML = "intake_ml"
    private const val KEY_NEXT_AT = "next_alarm_at"
    private const val KEY_GOAL = "daily_goal_ml"
    private const val KEY_SOUND_URI = "sound_uri"

    const val DEFAULT_INTERVAL = 60
    const val DEFAULT_GOAL = 2000
    const val GLASS_ML = 250

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun today() = dateFmt.format(Date())

    fun getIntervalMinutes(ctx: Context): Int =
        prefs(ctx).getInt(KEY_INTERVAL, DEFAULT_INTERVAL).coerceAtLeast(1)

    fun setIntervalMinutes(ctx: Context, v: Int) {
        prefs(ctx).edit().putInt(KEY_INTERVAL, v.coerceAtLeast(1)).apply()
    }

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, v: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, v).apply()
    }

    fun getTodayMl(ctx: Context): Int {
        val p = prefs(ctx)
        return if (p.getString(KEY_DATE, null) == today()) p.getInt(KEY_ML, 0) else 0
    }

    fun logMl(ctx: Context, ml: Int) {
        if (ml <= 0) return
        val p = prefs(ctx)
        val existing = if (p.getString(KEY_DATE, null) == today()) p.getInt(KEY_ML, 0) else 0
        p.edit().putString(KEY_DATE, today()).putInt(KEY_ML, existing + ml).apply()
    }

    fun getNextAlarmAt(ctx: Context): Long = prefs(ctx).getLong(KEY_NEXT_AT, 0L)
    fun setNextAlarmAt(ctx: Context, ts: Long) {
        prefs(ctx).edit().putLong(KEY_NEXT_AT, ts).apply()
    }

    fun getDailyGoal(ctx: Context): Int =
        prefs(ctx).getInt(KEY_GOAL, DEFAULT_GOAL).coerceAtLeast(1)

    fun setDailyGoal(ctx: Context, ml: Int) {
        prefs(ctx).edit().putInt(KEY_GOAL, ml.coerceAtLeast(1)).apply()
    }

    fun getSoundUri(ctx: Context): Uri? =
        prefs(ctx).getString(KEY_SOUND_URI, null)?.let(Uri::parse)

    fun setSoundUri(ctx: Context, uri: Uri?) {
        prefs(ctx).edit().putString(KEY_SOUND_URI, uri?.toString()).apply()
    }
}
