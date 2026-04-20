package com.example.mobapp

import android.content.Context
import android.net.Uri

/** Screen-time settings. */
object Prefs {
    private const val FILE = "mobapp_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_VALUE = "threshold_value"
    private const val KEY_UNIT = "threshold_unit"
    private const val KEY_SOUND_URI = "alert_sound_uri"

    const val UNIT_MINUTES = "MINUTES"
    const val UNIT_HOURS = "HOURS"

    const val DEFAULT_VALUE = 18
    const val DEFAULT_UNIT = UNIT_MINUTES

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun getThresholdValue(ctx: Context): Int =
        prefs(ctx).getInt(KEY_VALUE, DEFAULT_VALUE).coerceAtLeast(1)

    fun getThresholdUnit(ctx: Context): String =
        prefs(ctx).getString(KEY_UNIT, DEFAULT_UNIT) ?: DEFAULT_UNIT

    fun setThreshold(ctx: Context, value: Int, unit: String) {
        prefs(ctx).edit()
            .putInt(KEY_VALUE, value.coerceAtLeast(1))
            .putString(KEY_UNIT, unit)
            .apply()
    }

    fun getThresholdMs(ctx: Context): Long {
        val v = getThresholdValue(ctx).toLong()
        return when (getThresholdUnit(ctx)) {
            UNIT_HOURS -> v * 60L * 60L * 1000L
            else -> v * 60L * 1000L
        }
    }

    /** Custom alert sound URI (null = system default alarm). */
    fun getSoundUri(ctx: Context): Uri? =
        prefs(ctx).getString(KEY_SOUND_URI, null)?.let(Uri::parse)

    fun setSoundUri(ctx: Context, uri: Uri?) {
        prefs(ctx).edit().putString(KEY_SOUND_URI, uri?.toString()).apply()
    }
}
