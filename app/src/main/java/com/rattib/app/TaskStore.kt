package com.rattib.app

import android.content.Context
import org.json.JSONArray

object TaskStore {
    private const val PREFS = "rattib_prefs"
    private const val KEY = "tasks"
    fun load(context: Context): MutableList<TaskItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i -> TaskItem.fromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) { mutableListOf() }
    }
    fun save(context: Context, tasks: List<TaskItem>) {
        val arr = JSONArray()
        tasks.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
