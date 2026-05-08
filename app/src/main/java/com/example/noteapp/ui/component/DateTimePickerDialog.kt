package com.example.noteapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialTime: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    showQuickSelect: Boolean = false
) {
    val cal = remember { Calendar.getInstance().apply { timeInMillis = initialTime } }
    val now = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }
    var second by remember { mutableIntStateOf(cal.get(Calendar.SECOND)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dateTimeFmt = remember { SimpleDateFormat("yyyy年MM月dd日 HH:mm EEEE", Locale.CHINESE) }
    val todayStr = remember { dateTimeFmt.format(Date()) }

    val daysInMonth = remember(year, month) {
        val c = Calendar.getInstance()
        c.set(year, month - 1, 1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    if (day > daysInMonth) day = daysInMonth

    val selectedStr = remember(year, month, day, hour, minute) {
        val c = Calendar.getInstance().apply { set(year, month - 1, day, hour, minute) }
        dateTimeFmt.format(c.time)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "设定日期",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
            )
            Text(
                "当前：$todayStr",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                "设定：$selectedStr",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            if (showQuickSelect) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30L to "30秒后", 60L to "1分钟后", 1800L to "30分钟后", 3600L to "60分钟后").forEach { (secs, label) ->
                        Text(
                            label,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable {
                                    onConfirm(System.currentTimeMillis() + secs * 1000L)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
            }
            Spacer(Modifier.height(12.dp))

            // Wheel columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelColumn(
                    value = year,
                    range = (now.get(Calendar.YEAR) - 10)..(now.get(Calendar.YEAR) + 10),
                    onValueChange = { year = it },
                    label = "年",
                    cycling = false
                )
                WheelColumn(
                    value = month,
                    range = 1..12,
                    onValueChange = { month = it },
                    label = "月",
                    cycling = true
                )
                WheelColumn(
                    value = day,
                    range = 1..daysInMonth,
                    onValueChange = { day = it },
                    label = "日",
                    cycling = true
                )
                WheelColumn(
                    value = hour,
                    range = 0..23,
                    onValueChange = { hour = it },
                    label = "时",
                    cycling = true
                )
                WheelColumn(
                    value = minute,
                    range = 0..59,
                    onValueChange = { minute = it },
                    label = "分",
                    cycling = true
                )
                WheelColumn(
                    value = second,
                    range = 0..59,
                    onValueChange = { second = it },
                    label = "秒",
                    cycling = true
                )
            }

            Spacer(Modifier.height(16.dp))

            // Bottom buttons
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        set(year, month - 1, day, hour, minute, second)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(c.timeInMillis)
                }, modifier = Modifier.weight(1f)) {
                    Text("确定", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    label: String,
    cycling: Boolean
) {
    val items = range.toList()
    val initialIndex = items.indexOf(value).coerceAtLeast(0)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
        WheelScroller(
            items = items,
            initialIndex = initialIndex,
            label = label,
            cycling = cycling,
            onIndexChange = { onValueChange(items[it]) }
        )
    }
}

@Composable
private fun WheelScroller(
    items: List<Int>,
    initialIndex: Int,
    label: String,
    cycling: Boolean,
    onIndexChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    val itemHeightDp = 32.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val visibleCount = 5

    // Float state for smooth finger tracking
    var currentIndex by remember { mutableIntStateOf(initialIndex) }

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(itemHeightDp * visibleCount)
            .pointerInput(cycling, items.size) {
                var accumulated = 0f
                detectVerticalDragGestures(
                    onDragEnd = {
                        accumulated = 0f
                    }
                ) { _, dragAmount ->
                    accumulated += dragAmount
                    val steps = (accumulated / itemHeightPx).toInt()
                    if (steps != 0) {
                        accumulated -= steps * itemHeightPx
                        val raw = currentIndex - steps
                        currentIndex = if (cycling) {
                            ((raw % items.size) + items.size) % items.size
                        } else {
                            raw.coerceIn(0, items.lastIndex)
                        }
                        onIndexChange(currentIndex)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(itemHeightDp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
        )

        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val half = visibleCount / 2
            for (i in -half..half) {
                var idx = currentIndex + i
                if (cycling) idx = ((idx % items.size) + items.size) % items.size
                else if (idx !in items.indices) { Spacer(Modifier.height(itemHeightDp)); continue }
                val distance = kotlin.math.abs(i)
                val alpha = when (distance) { 0 -> 1f; 1 -> 0.45f; else -> 0.15f }
                val fontSize = when (distance) { 0 -> 18.sp; 1 -> 15.sp; else -> 13.sp }
                Row(
                    modifier = Modifier.height(itemHeightDp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = items[idx].toString().padStart(2, '0'),
                        fontSize = fontSize,
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )
                    if (distance == 0 && label != "年") {
                        Text(" $label", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}