package com.rattib.app

import org.json.JSONObject

data class TaskItem(
    val id: Long,
    var title: String,
    var triggerAt: Long,
    var repeat: String = "none",
    var done: Boolean = false
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("triggerAt", triggerAt)
        put("repeat", repeat); put("done", done)
    }
    companion object {
        fun fromJson(o: JSONObject) = TaskItem(
            o.optLong("id"), o.optString("title"), o.optLong("triggerAt"),
            o.optString("repeat", "none"), o.optBoolean("done", false)
        )
    }
}
