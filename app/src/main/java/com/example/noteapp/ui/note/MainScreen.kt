package com.example.noteapp.ui.note

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.noteapp.R
import com.example.noteapp.data.model.Note
import com.example.noteapp.ui.component.glass
import com.example.noteapp.ui.sidebar.SidebarContent
import com.example.noteapp.util.BackupUtils
import com.example.noteapp.util.Logger
import com.example.noteapp.util.MarkdownBlock
import com.example.noteapp.util.UserPrefs
import com.example.noteapp.util.parseMarkdownBlocks
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NoteViewModel,
    onNoteClick: (Long) -> Unit,
    onDateClick: (String) -> Unit,
    onSettings: () -> Unit,
    onRecycleBin: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val searchKeyword by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val noteCount by viewModel.noteCount.collectAsState()
    val consecutiveDays by viewModel.consecutiveDays.collectAsState()
    val dailyCounts by viewModel.dailyCounts.collectAsState()
    val showFavorites by viewModel.showFavorites.collectAsState()
    val favoriteNotes by viewModel.favoriteNotes.collectAsState()

    val displayNotes = if (showFavorites) favoriteNotes else notes

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    // Back gesture in favorites mode returns to main notes
    BackHandler(enabled = showFavorites) {
        viewModel.setShowFavorites(false)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Open drawer when returning from Settings/RecycleBin
    LaunchedEffect(Unit) {
        if (DrawerOpenSignal.consume()) {
            drawerState.open()
        }
    }

    // Reminder dialog state
    var reminderNote by remember { mutableStateOf<Note?>(null) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var pendingReminderNote by remember { mutableStateOf<Note?>(null) }

    // Notification permission launcher
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showReminderPicker = true
        else Toast.makeText(context, "需要通知权限才能收到提醒", Toast.LENGTH_SHORT).show()
    }

    // Export callback
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            scope.launch {
                val zip = BackupUtils.export(context)
                if (zip != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        zip.inputStream().use { it.copyTo(out) }
                    }
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Import callback
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val ok = BackupUtils.import(context, it)
                if (ok) {
                    Toast.makeText(context, "导入成功，请重启应用", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "导入失败，请查看日志", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.72f).glass(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp), bgColor = MaterialTheme.colorScheme.surface),
                drawerContainerColor = Color.Transparent,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                SidebarContent(
                    noteCount = noteCount,
                    consecutiveDays = consecutiveDays,
                    dailyCounts = dailyCounts,
                    onDateClick = { dateStr ->
                        scope.launch { drawerState.close(); onDateClick(dateStr) }
                    },
                    onSettings = {
                        scope.launch { drawerState.close(); onSettings() }
                    },
                    onFavorites = {
                        scope.launch { drawerState.close(); viewModel.setShowFavorites(true) }
                    },
                    onRecycleBin = {
                        scope.launch { drawerState.close(); onRecycleBin() }
                    },
                    onExport = {
                        scope.launch {
                            drawerState.close()
                            shareLauncher.launch("noteapp_backup_${System.currentTimeMillis()}.zip")
                        }
                    },
                    onImport = {
                        scope.launch {
                            drawerState.close()
                            importLauncher.launch(arrayOf("application/zip", "*/*"))
                        }
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val avatarPath = UserPrefs.getAvatarPath(context)
            val savedName = UserPrefs.getName(context)

            if (searchKeyword.isNotEmpty()) {
                // Search mode
                Column(modifier = Modifier.fillMaxSize()) {
                    MainHeader(avatarPath, savedName, scope, drawerState)
                    Spacer(Modifier.height(12.dp))
                    SearchBar(
                        value = searchKeyword,
                        onValueChange = { viewModel.setSearch(it); viewModel.searchNotes(it) },
                        onClear = { viewModel.clearSearch() }
                    )
                    if (searchResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("未找到匹配的笔记", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults, key = { it.id }) { note ->
                                SearchResultCard(note = note, onClick = { onNoteClick(note.id) })
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            } else {
                if (displayNotes.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (showFavorites) FavoritesHeader { viewModel.setShowFavorites(false) }
                        else MainHeader(avatarPath, savedName, scope, drawerState)
                        Spacer(Modifier.height(12.dp))
                        SearchBar(
                            value = searchKeyword,
                            onValueChange = { viewModel.setSearch(it); viewModel.searchNotes(it) },
                            onClear = { viewModel.clearSearch() }
                        )
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (showFavorites) "暂无收藏" else "暂无笔记，点击 + 创建", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "header") {
                            if (showFavorites) FavoritesHeader { viewModel.setShowFavorites(false) }
                            else MainHeader(avatarPath, savedName, scope, drawerState)
                        }
                        item(key = "search") {
                            SearchBar(
                                value = searchKeyword,
                                onValueChange = { viewModel.setSearch(it); viewModel.searchNotes(it) },
                                onClear = { viewModel.clearSearch() }
                            )
                        }
                        items(displayNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onPin = { viewModel.togglePinned(note) },
                            onDelete = { viewModel.deleteNote(note); viewModel.refreshStats() },
                            onRemind = {
                                reminderNote = note
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        pendingReminderNote = note
                                        notificationPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        showReminderPicker = true
                                    }
                                } else {
                                    showReminderPicker = true
                                }
                            },
                            onCancelRemind = {
                                viewModel.setReminderTime(note.id, 0)
                                com.example.noteapp.util.AlarmReceiver.cancel(context, note.id)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // FAB — open empty editor without creating note yet
            FloatingActionButton(
                onClick = { onNoteClick(-1L) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建笔记", tint = Color.White)
            }
        }
    }

    // Reminder picker dialog
    if (showReminderPicker && reminderNote != null) {
        val initialTime = if (reminderNote!!.reminderTime > 0) reminderNote!!.reminderTime
            else System.currentTimeMillis() + 30 * 60 * 1000L

        com.example.noteapp.ui.component.DateTimePickerDialog(
            initialTime = initialTime,
            showQuickSelect = true,
            onConfirm = { time ->
                val n = reminderNote!!
                viewModel.setReminderTime(n.id, time)
                try {
                    com.example.noteapp.util.AlarmReceiver.schedule(
                        context, n.id, n.title.ifBlank { "无标题" }, time
                    )
                } catch (e: SecurityException) {
                    Toast.makeText(context, "请在设置中允许「闹钟和提醒」权限", Toast.LENGTH_LONG).show()
                }
                showReminderPicker = false
            },
            onDismiss = { showReminderPicker = false }
        )
    }
    }
}

// --- Favorites Header ---

@Composable
private fun FavoritesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text("收藏", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- Header ---

@Composable
private fun MainHeader(
    avatarPath: String,
    savedName: String,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: DrawerState
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { scope.launch { drawerState.open() } },
            contentAlignment = Alignment.Center
        ) {
            if (avatarPath.isNotEmpty()) {
                AsyncImage(model = File(avatarPath), contentDescription = "头像", modifier = Modifier.fillMaxSize())
            } else {
                Text(savedName.take(1), color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("大友笔记", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- Search Bar ---

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("搜索笔记...") },
        singleLine = true,
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Text("✕", color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

// --- Note Card with swipe-to-reveal ---

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onRemind: () -> Unit,
    onCancelRemind: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }
    val imageBlocks = remember(note.content) { parseMarkdownBlocks(note.content).filterIsInstance<MarkdownBlock.Image>() }
    val plainText = remember(note.content) { note.content.replace(Regex("""!\[[^\]]*\]\(([^)]+)\)\n?"""), "").trim() }

    val density = LocalDensity.current
    val buttonWidthDp = 180.dp
    val buttonWidthPx = with(density) { buttonWidthDp.toPx() }
    val midPx = buttonWidthPx * 0.5f
    val offsetX = remember { Animatable(0f) }
    val animSpec = remember { androidx.compose.animation.core.spring<Float>(stiffness = 600f) }
    val scope = rememberCoroutineScope()

    // Capture card height without intrinsics (avoids SubcomposeLayout crash)
    var cardHeightPx by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        // Background action buttons — sized to card height
        if (cardHeightPx > 0) {
            val actionHeightDp = with(density) { cardHeightPx.toDp() }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(actionHeightDp)
                    .padding(end = 2.dp)
                    .offset { IntOffset((buttonWidthPx + offsetX.value).roundToInt(), 0) },
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End)
            ) {
                ActionButton(
                    icon = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    label = if (note.isPinned) "取消" else "置顶",
                    color = Color(0xFFFFA726),
                    onClick = { scope.launch { offsetX.animateTo(0f, animSpec) }; onPin() }
                )
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "删除",
                    color = Color(0xFFE53935),
                    onClick = { scope.launch { offsetX.animateTo(0f, animSpec) }; onDelete() }
                )
                ActionButton(
                    icon = Icons.Default.Notifications,
                    label = if (note.reminderTime > 0) "取消" else "提醒",
                    color = if (note.reminderTime > 0) Color(0xFF888888) else Color(0xFF42A5F5),
                    onClick = {
                        scope.launch { offsetX.animateTo(0f, animSpec) }
                        if (note.reminderTime > 0) onCancelRemind() else onRemind()
                    }
                )
            }
        }

        // Foreground card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { cardHeightPx = it.height }
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -midPx) offsetX.animateTo(-buttonWidthPx, animSpec)
                                else offsetX.animateTo(0f, animSpec)
                            }
                        }
                    ) { _, dragAmount ->
                        scope.launch {
                            val newX = (offsetX.value + dragAmount * 1.5f).coerceIn(-buttonWidthPx, 0f)
                            offsetX.snapTo(newX)
                        }
                    }
                }
                .clickable {
                    if (offsetX.value < 0f) {
                        scope.launch { offsetX.animateTo(0f, animSpec) }
                    } else {
                        onClick()
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = if (note.isPinned) 3.dp else 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Time display — replaces title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(Icons.Filled.PushPin, "已置顶", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    val timeShortFmt = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
                    Text(
                        text = timeShortFmt.format(Date(note.updatedAt)),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.reminderTime > 0) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Notifications, "已设置提醒", tint = Color(0xFF42A5F5), modifier = Modifier.size(14.dp))
                    }
                }
                if (plainText.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = plainText.take(100).replace("\n", " "),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                if (imageBlocks.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(imageBlocks.size, key = { imageBlocks[it].src }) { i ->
                            AsyncImage(
                                model = File(imageBlocks[i].src),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Action button for swipe ---

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(54.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color.White, fontSize = 10.sp)
        }
    }
}

// --- Search Result Card ---

@Composable
private fun SearchResultCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.title.ifBlank { "无标题" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.content.take(100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
