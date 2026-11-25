package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun VaultLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(primary, secondary),
                    start = Offset.Zero,
                    end = Offset(100f, 100f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = this.size.minDimension * 0.45f,
                center = Offset(this.size.width * 0.85f, this.size.height * 0.2f)
            )
        }
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.28f),
            modifier = Modifier.size(size * 0.68f)
        )
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

@Composable
fun AppIconAvatar(
    appName: String,
    iconDrawable: Drawable?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val bitmap = remember(iconDrawable) {
        iconDrawable?.let {
            runCatching { it.toBitmap(width = 96, height = 96).asImageBitmap() }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appName,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        val letter = appName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
