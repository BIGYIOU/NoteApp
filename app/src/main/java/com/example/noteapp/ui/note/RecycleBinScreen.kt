package com.example.noteapp.ui.note

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noteapp.data.model.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecycleBinScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val notes by viewModel.deletedNotes.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("回收站", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (notes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("回收站为空", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    DeletedNoteCard(
                        note = note,
                        onRestore = { viewModel.restoreNote(note.id) },
                        onPermanentDelete = { viewModel.permanentDeleteNote(note.id, note.content) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DeletedNoteCard(
    note: Note,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirm by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }

    val density = LocalDensity.current
    val buttonWidthDp = 120.dp
    val buttonWidthPx = with(density) { buttonWidthDp.toPx() }
    val midPx = buttonWidthPx * 0.5f
    val offsetX = remember { Animatable(0f) }
    val animSpec = remember { spring<Float>(stiffness = 600f) }
    val scope = rememberCoroutineScope()
    var cardHeightPx by remember { mutableIntStateOf(0) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("彻底删除") },
            text = { Text("彻底删除后将无法恢复，确定删除？") },
            confirmButton = {
                TextButton(onClick = { onPermanentDelete(); showConfirm = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            }
        )
    }

    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        if (cardHeightPx > 0) {
            val actionHeightDp = with(density) { cardHeightPx.toDp() }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(actionHeightDp)
                    .offset { IntOffset((buttonWidthPx + offsetX.value).roundToInt(), 0) },
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End)
            ) {
                ActionButton(
                    icon = Icons.Default.Restore, label = "恢复",
                    color = Color(0xFF4CAF50)
                ) { scope.launch { offsetX.animateTo(0f, animSpec) }; onRestore() }
                ActionButton(
                    icon = Icons.Default.DeleteForever, label = "删除",
                    color = Color(0xFFE53935)
                ) { scope.launch { offsetX.animateTo(0f, animSpec) }; showConfirm = true }
            }
        }

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
                    if (offsetX.value < 0f) { scope.launch { offsetX.animateTo(0f, animSpec) } }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    note.title.ifBlank { "无标题" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = note.content.take(100).replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateFmt.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

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
