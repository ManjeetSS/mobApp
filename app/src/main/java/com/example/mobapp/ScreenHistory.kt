package com.example.mobapp

import android.content.Context
import org.json.JSONObject

/**
 * Per-day screen-on time totals (in milliseconds), persisted as
 * { "yyyy-MM-dd": totalMs, ... }.
 *
 * [ScreenTimeService] records completed screen-on sessions via [addMs]; sessions that
 * straddle midnight are split between the two days. While a session is in progress,
 * the service stashes its start time via [setSessionStart] so the UI can show live
 * usage (stored total + active session delta) without waiting for SCREEN_OFF.
 */
object ScreenHistory {
    private const val FILE = "screen_history"
    private const val KEY_DAYS = "days"
    private const val KEY_SESSION_START = "session_start"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun readDays(ctx: Context): JSONObject =
        runCatching { JSONObject(prefs(ctx).getString(KEY_DAYS, "{}") ?: "{}") }
            .getOrElse { JSONObject() }

    private fun writeDays(ctx: Context, obj: JSONObject) {
        prefs(ctx).edit().putString(KEY_DAYS, obj.toString()).apply()
    }

    /**
     * Record a screen-on session from [startMs] to [endMs] (both epoch ms). If the
     * session crosses midnight, the duration is split between the two days.
     */
    fun addSession(ctx: Context, startMs: Long, endMs: Long) {
        if (endMs <= startMs) return
        val obj = readDays(ctx)
        var cursor = startMs
        while (cursor < endMs) {
            val nextMidnight = DailyStats.startOfDay(cursor) + 24L * 60L * 60L * 1000L
            val segmentEnd = minOf(nextMidnight, endMs)
            val day = DailyStats.dayKey(cursor)
            val add = segmentEnd - cursor
            obj.put(day, obj.optLong(day, 0L) + add)
            cursor = segmentEnd
        }
        writeDays(ctx, obj)
    }

    fun getMs(ctx: Context, day: String): Long = readDays(ctx).optLong(day, 0L)

    fun lastNDays(ctx: Context, n: Int): List<Pair<String, Long>> {
        val obj = readDays(ctx)
        return DailyStats.lastNDayKeys(n).map { it to obj.optLong(it, 0L) }
    }

    fun setSessionStart(ctx: Context, ts: Long) {
        prefs(ctx).edit().putLong(KEY_SESSION_START, ts).apply()
    }

    fun getSessionStart(ctx: Context): Long = prefs(ctx).getLong(KEY_SESSION_START, 0L)

    /** Today's total including any in-flight session. */
    fun getMsTodayWithLive(ctx: Context): Long {
        val stored = getMs(ctx, DailyStats.today())
        val ss = getSessionStart(ctx)
        val live = if (ss > 0L) (System.currentTimeMillis() - ss).coerceAtLeast(0L) else 0L
        return stored + live
    }

    /** Same as [lastNDays] but adds the in-flight session to today's bucket. */
    fun lastNDaysWithLive(ctx: Context, n: Int): List<Pair<String, Long>> {
        val days = lastNDays(ctx, n).toMutableList()
        val ss = getSessionStart(ctx)
        if (ss > 0L && days.isNotEmpty() && days.last().first == DailyStats.today()) {
            val live = (System.currentTimeMillis() - ss).coerceAtLeast(0L)
            val (k, v) = days.last()
            days[days.size - 1] = k to (v + live)
        }
        return days
    }
}
