package com.g2link.connect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object G2Colors {
    // Brand colors
    val Brand        = Color(0xFF00C6FF)   // G2-Link cyan-blue
    val BrandDeep    = Color(0xFF0072FF)   // Deep blue
    val BrandGlow    = Color(0xFF00C6FF).copy(alpha = 0.15f)

    // Status
    val Connected    = Color(0xFF00E676)   // Bright green
    val Searching    = Color(0xFFFFD740)   // Amber
    val Disconnected = Color(0xFFFF1744)   // Red
    val Emergency    = Color(0xFFFF1744)

    // Messages
    val OutgoingBubble = Color(0xFF0072FF)
    val IncomingBubble = Color(0xFF1E2A3A)
    val BroadcastBg    = Color(0xFF6A0080)

    // Priority
    val PriorityFamily    = Color(0xFFFF4081)
    val PriorityEmergency = Color(0xFFFF1744)

    // Surfaces
    val Background     = Color(0xFF090E18)
    val Surface        = Color(0xFF101828)
    val SurfaceVariant = Color(0xFF1A2535)
    val OnSurface      = Color(0xFFE8F0FE)

    val Primary     = Brand
    val OnPrimary   = Color(0xFF000000)
}

private val DarkColorScheme = darkColorScheme(
    primary           = G2Colors.Brand,
    onPrimary         = G2Colors.OnPrimary,
    primaryContainer  = Color(0xFF003060),
    secondary         = G2Colors.Searching,
    error             = G2Colors.Emergency,
    background        = G2Colors.Background,
    surface           = G2Colors.Surface,
    surfaceVariant    = G2Colors.SurfaceVariant,
    onBackground      = G2Colors.OnSurface,
    onSurface         = G2Colors.OnSurface,
    onSurfaceVariant  = Color(0xFF8BA0BF)
)

@Composable
fun G2LinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme, // G2-Link is dark-first
        typography = Typography(),
        content = content
    )
}
