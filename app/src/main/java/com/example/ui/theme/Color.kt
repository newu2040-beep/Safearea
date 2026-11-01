package com.example.ui.theme

import androidx.compose.ui.graphics.Color

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
    )
}

enum class AppearanceMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
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
    val isDark: Boolean
)

fun resolveSafeAreaPalette(theme: VaultThemeId, effectiveDark: Boolean): SafeAreaPalette {
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
            isDark = true
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
            isDark = false
        )
    }
}
