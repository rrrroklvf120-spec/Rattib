package com.rattib.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    fun schedule(context: Context, task: TaskItem) {
        if (task.done) return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("task_id", task.id); putExtra("task_title", task.title); putExtra("repeat", task.repeat)
        }
        val pending = PendingIntent.getBroadcast(context, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at = if (task.triggerAt > System.currentTimeMillis()) task.triggerAt else {
            if (task.repeat == "daily" || task.repeat == "weekly") {
                var next = task.triggerAt
                val interval = if (task.repeat == "daily") 86400000L else 604800000L
                while (next <= System.currentTimeMillis()) next += interval
                next
            } else return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }
    fun cancel(context: Context, task: TaskItem) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }
}
