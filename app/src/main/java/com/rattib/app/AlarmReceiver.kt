package com.rattib.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("task_id", 0L)
        val title = intent.getStringExtra("task_title") ?: "حان وقت مهمتك"
        val repeat = intent.getStringExtra("repeat") ?: "none"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("rattib_tasks", "تنبيهات المهام", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تذكيرات تطبيق رَتّب"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            })
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "rattib_tasks")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ حان وقت مهمتك")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        manager.notify(id.toInt(), notification)
        if (repeat == "daily" || repeat == "weekly") {
            val tasks = TaskStore.load(context)
            tasks.find { it.id == id }?.let { task ->
                task.triggerAt += if (repeat == "daily") 86400000L else 604800000L
                TaskStore.save(context, tasks)
                AlarmScheduler.schedule(context, task)
            }
        }
    }
}
