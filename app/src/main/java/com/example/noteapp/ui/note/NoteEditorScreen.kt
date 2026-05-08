package com.example.noteapp.ui.note

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.noteapp.data.model.Note
import com.example.noteapp.util.ImageUtils
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build as AndroidBuild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.example.noteapp.util.MarkdownBlock
import com.example.noteapp.util.parseInline
import com.example.noteapp.util.parseMarkdownBlocks
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val imageTagRegex = Regex("""!\[[^\]]*\]\(([^)]+)\)""")

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun getAudioDurationMs(file: File): Int {
    return try {
        val mp = MediaPlayer()
        mp.setDataSource(file.absolutePath)
        mp.prepare()
        val dur = mp.duration
        mp.release()
        dur
    } catch (_: Exception) { 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    val focusRequester = remember { FocusRequester() }

    // Location permission for weather/address
    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* retry fetch after permission */ }

    var title by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val textContent by remember { derivedStateOf { textFieldValue.text } }
    val imageList = remember { mutableStateListOf<MarkdownBlock.Image>() }
    val audioList = remember { mutableStateListOf<MarkdownBlock.Audio>() }
    var isPreview by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf<Note?>(null) }
    var selectedTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    val recorderRef = remember { mutableStateOf<MediaRecorder?>(null) }
    val recordingRunnable = remember { mutableStateOf<Runnable?>(null) }

    // Playback state
    var playingPath by remember { mutableStateOf<String?>(null) }
    var playbackPos by remember { mutableIntStateOf(0) }
    var playbackDur by remember { mutableIntStateOf(0) }
    val playerRef = remember { mutableStateOf<MediaPlayer?>(null) }
    val playbackRunnable = remember { mutableStateOf<Runnable?>(null) }

    // Permission
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = ImageUtils.getAudioDir(context)
            val file = File(dir, "audio_${System.currentTimeMillis()}.m4a")
            recordingFile = file
            try {
                val mr = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                recorderRef.value = mr
                isRecording = true
                recordingSeconds = 0
                val r = object : Runnable {
                    override fun run() {
                        recordingSeconds++
                        recordingRunnable.value = this
                        handler.postDelayed(this, 1000)
                    }
                }
                recordingRunnable.value = r
                handler.postDelayed(r, 1000)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun beginRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            val dir = ImageUtils.getAudioDir(context)
            val file = File(dir, "audio_${System.currentTimeMillis()}.m4a")
            recordingFile = file
            try {
                val mr = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                recorderRef.value = mr
                isRecording = true
                recordingSeconds = 0
                val r = object : Runnable {
                    override fun run() {
                        recordingSeconds++
                        recordingRunnable.value = this
                        handler.postDelayed(this, 1000)
                    }
                }
                recordingRunnable.value = r
                handler.postDelayed(r, 1000)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun endRecording() {
        try { recorderRef.value?.apply { stop(); release() } } catch (_: Exception) {}
        recorderRef.value = null
        recordingRunnable.value?.let { handler.removeCallbacks(it) }
        recordingRunnable.value = null
        isRecording = false
        val f = recordingFile
        recordingSeconds = 0
        if (f != null && f.exists() && f.length() > 0) {
            val dur = getAudioDurationMs(f)
            audioList.add(MarkdownBlock.Audio(src = f.absolutePath, durationMs = dur.toLong()))
        }
    }

    // Playback
    fun stopPlaybackInternal() {
        try { playerRef.value?.apply { if (isPlaying) stop(); release() } } catch (_: Exception) {}
        playerRef.value = null
        playbackRunnable.value?.let { handler.removeCallbacks(it) }
        playbackRunnable.value = null
        playingPath = null
        playbackPos = 0
        playbackDur = 0
    }

    fun togglePlayback(path: String) {
        if (playingPath == path) {
            val mp = playerRef.value ?: return
            if (mp.isPlaying) {
                mp.pause()
            } else {
                mp.start()
                val r = object : Runnable {
                    override fun run() {
                        val p = playerRef.value
                        if (p != null && p.isPlaying) {
                            playbackPos = p.currentPosition / 1000
                            playbackDur = p.duration / 1000
                            playbackRunnable.value = this
                            handler.postDelayed(this, 200)
                        }
                    }
                }
                playbackRunnable.value = r
                handler.postDelayed(r, 200)
            }
        } else {
            stopPlaybackInternal()
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                }
                playerRef.value = mp
                val durSec = mp.duration / 1000
                playingPath = path
                playbackPos = 0
                playbackDur = durSec

                mp.setOnCompletionListener {
                    handler.removeCallbacks(playbackRunnable.value ?: return@setOnCompletionListener)
                    playbackRunnable.value = null
                    try { mp.release() } catch (_: Exception) {}
                    playerRef.value = null
                    playingPath = null
                    playbackPos = 0
                    playbackDur = 0
                }

                val r = object : Runnable {
                    override fun run() {
                        val p = playerRef.value
                        if (p != null && p.isPlaying) {
                            playbackPos = p.currentPosition / 1000
                            playbackDur = p.duration / 1000
                            playbackRunnable.value = this
                            handler.postDelayed(this, 200)
                        }
                    }
                }
                playbackRunnable.value = r
                handler.postDelayed(r, 200)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            try { playerRef.value?.apply { if (isPlaying) stop(); release() } } catch (_: Exception) {}
            try { recorderRef.value?.apply { stop(); release() } } catch (_: Exception) {}
        }
    }

    // Load note
    val isNewNote = noteId == -1L

    // Auto-focus content field when editor opens
    LaunchedEffect(isLoaded) {
        if (isLoaded) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(noteId) {
        if (isNewNote) {
            isLoaded = true
        } else {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                currentNote = note
                title = note.title
                selectedTime = note.createdAt
                val blocks = parseMarkdownBlocks(note.content)
                imageList.clear()
                audioList.clear()
                imageList.addAll(blocks.filterIsInstance<MarkdownBlock.Image>())
                audioList.addAll(blocks.filterIsInstance<MarkdownBlock.Audio>())
                val plainText = imageTagRegex.replace(note.content, "").trimEnd()
                val cleanText = plainText.replace(Regex("\n{3,}"), "\n\n")
                textFieldValue = TextFieldValue(cleanText.trim())
            }
            isLoaded = true
        }
    }

    fun buildFullContent(): String {
        val text = textContent.trimEnd()
        val imageTags = imageList.joinToString("\n") { "![image](${it.src})" }
        val audioTags = audioList.joinToString("\n") { "![audio](${it.src})" }
        val media = listOfNotNull(
            if (imageTags.isNotEmpty()) imageTags else null,
            if (audioTags.isNotEmpty()) audioTags else null
        ).joinToString("\n")
        return if (media.isNotEmpty()) { if (text.isNotEmpty()) "$text\n\n$media" else media } else text
    }

    // Atomic guard to prevent double-click — reads are instant, no snapshot delay
    val savingGuard = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    var pendingBack by remember { mutableStateOf(false) }

    // When save completes for new note, trigger back navigation
    LaunchedEffect(pendingBack) {
        if (pendingBack) {
            // Small delay to ensure DB write committed
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    fun saveAndBack() {
        // Atomic guard: only one save at a time, no snapshot-delay race
        if (!savingGuard.compareAndSet(false, true)) return
        val fullContent = buildFullContent()
        // Skip empty new notes — just go back
        if (isNewNote && currentNote == null && title.isBlank() && fullContent.isBlank()) {
            savingGuard.set(false)
            onBack()
            return
        }
        val autoTitle = if (title.isBlank() && textContent.isNotBlank()) {
            textContent.lines().firstOrNull()?.take(30) ?: ""
        } else title

        val note = currentNote
        if (note == null) {
            // New note: must wait for insert to complete
            scope.launch {
                val id = viewModel.insertNote(autoTitle, fullContent)
                currentNote = Note(id = id, title = autoTitle, content = fullContent,
                    createdAt = selectedTime, updatedAt = selectedTime)
                savingGuard.set(false)
                pendingBack = true
            }
        } else {
            viewModel.updateNote(note.copy(title = autoTitle, content = fullContent))
            savingGuard.set(false)
            onBack()
        }
    }

    val charCount = remember(textContent) { textContent.length }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除笔记") },
            text = { Text("确定删除这条笔记？") },
            confirmButton = {
                TextButton(onClick = {
                    currentNote?.let { viewModel.deleteNote(it) }
                    showDeleteDialog = false
                    onBack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    // Time dialog
    if (showTimeDialog) {
        com.example.noteapp.ui.component.DateTimePickerDialog(
            initialTime = selectedTime,
            onConfirm = { t ->
                selectedTime = t
                currentNote?.let {
                    viewModel.updateNoteTime(it, t)
                    currentNote = it.copy(createdAt = t, updatedAt = t)
                }
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }

    // Image launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { u -> scope.launch {
        val ext = getFileExtension(context, u) ?: "jpg"
        ImageUtils.copyUriToImages(context, u, ext)?.let { imageList.add(MarkdownBlock.Image("image", it.absolutePath)) }
    } } }

    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) scope.launch {
        cameraTempFile?.let { imageList.add(MarkdownBlock.Image("image", it.absolutePath)); cameraTempFile = null }
    } }

    var fullScreenImage by remember { mutableStateOf<MarkdownBlock.Image?>(null) }
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    // --- UI ---
    if (!isLoaded) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("加载中...", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { stopPlaybackInternal(); saveAndBack() }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                }

                Spacer(Modifier.weight(1f))

                // Modify time button
                TextButton(onClick = { showTimeDialog = true }) {
                    Text("修改时间", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }

                // Favorite text button
                val isFav = currentNote?.isFavorite == true
                TextButton(onClick = {
                    currentNote?.let { viewModel.toggleFavorite(it); currentNote = it.copy(isFavorite = !it.isFavorite) }
                }) {
                    Text(if (isFav) "已收藏" else "收藏",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (isFav) Color(0xFFFFC107) else MaterialTheme.colorScheme.primary)
                }

                TextButton(onClick = { stopPlaybackInternal(); saveAndBack() }) {
                    Text("保存", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Recording indicator
            if (isRecording) {
                val pulse = rememberInfiniteTransition(label = "p")
                val alpha by pulse.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a")
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFEF5350).copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFE53935).copy(alpha = alpha)))
                    Spacer(Modifier.width(10.dp))
                    Text("录音中", fontSize = 14.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Text(formatDuration(recordingSeconds), fontSize = 14.sp, color = Color(0xFFE53935), fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { endRecording() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Stop, "停止", tint = Color(0xFFE53935), modifier = Modifier.size(22.dp))
                    }
                }
            }

            if (isPreview) {
                // Preview mode
                val fullContent = remember(buildFullContent()) { buildFullContent() }
                val blocks = remember(fullContent) { parseMarkdownBlocks(fullContent) }
                LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 16.dp)) {
                    if (title.isNotBlank()) {
                        item(key = "pt") {
                            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    itemsIndexed(blocks, key = { i, _ -> i }) { _, block ->
                        when (block) {
                            is MarkdownBlock.Heading -> PreviewHeading(block)
                            is MarkdownBlock.Paragraph -> PreviewParagraph(block)
                            is MarkdownBlock.Image -> PreviewImage(block)
                            is MarkdownBlock.Audio -> AudioBar(block.src, block.durationMs, playingPath == block.src, playbackPos, playbackDur, onTogglePlay = { togglePlayback(block.src) })
                            is MarkdownBlock.CodeBlock -> PreviewCodeBlock(block)
                            is MarkdownBlock.Quote -> PreviewQuote(block)
                            is MarkdownBlock.ListItem -> PreviewListItem(block)
                            is MarkdownBlock.BlankLine -> Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                // Edit mode — flat layout
                LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
                    // Time
                    item {
                        var liveTime by remember { mutableStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                liveTime = System.currentTimeMillis()
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                        val timeSecFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                        Text(
                            timeSecFmt.format(Date(liveTime)),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                        )
                    }

                    // Content
                    item {
                        BasicTextField(
                            value = textFieldValue, onValueChange = { textFieldValue = it },
                            textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 260.dp).focusRequester(focusRequester),
                            decorationBox = { inner ->
                                Box { if (textContent.isEmpty()) Text("此刻的想法...", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), lineHeight = 26.sp); inner() }
                            }
                        )
                    }

                    // Device info
                    item {
                        val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
                        val showModel = prefs.getBoolean("show_model", true)
                        val showWeather = prefs.getBoolean("show_weather", false)
                        val showAddress = prefs.getBoolean("show_address", false)

                        var weatherInfo by remember { mutableStateOf("") }
                        var addressInfo by remember { mutableStateOf("") }
                        LaunchedEffect(showWeather, showAddress, isLoaded) {
                            if ((showWeather || showAddress) && isLoaded) {
                                val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    locationPermLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                                withContext(Dispatchers.IO) {
                                    try {
                                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                        val loc = if (hasPerm) {
                                            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                        } else null
                                        if (loc != null) {
                                            if (showAddress) {
                                                val geo = Geocoder(context, java.util.Locale.getDefault())
                                                val addrs = geo.getFromLocation(loc.latitude, loc.longitude, 1)
                                                addrs?.firstOrNull()?.let { addr ->
                                                    val parts = listOfNotNull(
                                                        addr.adminArea, addr.subAdminArea, addr.locality,
                                                        addr.subLocality, addr.featureName, addr.thoroughfare
                                                    ).filter { it.isNotBlank() }
                                                    val unique = mutableListOf<String>()
                                                    for (p in parts) {
                                                        if (unique.none { it.contains(p) || p.contains(it) }) unique.add(p)
                                                    }
                                                    addressInfo = unique.joinToString("")
                                                }
                                            }
                                            if (showWeather) {
                                                try {
                                                    val url = "https://api.open-meteo.com/v1/forecast?latitude=${loc.latitude}&longitude=${loc.longitude}&current_weather=true"
                                                    val json = URL(url).readText()
                                                    val root = org.json.JSONObject(json)
                                                    val temp = root.getJSONObject("current_weather").getDouble("temperature")
                                                    val weatherCode = root.getJSONObject("current_weather").optInt("weathercode", 0)
                                                    val desc = when (weatherCode) {
                                                        0, 1 -> "晴"; 2 -> "多云"; 3 -> "阴"
                                                        in 50..59 -> "雨"; in 70..79 -> "雪"; else -> ""
                                                    }
                                                    weatherInfo = if (desc.isNotEmpty()) "$desc ${temp.toInt()}C" else "${temp.toInt()}C"
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }

                        val customModel = remember { com.example.noteapp.ui.sidebar.ReminderPrefs.getCustomModel(context) }
                        val displayModel = customModel.ifBlank { AndroidBuild.MODEL }
                        // Divider between content and info
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 4.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
                        Spacer(Modifier.height(8.dp))

                        if (showModel || addressInfo.isNotEmpty() || weatherInfo.isNotEmpty()) {
                            if (showModel) Text(displayModel, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            if (addressInfo.isNotEmpty()) Text(addressInfo, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            if (weatherInfo.isNotEmpty()) Text(weatherInfo, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }

                        if (currentNote != null) {
                            val note = currentNote!!
                            val timeF = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
                            Spacer(Modifier.height(8.dp))
                            Text("创建: ${timeF.format(Date(note.createdAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Text("修改: ${timeF.format(Date(note.updatedAt))} (${note.modifyCount}次)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }

                    // Audio blocks
                    if (audioList.isNotEmpty()) {
                        item { Spacer(Modifier.height(12.dp)) }
                        audioList.forEachIndexed { i, a ->
                            item {
                                AudioBar(
                                    src = a.src, durationMs = a.durationMs,
                                    isPlaying = playingPath == a.src,
                                    position = if (playingPath == a.src) playbackPos else 0,
                                    duration = if (playingPath == a.src) playbackDur else (a.durationMs / 1000).toInt(),
                                    onTogglePlay = { togglePlayback(a.src) },
                                    onDelete = {
                                        stopPlaybackInternal()
                                        try { File(a.src).delete() } catch (_: Exception) {}
                                        audioList.removeAt(i)
                                    },
                                    showDelete = true
                                )
                            }
                        }
                    }

                    // Image thumbnails
                    if (imageList.isNotEmpty()) {
                        item { Spacer(Modifier.height(12.dp)) }
                        item {
                            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(imageList.size, key = { imageList[it].src }) { idx ->
                                    Box(modifier = Modifier.height(80.dp)) {
                                        AsyncImage(model = File(imageList[idx].src), contentDescription = "图片",
                                            modifier = Modifier.height(80.dp).width(80.dp).clip(RoundedCornerShape(10.dp)).clickable { fullScreenImage = imageList[idx] },
                                            contentScale = ContentScale.Crop)
                                        Box(
                                            modifier = Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).clickable { imageList.removeAt(idx) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Close, "删除", tint = Color.White, modifier = Modifier.size(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom toolbar
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Image, "相册", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = {
                                    val f = File(ImageUtils.getImageDir(context), "capture_${System.currentTimeMillis()}.jpg")
                                    f.parentFile?.mkdirs(); cameraTempFile = f
                                    cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
                                }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.CameraAlt, "拍照", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = { if (isRecording) endRecording() else beginRecording() }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Mic, "录音",
                                        tint = if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${charCount}字", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Full-screen image with zoom
    fullScreenImage?.let { img ->
        Dialog(onDismissRequest = { fullScreenImage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = File(img.src), contentDescription = img.alt,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentScale = ContentScale.Fit
                )
                IconButton(onClick = { fullScreenImage = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, "关闭", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

// ── Shared audio bar (edit + preview) ──

@Composable
private fun AudioBar(
    src: String, durationMs: Long, isPlaying: Boolean, position: Int, duration: Int,
    onTogglePlay: () -> Unit, onDelete: (() -> Unit)? = null, showDelete: Boolean = false
) {
    val d: Int = if (duration > 0) duration else (durationMs / 1000).toInt()
    val p = if (d > 0) position.toFloat() / d.toFloat() else 0f

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onTogglePlay, modifier = Modifier.size(32.dp)) {
            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                if (isPlaying) "暂停" else "播放", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
            Box(Modifier.fillMaxWidth(p).height(4.dp).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.width(8.dp))
        Text(if (isPlaying) formatDuration(position) else formatDuration(d),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, fontFamily = FontFamily.Monospace)
        if (showDelete && onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "删除", tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Preview composables ──

@Composable
private fun PreviewHeading(block: MarkdownBlock.Heading) {
    val s = when (block.level) { 1 -> 22.sp; 2 -> 18.sp; else -> 16.sp }
    Text(parseInline(block.rawText), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = s, color = MaterialTheme.colorScheme.onSurface, lineHeight = (s.value * 1.4f).sp))
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PreviewParagraph(block: MarkdownBlock.Paragraph) {
    Text(parseInline(block.rawText), style = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurface))
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PreviewImage(block: MarkdownBlock.Image) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        AsyncImage(model = File(block.src), contentDescription = block.alt,
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.FillWidth)
    }
}

@Composable
private fun PreviewCodeBlock(block: MarkdownBlock.CodeBlock) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(block.code, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(12.dp))
    }
}

@Composable
private fun PreviewQuote(block: MarkdownBlock.Quote) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Box(Modifier.width(3.dp).height(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
        Spacer(Modifier.width(10.dp))
        Text(parseInline(block.rawText), style = TextStyle(fontSize = 15.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.outline))
    }
}

@Composable
private fun PreviewListItem(block: MarkdownBlock.ListItem) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Spacer(Modifier.width(8.dp))
        Text("  •  ", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
        Text(parseInline(block.rawText), style = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface))
    }
}

private fun getFileExtension(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && i >= 0) cursor.getString(i)?.substringAfterLast('.', "") else null
    }
}
