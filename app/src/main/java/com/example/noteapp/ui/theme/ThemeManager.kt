package com.example.noteapp.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemeManager {
    val mode = mutableStateOf(ThemeMode.SYSTEM)
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("mode", "SYSTEM") ?: "SYSTEM"
        mode.value = try { ThemeMode.valueOf(saved) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode.value = newMode
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit().putString("mode", newMode.name).apply()
    }
}
