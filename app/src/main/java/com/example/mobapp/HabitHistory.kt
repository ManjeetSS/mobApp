package com.example.mobapp

import android.content.Context
import org.json.JSONObject

/**
 * Per-habit, per-day completion counts. Persisted as a nested JSON:
 *   { "<habitId>": { "yyyy-MM-dd": count, ... }, ... }
 */
object HabitHistory {
    private const val FILE = "habit_history"
    private const val KEY = "habits"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun read(ctx: Context): JSONObject =
        runCatching { JSONObject(prefs(ctx).getString(KEY, "{}") ?: "{}") }
            .getOrElse { JSONObject() }

    private fun write(ctx: Context, obj: JSONObject) {
        prefs(ctx).edit().putString(KEY, obj.toString()).apply()
    }

    fun recordDone(ctx: Context, habitId: Int) {
        val day = DailyStats.today()
        val root = read(ctx)
        val key = habitId.toString()
        val habitObj = root.optJSONObject(key) ?: JSONObject()
        habitObj.put(day, habitObj.optInt(day, 0) + 1)
        root.put(key, habitObj)
        write(ctx, root)
    }

    fun lastNDays(ctx: Context, habitId: Int, n: Int): List<Pair<String, Int>> {
        val root = read(ctx)
        val habitObj = root.optJSONObject(habitId.toString()) ?: JSONObject()
        return DailyStats.lastNDayKeys(n).map { it to habitObj.optInt(it, 0) }
    }

    fun removeHabit(ctx: Context, habitId: Int) {
        val root = read(ctx)
        root.remove(habitId.toString())
        write(ctx, root)
    }
}
