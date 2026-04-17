package com.example.mobapp

import android.content.Context

/** Tiny SharedPreferences helper for the persistent enabled flag. */
object Prefs {
    private const val FILE = "mobapp_prefs"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, value).apply()
    }
}
