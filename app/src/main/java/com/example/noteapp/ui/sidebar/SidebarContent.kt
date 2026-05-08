package com.example.noteapp.ui.sidebar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.noteapp.R
import com.example.noteapp.ui.component.glass
import com.example.noteapp.ui.theme.ThemeManager
import com.example.noteapp.ui.theme.ThemeMode
import com.example.noteapp.util.BackupUtils
import com.example.noteapp.util.UserPrefs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SidebarContent(
    noteCount: Int,
    consecutiveDays: Int,
    dailyCounts: Map<String, Int>,
    onDateClick: (String) -> Unit,
    onSettings: () -> Unit,
    onRecycleBin: () -> Unit,
    onFavorites: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(20.dp)
    ) {
        SidebarHeader()
        Spacer(Modifier.height(18.dp))
        StatsRow(noteCount, consecutiveDays)
        Spacer(Modifier.height(14.dp))
        CalendarHeatmap(dailyCounts, onDateClick)
        Spacer(Modifier.height(14.dp))
        SidebarMenu(onFavorites, onSettings, onRecycleBin, onExport, onImport)
    }
}

// --- Header ---

@Composable
private fun SidebarHeader() {
    val context = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(UserPrefs.getName(context)) }
    val avatarPath = remember { mutableStateOf(UserPrefs.getAvatarPath(context)) }
    val savedName = UserPrefs.getName(context)

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val path = UserPrefs.saveAvatarFile(context, it)
            if (path != null) {
                UserPrefs.setAvatarPath(context, path)
                avatarPath.value = path
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("编辑资料") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                pickLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarPath.value.isNotEmpty()) {
                            AsyncImage(
                                model = File(avatarPath.value),
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(editName.take(1), color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("点击更换头像", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("昵称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) UserPrefs.setName(context, editName.trim())
                    showEdit = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("取消") }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.clickable {
                editName = savedName
                showEdit = true
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (avatarPath.value.isNotEmpty()) {
                    AsyncImage(model = File(avatarPath.value), contentDescription = "头像", modifier = Modifier.fillMaxSize())
                } else {
                    Text(savedName.take(1), color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(savedName, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// --- Stats Row ---

@Composable
private fun StatsRow(notes: Int, streak: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("笔记数量", notes.toString(), Modifier.weight(1f))
        StatCard("连续天数", streak.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}

// --- Calendar Heatmap ---

@Composable
private fun CalendarHeatmap(dailyCounts: Map<String, Int>, onDateClick: (String) -> Unit) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthFmt = remember { SimpleDateFormat("yyyy年M月", Locale.getDefault()) }

    val now = remember { Calendar.getInstance() }
    val thisYear = now.get(Calendar.YEAR)
    val thisMonth = now.get(Calendar.MONTH)

    var displayYear by remember { mutableStateOf(thisYear) }
    var displayMonth by remember { mutableStateOf(thisMonth) }

    val todayStr = remember { fmt.format(now.time) }
    val isCurrentMonth = displayYear == thisYear && displayMonth == thisMonth

    // Build calendar grid for display month
    val weeks = remember(displayYear, displayMonth, dailyCounts) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, displayYear)
        cal.set(Calendar.MONTH, displayMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
        val offset = if (firstDayOfWeek == 1) 6 else firstDayOfWeek - 2 // 0=Mon..6=Sun
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<Pair<String?, Int>>()
        repeat(offset) { cells.add(null to 0) }
        for (day in 1..maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val key = fmt.format(cal.time)
            cells.add(key to (dailyCounts[key] ?: 0))
        }
        // Pad last week to 7 cells
        while (cells.size % 7 != 0) cells.add(null to 0)
        cells.chunked(7)
    }

    val monthLabel = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, displayYear)
        cal.set(Calendar.MONTH, displayMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        monthFmt.format(cal.time)
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (displayMonth == 0) { displayMonth = 11; displayYear-- }
                else displayMonth--
            }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
            Text(monthLabel, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = {
                if (isCurrentMonth) return@IconButton
                if (displayMonth == 11) { displayMonth = 0; displayYear++ }
                else displayMonth++
            }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.width(18.dp))
            }
        }
        Spacer(Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { (dateStr, count) ->
                        if (dateStr == null) {
                            Spacer(Modifier.size(18.dp))
                        } else {
                            val isToday = dateStr == todayStr
                            val dayNum = dateStr.substringAfterLast("-").toIntOrNull() ?: 0
                            val hasNotes = count > 0
                            val cellBg = if (hasNotes) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            val textColor = if (hasNotes) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellBg)
                                    .clickable { onDateClick(dateStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$dayNum",
                                    fontSize = 9.sp,
                                    color = textColor,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Menu List ---

@Composable
private fun SidebarMenu(onFavorites: () -> Unit, onSettings: () -> Unit, onRecycleBin: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    Spacer(Modifier.height(4.dp))

    // Data manage dialog
    var showDataDialog by remember { mutableStateOf(false) }
    if (showDataDialog) {
        AlertDialog(
            onDismissRequest = { showDataDialog = false },
            title = { Text("数据管理") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { showDataDialog = false; onExport() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, null, tint = Color(0xFFFFA726), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("导出数据", fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { showDataDialog = false; onImport() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color(0xFF42A5F5), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("导入数据", fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDataDialog = false }) { Text("取消") }
            }
        )
    }

    val menuItems = remember {
        listOf(
            MenuItem("收藏", Icons.Filled.Star, Color(0xFFFFC107), onFavorites),
            MenuItem("回收站", Icons.Default.Delete, Color(0xFFEF5350), onRecycleBin),
            MenuItem("数据管理", Icons.Default.Upload, Color(0xFFFFA726)) { showDataDialog = true },
            MenuItem("设置", Icons.Default.Settings, Color(0xFF78909C), onSettings)
        )
    }

    menuItems.forEach { item ->
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { item.onClick() }
                .padding(vertical = 11.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ThemeSelector() {
    val context = LocalContext.current
    val currentMode = ThemeManager.mode.value
    val options = listOf(
        Triple(ThemeMode.LIGHT, "浅色模式", Icons.Default.LightMode),
        Triple(ThemeMode.DARK, "深色模式", Icons.Default.DarkMode),
        Triple(ThemeMode.SYSTEM, "跟随系统", Icons.Default.BrightnessAuto)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (mode, label, icon) ->
            val selected = mode == currentMode
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { ThemeManager.setMode(context, mode) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, label, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

private data class MenuItem(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)
