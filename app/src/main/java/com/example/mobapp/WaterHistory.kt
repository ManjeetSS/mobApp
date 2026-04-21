package com.example.mobapp

import android.content.Context
import org.json.JSONObject

/**
 * Per-day water intake totals, persisted as { "yyyy-MM-dd": totalMl, ... }
 * in SharedPreferences. Daily rollover is implicit: [WaterPrefs.logMl] also calls
 * [addMl], so each glass/custom entry lands in today's bucket.
 */
object WaterHistory {
    private const val FILE = "water_history"
    private const val KEY = "days"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun read(ctx: Context): JSONObject =
        runCatching { JSONObject(prefs(ctx).getString(KEY, "{}") ?: "{}") }
            .getOrElse { JSONObject() }

    private fun write(ctx: Context, obj: JSONObject) {
        prefs(ctx).edit().putString(KEY, obj.toString()).apply()
    }

    fun addMl(ctx: Context, ml: Int) {
        if (ml <= 0) return
        val day = DailyStats.today()
        val obj = read(ctx)
        val current = obj.optInt(day, 0)
        obj.put(day, current + ml)
        write(ctx, obj)
    }

    fun getMl(ctx: Context, day: String): Int = read(ctx).optInt(day, 0)

    /** Last [n] days in chronological order. */
    fun lastNDays(ctx: Context, n: Int): List<Pair<String, Int>> {
        val obj = read(ctx)
        return DailyStats.lastNDayKeys(n).map { it to obj.optInt(it, 0) }
    }
}
