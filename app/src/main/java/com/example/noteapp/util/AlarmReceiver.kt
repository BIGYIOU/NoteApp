package com.example.noteapp.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import com.example.noteapp.AlarmActivity
import com.example.noteapp.MainActivity
import com.example.noteapp.ui.sidebar.ReminderPrefs

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra("noteId", -1)
        val noteTitle = intent.getStringExtra("noteTitle") ?: "笔记提醒"

        Logger.i("AlarmReceiver", "onReceive noteId=$noteId title=$noteTitle")

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openNoteId", noteId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, noteId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val useAlarm = ReminderPrefs.isAlarmReminder(context)
        val alarmFullScreenPI = if (useAlarm) {
            val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("noteId", noteId)
                putExtra("noteTitle", noteTitle)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(context, noteId.toInt() + 1000, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else null

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("大友笔记")
            .setContentText(noteTitle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (useAlarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        if (useAlarm && alarmFullScreenPI != null) {
            // Play alarm sound directly (bypasses OEM notification restrictions)
            try {
                RingtoneManager.getRingtone(context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))?.play()
            } catch (_: Exception) {}
            try {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500, 300, 500), 0))
            } catch (_: Exception) {}
            builder.setFullScreenIntent(alarmFullScreenPI, true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
        } else if (!useAlarm) {
            builder.setSilent(true)
        }
        val notification = builder.build()

        // Clear reminder after firing
        val db = com.example.noteapp.data.db.AppDatabase.getInstance(context)
        kotlinx.coroutines.runBlocking {
            db.noteDao().setReminderTime(noteId, 0)
            Logger.i("AlarmReceiver", "提醒已清除 noteId=$noteId")
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify("note_$noteId", noteId.toInt(), notification)
        Logger.i("AlarmReceiver", "通知已弹出")
    }

    companion object {
        const val CHANNEL_ID = "note_reminder"

        fun schedule(context: Context, noteId: Long, title: String, timeMillis: Long) {
            cancel(context, noteId)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("noteId", noteId)
                putExtra("noteTitle", title)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, noteId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            Logger.i("AlarmReceiver", "闹钟已设置 noteId=$noteId time=${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(timeMillis))}")
        }

        fun cancel(context: Context, noteId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, noteId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Logger.i("AlarmReceiver", "闹钟已取消 noteId=$noteId")
        }
    }
}
