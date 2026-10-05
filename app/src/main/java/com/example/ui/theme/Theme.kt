package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.core.model.ThemePreference

val THPDarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = ObsidianDark,
    primaryContainer = Emerald700,
    onPrimaryContainer = Emerald100,

    secondary = Indigo400,
    onSecondary = ObsidianDark,
    secondaryContainer = Indigo700,
    onSecondaryContainer = Indigo100,

    tertiary = Cyan400,
    onTertiary = ObsidianDark,
    tertiaryContainer = Cyan600,
    onTertiaryContainer = Cyan100,

    background = ObsidianDark,
    onBackground = Color(0xFFF1F5F9),

    surface = SlateSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateCardDark,
    onSurfaceVariant = Color(0xFF94A3B8),

    outline = SlateBorderDark,
    outlineVariant = Color(0xFF1E293B),

    error = CoralRose,
    onError = Color.White
)

val THPLightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald700,

    secondary = Indigo600,
    onSecondary = Color.White,
    secondaryContainer = Indigo100,
    onSecondaryContainer = Indigo700,

    tertiary = Cyan600,
    onTertiary = Color.White,
    tertiaryContainer = Cyan100,
    onTertiaryContainer = Cyan600,

    background = CrispLight,
    onBackground = Color(0xFF0F172A),

    surface = CrispSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = CrispCardLight,
    onSurfaceVariant = Color(0xFF475569),

    outline = CrispBorderLight,
    outlineVariant = Color(0xFFE2E8F0),

    error = CoralRose,
    onError = Color.White
)

@Composable
fun THPTrackerTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemDark
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> THPDarkColorScheme
        else -> THPLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
