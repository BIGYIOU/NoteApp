package com.example.noteapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    bgColor: Color = Color(0xFF1C1C1E),
    bgAlpha: Float = 0.82f
): Modifier = this
    .clip(shape)
    .background(bgColor.copy(alpha = bgAlpha))
    .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    bgColor: Color = Color(0xFF1C1C1E),
    bgAlpha: Float = 0.82f,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.glass(shape, bgColor, bgAlpha)) {
        content()
    }
}
