package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkNavyPrimary,
    onPrimary = DarkOnNavyPrimary,
    primaryContainer = DarkNavyPrimaryContainer,
    onPrimaryContainer = DarkOnNavyPrimaryContainer,
    secondary = DarkAzureSecondary,
    onSecondary = DarkOnAzureSecondary,
    secondaryContainer = DarkAzureSecondaryContainer,
    onSecondaryContainer = DarkOnAzureSecondaryContainer,
    tertiary = DarkSlateTertiary,
    onTertiary = DarkOnSlateTertiary,
    tertiaryContainer = DarkSlateTertiaryContainer,
    onTertiaryContainer = DarkOnSlateTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = VaultErrorDark,
    errorContainer = VaultErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = OnNavyPrimary,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = OnNavyPrimaryContainer,
    secondary = AzureSecondary,
    onSecondary = OnAzureSecondary,
    secondaryContainer = AzureSecondaryContainer,
    onSecondaryContainer = OnAzureSecondaryContainer,
    tertiary = SlateTertiary,
    onTertiary = OnSlateTertiary,
    tertiaryContainer = SlateTertiaryContainer,
    onTertiaryContainer = OnSlateTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = VaultErrorLight,
    errorContainer = VaultErrorContainerLight
)

val VaultShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NotifVaultTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = VaultShapes,
        content = content
    )
}
