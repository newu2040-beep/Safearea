package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSafeAreaPalette = staticCompositionLocalOf {
    resolveSafeAreaPalette(VaultThemeId.CREAM, effectiveDark = false)
}

@Composable
fun SafeAreaTheme(
    themeId: VaultThemeId = VaultThemeId.CREAM,
    appearanceMode: AppearanceMode = AppearanceMode.LIGHT,
    useSystemFont: Boolean = false,
    fontScale: Float = 1.0f,
    cornerRadiusStyle: CornerRadiusStyle = CornerRadiusStyle.APPLE_ROUNDED,
    cardDensityStyle: CardDensityStyle = CardDensityStyle.COMFORTABLE,
    borderContrastStyle: BorderContrastStyle = BorderContrastStyle.STANDARD,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> systemDark
    }
    val palette = resolveSafeAreaPalette(
        theme = themeId,
        effectiveDark = effectiveDark,
        cornerStyle = cornerRadiusStyle,
        densityStyle = cardDensityStyle,
        borderContrastStyle = borderContrastStyle
    )
    val typography = remember(useSystemFont, fontScale) {
        buildSafeAreaTypography(useSystemFont = useSystemFont, fontScale = fontScale)
    }

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.secondary,
            onSecondary = palette.ink,
            tertiary = palette.success,
            background = palette.canvas,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.inkMuted,
            outline = palette.border,
            error = palette.danger
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.secondary,
            onSecondary = palette.ink,
            tertiary = palette.success,
            background = palette.canvas,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.inkMuted,
            outline = palette.border,
            error = palette.danger
        )
    }

    CompositionLocalProvider(LocalSafeAreaPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
