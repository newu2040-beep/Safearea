package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class VaultThemeId(
    val displayName: String,
    val isDarkPreset: Boolean,
    val canvasHex: Long,
    val surfaceHex: Long,
    val inkHex: Long,
    val primaryHex: Long,
    val secondaryHex: Long,
    val borderHex: Long
) {
    CREAM(
        displayName = "Cream (Default)",
        isDarkPreset = false,
        canvasHex = 0xFFF1EADB,
        surfaceHex = 0xFFFFFDF7,
        inkHex = 0xFF171431,
        primaryHex = 0xFFD5104B,
        secondaryHex = 0xFF78A88F,
        borderHex = 0xFF171431
    ),
    APPLE_MINIMAL_SNOW(
        displayName = "Apple Minimal Snow",
        isDarkPreset = false,
        canvasHex = 0xFFF5F5F7,
        surfaceHex = 0xFFFFFFFF,
        inkHex = 0xFF1D1D1F,
        primaryHex = 0xFF0071E3,
        secondaryHex = 0xFF34C759,
        borderHex = 0xFF86868B
    ),
    MINT(
        displayName = "Mint",
        isDarkPreset = false,
        canvasHex = 0xFFEAF3EC,
        surfaceHex = 0xFFF9FCF9,
        inkHex = 0xFF152821,
        primaryHex = 0xFF347B62,
        secondaryHex = 0xFFB7D9C4,
        borderHex = 0xFF152821
    ),
    LAVENDER(
        displayName = "Lavender",
        isDarkPreset = false,
        canvasHex = 0xFFF0ECF7,
        surfaceHex = 0xFFFCF9FF,
        inkHex = 0xFF1F1930,
        primaryHex = 0xFF7562A6,
        secondaryHex = 0xFFC9BDE4,
        borderHex = 0xFF1F1930
    ),
    POWDER_BLUE(
        displayName = "Powder Blue",
        isDarkPreset = false,
        canvasHex = 0xFFEAF1F7,
        surfaceHex = 0xFFF8FBFF,
        inkHex = 0xFF162330,
        primaryHex = 0xFF456F9B,
        secondaryHex = 0xFFB9D2E8,
        borderHex = 0xFF162330
    ),
    NORDIC_FROST(
        displayName = "Nordic Frost",
        isDarkPreset = false,
        canvasHex = 0xFFECEFF4,
        surfaceHex = 0xFFF9FBFD,
        inkHex = 0xFF2E3440,
        primaryHex = 0xFF5E81AC,
        secondaryHex = 0xFF88C0D0,
        borderHex = 0xFF4C566A
    ),
    BLUSH(
        displayName = "Blush",
        isDarkPreset = false,
        canvasHex = 0xFFF8ECEB,
        surfaceHex = 0xFFFFF9F8,
        inkHex = 0xFF2B171B,
        primaryHex = 0xFFB94F68,
        secondaryHex = 0xFFE9C1C8,
        borderHex = 0xFF2B171B
    ),
    SAKURA_BLOSSOM(
        displayName = "Sakura Blossom",
        isDarkPreset = false,
        canvasHex = 0xFFFDF0F5,
        surfaceHex = 0xFFFFFBFC,
        inkHex = 0xFF2F1824,
        primaryHex = 0xFFD84A7E,
        secondaryHex = 0xFFF2B5CB,
        borderHex = 0xFF542D40
    ),
    BUTTER_LEMON(
        displayName = "Butter / Lemon",
        isDarkPreset = false,
        canvasHex = 0xFFF7F1DE,
        surfaceHex = 0xFFFFFDF6,
        inkHex = 0xFF282110,
        primaryHex = 0xFFA77A20,
        secondaryHex = 0xFFE9D58B,
        borderHex = 0xFF282110
    ),
    PEACH(
        displayName = "Peach",
        isDarkPreset = false,
        canvasHex = 0xFFF8EDE4,
        surfaceHex = 0xFFFFFAF6,
        inkHex = 0xFF2B1D16,
        primaryHex = 0xFFB86F4D,
        secondaryHex = 0xFFEBC6AE,
        borderHex = 0xFF2B1D16
    ),
    SAND(
        displayName = "Sand",
        isDarkPreset = false,
        canvasHex = 0xFFEEE8DC,
        surfaceHex = 0xFFFAF7F0,
        inkHex = 0xFF241F19,
        primaryHex = 0xFF806D55,
        secondaryHex = 0xFFCFC1A9,
        borderHex = 0xFF241F19
    ),
    OCEAN_BREEZE(
        displayName = "Ocean Breeze",
        isDarkPreset = false,
        canvasHex = 0xFFE6F4F6,
        surfaceHex = 0xFFF7FEFF,
        inkHex = 0xFF102A32,
        primaryHex = 0xFF00798C,
        secondaryHex = 0xFF72C3D0,
        borderHex = 0xFF1D4754
    ),
    GRAPHITE_DARK(
        displayName = "Graphite Dark",
        isDarkPreset = true,
        canvasHex = 0xFF171923,
        surfaceHex = 0xFF222534,
        inkHex = 0xFFF5F2EB,
        primaryHex = 0xFFE58AA4,
        secondaryHex = 0xFF91BCA6,
        borderHex = 0xFF4A4E69
    ),
    MIDNIGHT_DARK(
        displayName = "Midnight Dark",
        isDarkPreset = true,
        canvasHex = 0xFF141629,
        surfaceHex = 0xFF1E223D,
        inkHex = 0xFFF3F2FA,
        primaryHex = 0xFFA9A2F0,
        secondaryHex = 0xFF8CC7B0,
        borderHex = 0xFF484D7A
    ),
    OLED_PITCH_BLACK(
        displayName = "OLED Pitch Black",
        isDarkPreset = true,
        canvasHex = 0xFF000000,
        surfaceHex = 0xFF121216,
        inkHex = 0xFFF8F9FA,
        primaryHex = 0xFF30D158,
        secondaryHex = 0xFF0A84FF,
        borderHex = 0xFF2C2C34
    ),
    EMERALD_FOREST_DARK(
        displayName = "Emerald Forest",
        isDarkPreset = true,
        canvasHex = 0xFF0F1E19,
        surfaceHex = 0xFF172C24,
        inkHex = 0xFFEBF7F2,
        primaryHex = 0xFF4BD09A,
        secondaryHex = 0xFF8BD8B7,
        borderHex = 0xFF2E5446
    ),
    ROYAL_GOLD_DARK(
        displayName = "Royal Gold",
        isDarkPreset = true,
        canvasHex = 0xFF181512,
        surfaceHex = 0xFF241F1A,
        inkHex = 0xFFFAF3E8,
        primaryHex = 0xFFE5B84B,
        secondaryHex = 0xFFD49B54,
        borderHex = 0xFF524433
    ),
    MOCHA_ESPRESSO_DARK(
        displayName = "Mocha Espresso",
        isDarkPreset = true,
        canvasHex = 0xFF1E1B18,
        surfaceHex = 0xFF2B2623,
        inkHex = 0xFFF5EBE6,
        primaryHex = 0xFFE08E6D,
        secondaryHex = 0xFFB8A398,
        borderHex = 0xFF524640
    )
}

enum class AppearanceMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class CornerRadiusStyle(val label: String, val cardRadiusDp: Int, val buttonRadiusDp: Int) {
    SHARP("Sharp (8dp)", 8, 12),
    MODERN("Modern (14dp)", 14, 18),
    APPLE_ROUNDED("Apple (20dp)", 20, 26),
    PILL("Soft Pill (26dp)", 26, 30)
}

enum class CardDensityStyle(val label: String, val verticalPaddingDp: Int, val itemSpacingDp: Int) {
    COMPACT("Compact", 9, 8),
    COMFORTABLE("Comfortable", 13, 11),
    SPACIOUS("Spacious", 17, 15)
}

enum class BorderContrastStyle(val label: String, val borderAlpha: Float, val borderWidthDp: Float) {
    SUBTLE("Subtle", 0.16f, 0.8f),
    STANDARD("Standard", 0.34f, 1.1f),
    BOLD("High Contrast", 0.72f, 1.6f)
}

data class SafeAreaPalette(
    val canvas: Color,
    val surface: Color,
    val ink: Color,
    val inkMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val border: Color,
    val isDark: Boolean,
    val cornerStyle: CornerRadiusStyle = CornerRadiusStyle.APPLE_ROUNDED,
    val densityStyle: CardDensityStyle = CardDensityStyle.COMFORTABLE,
    val borderContrastStyle: BorderContrastStyle = BorderContrastStyle.STANDARD
) {
    val cardCornerRadius: Dp
        get() = cornerStyle.cardRadiusDp.dp

    val buttonCornerRadius: Dp
        get() = cornerStyle.buttonRadiusDp.dp

    val cardVerticalPadding: Dp
        get() = densityStyle.verticalPaddingDp.dp

    val itemSpacing: Dp
        get() = densityStyle.itemSpacingDp.dp

    val borderAlpha: Float
        get() = borderContrastStyle.borderAlpha

    val borderWidth: Dp
        get() = borderContrastStyle.borderWidthDp.dp
}

fun resolveSafeAreaPalette(
    theme: VaultThemeId,
    effectiveDark: Boolean,
    cornerStyle: CornerRadiusStyle = CornerRadiusStyle.APPLE_ROUNDED,
    densityStyle: CardDensityStyle = CardDensityStyle.COMFORTABLE,
    borderContrastStyle: BorderContrastStyle = BorderContrastStyle.STANDARD
): SafeAreaPalette {
    val useDark = effectiveDark || theme.isDarkPreset
    return if (useDark) {
        val baseTheme = if (theme.isDarkPreset) theme else VaultThemeId.GRAPHITE_DARK
        val primaryColor = if (theme.isDarkPreset) Color(theme.primaryHex) else Color(0xFFE58AA4)
        val secondaryColor = if (theme.isDarkPreset) Color(theme.secondaryHex) else Color(0xFF91BCA6)
        SafeAreaPalette(
            canvas = Color(baseTheme.canvasHex),
            surface = Color(baseTheme.surfaceHex),
            ink = Color(baseTheme.inkHex),
            inkMuted = Color(baseTheme.inkHex).copy(alpha = 0.72f),
            primary = primaryColor,
            onPrimary = Color(0xFF171431),
            secondary = secondaryColor,
            success = Color(0xFF68B692),
            successContainer = Color(0xFF1F3B30),
            onSuccessContainer = Color(0xFFD7F2E4),
            warning = Color(0xFFF3B73E),
            warningContainer = Color(0xFF3E3218),
            danger = Color(0xFFFF6B81),
            border = Color(baseTheme.borderHex),
            isDark = true,
            cornerStyle = cornerStyle,
            densityStyle = densityStyle,
            borderContrastStyle = borderContrastStyle
        )
    } else {
        SafeAreaPalette(
            canvas = Color(theme.canvasHex),
            surface = Color(theme.surfaceHex),
            ink = Color(theme.inkHex),
            inkMuted = Color(theme.inkHex).copy(alpha = 0.68f),
            primary = Color(theme.primaryHex),
            onPrimary = Color(0xFFFFFDF7),
            secondary = Color(theme.secondaryHex),
            success = Color(0xFF438B68),
            successContainer = Color(0xFFDCECE2),
            onSuccessContainer = Color(0xFF171431),
            warning = Color(0xFFF3B73E),
            warningContainer = Color(0xFFFEF3D6),
            danger = Color(0xFFC91D42),
            border = Color(theme.borderHex),
            isDark = false,
            cornerStyle = cornerStyle,
            densityStyle = densityStyle,
            borderContrastStyle = borderContrastStyle
        )
    }
}
