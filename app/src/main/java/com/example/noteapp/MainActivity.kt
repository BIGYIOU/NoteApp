package com.example.noteapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.noteapp.ui.navigation.NoteNavGraph
import com.example.noteapp.ui.theme.NoteAppTheme
import com.example.noteapp.ui.theme.ThemeManager
import com.example.noteapp.util.Logger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Logger.init(applicationContext)
        ThemeManager.init(applicationContext)
        createNotificationChannel()
        Logger.i("App", "=== App 启动 ===")

        // Global exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e("Crash", "未捕获异常", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            NoteAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NoteNavGraph(navController = navController)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channel first to recreate with new settings
            val manager = getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel("note_reminder")

            val channel = NotificationChannel(
                "note_reminder", "笔记提醒", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "笔记定时提醒"
                enableVibration(true)
                enableLights(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
