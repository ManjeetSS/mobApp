package com.example.mobapp

import android.content.Context

/** Renders "5 minutes" / "1 hour" for UI + notification text. */
object DurationFormat {
    fun format(ctx: Context, value: Int, unit: String): String {
        val pluralId = if (unit == Prefs.UNIT_HOURS)
            R.plurals.duration_hours else R.plurals.duration_minutes
        return ctx.resources.getQuantityString(pluralId, value, value)
    }
}
