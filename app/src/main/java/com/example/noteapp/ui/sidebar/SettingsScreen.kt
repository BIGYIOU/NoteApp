package com.example.noteapp.ui.sidebar

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.noteapp.ui.theme.ThemeManager
import com.example.noteapp.ui.theme.ThemeMode

object ReminderPrefs {
    private const val PREFS = "reminder_prefs"
    private const val KEY_ALARM = "use_alarm"
    private const val KEY_SHOW_MODEL = "show_model"
    private const val KEY_SHOW_WEATHER = "show_weather"
    private const val KEY_SHOW_ADDRESS = "show_address"
    private const val KEY_CUSTOM_MODEL = "custom_model"

    fun isAlarmReminder(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ALARM, true)
    }
    fun setAlarmReminder(context: Context, alarm: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ALARM, alarm).apply()
    }
    fun isShowModel(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_MODEL, true)
    }
    fun setShowModel(context: Context, show: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_MODEL, show).apply()
    }
    fun isShowWeather(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_WEATHER, false)
    }
    fun setShowWeather(context: Context, show: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_WEATHER, show).apply()
    }
    fun isShowAddress(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_ADDRESS, false)
    }
    fun setShowAddress(context: Context, show: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_ADDRESS, show).apply()
    }
    fun getCustomModel(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_MODEL, "") ?: ""
    }
    fun setCustomModel(context: Context, model: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_MODEL, model).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentMode = ThemeManager.mode.value
    var useAlarm by remember { mutableStateOf(ReminderPrefs.isAlarmReminder(context)) }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // Inline header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("设置", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            // Theme section
            Text("主题模式", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp))

            val options = listOf(
                Triple(ThemeMode.LIGHT, "浅色模式", Icons.Default.LightMode),
                Triple(ThemeMode.DARK, "深色模式", Icons.Default.DarkMode),
                Triple(ThemeMode.SYSTEM, "跟随系统", Icons.Default.BrightnessAuto)
            )

            options.forEach { (mode, label, icon) ->
                val selected = mode == currentMode
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { ThemeManager.setMode(context, mode) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(label, fontSize = 15.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f))
                    if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(20.dp))
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
            Spacer(Modifier.height(16.dp))

            // Reminder type section
            Text("提醒方式", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp))

            // Alarm option
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (useAlarm) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { useAlarm = true; ReminderPrefs.setAlarmReminder(context, true) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Alarm, null, tint = if (useAlarm) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("闹钟提醒", fontSize = 15.sp,
                    color = if (useAlarm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (useAlarm) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f))
                if (useAlarm) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))

            // Notification option
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (!useAlarm) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { useAlarm = false; ReminderPrefs.setAlarmReminder(context, false) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, null, tint = if (!useAlarm) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("消息提醒", fontSize = 15.sp,
                    color = if (!useAlarm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (!useAlarm) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f))
                if (!useAlarm) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
            Spacer(Modifier.height(16.dp))

            // Editor display options
            Text("编辑页显示", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp))

            var showModel by remember { mutableStateOf(ReminderPrefs.isShowModel(context)) }
            SelectRow("手机型号", showModel) {
                showModel = !showModel; ReminderPrefs.setShowModel(context, showModel)
            }
            if (showModel) {
                var customModel by remember { mutableStateOf(ReminderPrefs.getCustomModel(context)) }
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it; ReminderPrefs.setCustomModel(context, it) },
                    label = { Text("自定义型号 (留空则用系统型号)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textStyle = TextStyle(fontSize = 14.sp)
                )
            }
            Spacer(Modifier.height(8.dp))

            var showWeather by remember { mutableStateOf(ReminderPrefs.isShowWeather(context)) }
            SelectRow("天气", showWeather) {
                showWeather = !showWeather; ReminderPrefs.setShowWeather(context, showWeather)
            }
            Spacer(Modifier.height(8.dp))

            var showAddress by remember { mutableStateOf(ReminderPrefs.isShowAddress(context)) }
            SelectRow("地址", showAddress) {
                showAddress = !showAddress; ReminderPrefs.setShowAddress(context, showAddress)
            }
        }
    }
}

@Composable
private fun SelectRow(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
