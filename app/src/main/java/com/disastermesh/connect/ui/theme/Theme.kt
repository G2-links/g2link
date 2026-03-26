package com.disastermesh.connect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Emergency-grade Color System ────────────────────────
// High contrast, readable under stress and outdoor light

object MeshColors {
    // Status indicators
    val Connected    = Color(0xFF00C853)   // Bright green
    val Searching    = Color(0xFFFFAB00)   // Amber
    val Disconnected = Color(0xFFFF1744)   // Red
    val Emergency    = Color(0xFFFF1744)   // Red

    // Message bubbles
    val OutgoingBubble = Color(0xFF1565C0)     // Deep blue
    val IncomingBubble = Color(0xFF263238)     // Dark blue-grey
    val BroadcastBg    = Color(0xFF7B1FA2)    // Purple - emergency

    // Priority indicators
    val PriorityFamily    = Color(0xFFE91E63)  // Pink
    val PriorityEmergency = Color(0xFFFF1744)  // Red

    // Background
    val Background     = Color(0xFF0D1117)    // Near black
    val Surface        = Color(0xFF161B22)    // Dark surface
    val SurfaceVariant = Color(0xFF1F2937)    // Slightly lighter
    val OnSurface      = Color(0xFFE6EDF3)    // Near white

    // Accent
    val Primary        = Color(0xFF2F81F7)    // GitHub blue
    val PrimaryVariant = Color(0xFF1F6FEB)
    val OnPrimary      = Color(0xFFFFFFFF)
}

private val DarkColorScheme = darkColorScheme(
    primary           = MeshColors.Primary,
    onPrimary         = MeshColors.OnPrimary,
    primaryContainer  = Color(0xFF1F3A5F),
    secondary         = MeshColors.Searching,
    error             = MeshColors.Emergency,
    background        = MeshColors.Background,
    surface           = MeshColors.Surface,
    surfaceVariant    = MeshColors.SurfaceVariant,
    onBackground      = MeshColors.OnSurface,
    onSurface         = MeshColors.OnSurface,
    onSurfaceVariant  = Color(0xFF8B949E)
)

// Light theme (also high contrast)
private val LightColorScheme = lightColorScheme(
    primary           = Color(0xFF0550AE),
    onPrimary         = Color(0xFFFFFFFF),
    primaryContainer  = Color(0xFFD3E4FF),
    secondary         = Color(0xFFFF8F00),
    error             = Color(0xFFBA1A1A),
    background        = Color(0xFFF6F8FA),
    surface           = Color(0xFFFFFFFF),
    surfaceVariant    = Color(0xFFF0F2F4),
    onBackground      = Color(0xFF1F2328),
    onSurface         = Color(0xFF1F2328)
)

@Composable
fun DisasterMeshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
