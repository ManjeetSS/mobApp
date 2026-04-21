package com.example.mobapp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Shared helpers for bucketing events into yyyy-MM-dd days. */
object DailyStats {
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val labelFmt = SimpleDateFormat("EEE", Locale.getDefault())

    fun today(): String = dayFmt.format(Date())

    fun dayKey(ts: Long): String = dayFmt.format(Date(ts))

    /**
     * Returns the last [n] day keys in chronological order, ending with today.
     * e.g. n=7 → [... 6 days ago, ..., yesterday, today].
     */
    fun lastNDayKeys(n: Int): List<String> {
        val cal = Calendar.getInstance()
        // Roll back (n-1) days.
        cal.add(Calendar.DAY_OF_YEAR, -(n - 1))
        return (0 until n).map {
            val k = dayFmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            k
        }
    }

    /** Short weekday label for a day key (e.g. "Mon"). Returns "Today" for today. */
    fun shortLabel(dayKey: String): String {
        if (dayKey == today()) return "Today"
        return try {
            val d = dayFmt.parse(dayKey) ?: return dayKey
            labelFmt.format(d)
        } catch (e: Exception) {
            dayKey
        }
    }

    /** Start-of-day timestamp for the day containing [ts]. */
    fun startOfDay(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
