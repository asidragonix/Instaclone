package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val InstaDarkColorScheme = darkColorScheme(
    primary = InstaCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4B1126),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = InstaMagenta,
    onSecondary = Color.White,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = OnObsidianPrimary,
    tertiary = InstaAmber,
    onTertiary = Color.White,
    background = ObsidianBackground,
    onBackground = OnObsidianPrimary,
    surface = ObsidianSurface,
    onSurface = OnObsidianPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = OnObsidianSecondary,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF252136)
)

private val InstaLightColorScheme = lightColorScheme(
    primary = InstaCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5EC),
    onPrimaryContainer = Color(0xFF3E0014),
    secondary = InstaMagenta,
    onSecondary = Color.White,
    secondaryContainer = DaylightSurfaceVariant,
    onSecondaryContainer = OnDaylightPrimary,
    tertiary = InstaAmber,
    onTertiary = Color.White,
    background = DaylightBackground,
    onBackground = OnDaylightPrimary,
    surface = DaylightSurface,
    onSurface = OnDaylightPrimary,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = OnDaylightSecondary,
    outline = DaylightOutline,
    outlineVariant = Color(0xFFE7E3F3)
)

val InstaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun InstaCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) InstaDarkColorScheme else InstaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = InstaShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    InstaCloneTheme(darkTheme = darkTheme, content = content)
}
