package com.example.mobapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists the list of habits as a JSON array in SharedPreferences. */
object HabitsStore {
    private const val FILE = "habits_prefs"
    private const val KEY_LIST = "habits"
    private const val KEY_NEXT_ID = "next_id"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getAll(ctx: Context): List<Habit> {
        val raw = prefs(ctx).getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> fromJson(arr.getJSONObject(i)) }
        }.getOrElse { emptyList() }
    }

    fun get(ctx: Context, id: Int): Habit? = getAll(ctx).firstOrNull { it.id == id }

    fun add(ctx: Context, name: String, intervalMinutes: Int): Habit {
        val id = nextId(ctx)
        val h = Habit(id, name, intervalMinutes, enabled = true, lastDoneAt = 0L)
        saveAll(ctx, getAll(ctx) + h)
        return h
    }

    fun remove(ctx: Context, id: Int) {
        saveAll(ctx, getAll(ctx).filter { it.id != id })
    }

    fun update(ctx: Context, habit: Habit) {
        saveAll(ctx, getAll(ctx).map { if (it.id == habit.id) habit else it })
    }

    private fun saveAll(ctx: Context, list: List<Habit>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs(ctx).edit().putString(KEY_LIST, arr.toString()).apply()
    }

    private fun nextId(ctx: Context): Int {
        val p = prefs(ctx)
        val id = p.getInt(KEY_NEXT_ID, 1)
        p.edit().putInt(KEY_NEXT_ID, id + 1).apply()
        return id
    }

    private fun toJson(h: Habit): JSONObject = JSONObject().apply {
        put("id", h.id)
        put("name", h.name)
        put("intervalMinutes", h.intervalMinutes)
        put("enabled", h.enabled)
        put("lastDoneAt", h.lastDoneAt)
    }

    private fun fromJson(o: JSONObject): Habit = Habit(
        id = o.getInt("id"),
        name = o.getString("name"),
        intervalMinutes = o.getInt("intervalMinutes"),
        enabled = o.optBoolean("enabled", true),
        lastDoneAt = o.optLong("lastDoneAt", 0L)
    )
}
