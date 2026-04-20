package com.ordemservico.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryBlue = Color(0xFF1565C0)
private val SecondaryBlue = Color(0xFF42A5F5)
private val LightBackground = Color(0xFFF5F7FA)
private val SurfaceColor = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    background = LightBackground,
    surface = SurfaceColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun OrdemServicoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

// Status colors
val StatusPending = Color(0xFFFF9800)
val StatusApproved = Color(0xFF4CAF50)
val StatusInProgress = Color(0xFF2196F3)
val StatusCompleted = Color(0xFF9C27B0)
val StatusCancelled = Color(0xFFF44336)

fun statusColor(status: String): Color = when (status) {
    "pending" -> StatusPending
    "approved" -> StatusApproved
    "in_progress" -> StatusInProgress
    "completed" -> StatusCompleted
    "cancelled" -> StatusCancelled
    else -> Color.Gray
}
