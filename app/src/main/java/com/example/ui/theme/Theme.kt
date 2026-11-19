package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val NaqshLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = ElegantWhite,
    primaryContainer = EmeraldLightTint,
    onPrimaryContainer = DeepEmerald,
    secondary = ChampagneGoldDark,
    onSecondary = ElegantWhite,
    secondaryContainer = ChampagneGoldLight,
    onSecondaryContainer = DeepEmerald,
    tertiary = EmeraldMedium,
    onTertiary = ElegantWhite,
    background = WarmIvory,
    onBackground = InkDark,
    surface = ElegantWhite,
    onSurface = InkDark,
    surfaceVariant = IvorySurface,
    onSurfaceVariant = InkMuted,
    outline = OutlineIvory,
    error = LossCrimson,
    onError = ElegantWhite
)

private val NaqshDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = DeepEmerald,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = ChampagneGoldLight,
    secondary = ChampagneGoldLight,
    onSecondary = DeepEmerald,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = ChampagneGoldLight,
    tertiary = EmeraldMedium,
    onTertiary = ElegantWhite,
    background = DarkSurfaceBase,
    onBackground = WarmIvory,
    surface = DarkSurfaceCard,
    onSurface = WarmIvory,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFB2C7C0),
    outline = DarkOutline,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

val NaqshShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NaqshCollectionTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NaqshDarkColorScheme else NaqshLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NaqshTypography,
        shapes = NaqshShapes,
        content = content
    )
}
