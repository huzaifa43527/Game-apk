package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IndigoColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    secondary = IndigoSecondary,
    onSecondary = Color.Black,
    tertiary = IndigoTertiary,
    onTertiary = Color.Black,
    background = IndigoDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = IndigoDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val CyberColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = Color.Black,
    secondary = CyberSecondary,
    onSecondary = Color.White,
    tertiary = CyberTertiary,
    onTertiary = Color.Black,
    background = CyberDarkBg,
    onBackground = Color(0xFFF0FDF4),
    surface = CyberDarkSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E2235),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    secondary = EmeraldSecondary,
    onSecondary = Color.Black,
    tertiary = EmeraldTertiary,
    onTertiary = Color.Black,
    background = EmeraldDarkBg,
    onBackground = Color(0xFFECFDF5),
    surface = EmeraldDarkSurface,
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = Color(0xFF1B432C),
    onSurfaceVariant = Color(0xFFA7F3D0)
)

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color.Black,
    secondary = SunsetSecondary,
    onSecondary = Color.Black,
    tertiary = SunsetTertiary,
    onTertiary = Color.White,
    background = SunsetDarkBg,
    onBackground = Color(0xFFFFF7ED),
    surface = SunsetDarkSurface,
    onSurface = Color(0xFFFFEDD5),
    surfaceVariant = Color(0xFF43281C),
    onSurfaceVariant = Color(0xFFFED7AA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    themeName: String = "INDIGO",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName.uppercase()) {
        "CYBERPUNK" -> CyberColorScheme
        "EMERALD" -> EmeraldColorScheme
        "SUNSET" -> SunsetColorScheme
        else -> IndigoColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

