package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentIconType
import com.example.ui.SafeAreaScreen
import com.example.ui.theme.LocalSafeAreaPalette

/**
 * Custom Canvas reproduction of the SAFEAREA Shield + Lock Mark with soft green bottom-right swoosh.
 */
@Composable
fun SafeAreaShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    val palette = LocalSafeAreaPalette.current
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Green accent ribbon curve underneath bottom-right of shield (from mockup)
        val greenPath = Path().apply {
            moveTo(w * 0.44f, h * 0.90f)
            cubicTo(
                w * 0.68f, h * 0.86f,
                w * 0.86f, h * 0.68f,
                w * 0.90f, h * 0.46f
            )
            lineTo(w * 0.94f, h * 0.52f)
            cubicTo(
                w * 0.88f, h * 0.78f,
                w * 0.68f, h * 0.94f,
                w * 0.48f, h * 0.98f
            )
            close()
        }
        drawPath(path = greenPath, color = Color(0xFF438B68))

        // Main Navy Shield
        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.06f)
            lineTo(w * 0.14f, h * 0.20f)
            lineTo(w * 0.14f, h * 0.48f)
            cubicTo(
                w * 0.14f, h * 0.72f,
                w * 0.30f, h * 0.86f,
                w * 0.50f, h * 0.92f
            )
            cubicTo(
                w * 0.70f, h * 0.86f,
                w * 0.86f, h * 0.72f,
                w * 0.86f, h * 0.48f
            )
            lineTo(w * 0.86f, h * 0.20f)
            close()
        }
        drawPath(
            path = shieldPath,
            color = if (palette.isDark) Color(0xFF2B2852) else Color(0xFF171431)
        )

        // Cream Padlock Shackle
        val lockColor = Color(0xFFFFFDF7)
        drawArc(
            color = lockColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.37f, h * 0.25f),
            size = Size(w * 0.26f, h * 0.26f),
            style = Stroke(width = w * 0.065f, cap = StrokeCap.Round)
        )

        // Cream Padlock Body
        drawRoundRect(
            color = lockColor,
            topLeft = Offset(w * 0.31f, h * 0.40f),
            size = Size(w * 0.38f, h * 0.31f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )

        // Navy Keyhole inside padlock
        val keyholeColor = Color(0xFF171431)
        drawCircle(
            color = keyholeColor,
            radius = w * 0.038f,
            center = Offset(w * 0.50f, h * 0.52f)
        )
        drawRoundRect(
            color = keyholeColor,
            topLeft = Offset(w * 0.48f, h * 0.53f),
            size = Size(w * 0.04f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )
    }
}

/**
 * Custom vector illustration reproducing Screen 1 of the SAFEAREA mockup:
 * Tilted DRIVING LICENSE card, NATIONAL ID card, and dark navy PASSPORT booklet with green sparkles.
 */
@Composable
fun WelcomeDocumentsHeroIllustration(
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(290.dp, 200.dp)) {
            val w = size.width
            val h = size.height

            // Soft radial shadow under cards
            drawOval(
                color = palette.ink.copy(alpha = 0.07f),
                topLeft = Offset(w * 0.12f, h * 0.84f),
                size = Size(w * 0.76f, h * 0.12f)
            )

            // Four-point soft green sparkles (+) from mockup
            fun drawSparkle(cx: Float, cy: Float, r: Float) {
                val p = Path().apply {
                    moveTo(cx, cy - r)
                    quadraticTo(cx, cy, cx + r, cy)
                    quadraticTo(cx, cy, cx, cy + r)
                    quadraticTo(cx, cy, cx - r, cy)
                    close()
                }
                drawPath(p, color = Color(0xFF78A88F))
            }
            drawSparkle(w * 0.10f, h * 0.12f, 11f)
            drawSparkle(w * 0.22f, h * 0.07f, 7f)
            drawSparkle(w * 0.85f, h * 0.12f, 12f)

            // 1. Top-left tilted DRIVING LICENSE card
            rotate(degrees = -7f, pivot = Offset(w * 0.34f, h * 0.36f)) {
                val cardLeft = w * 0.07f
                val cardTop = h * 0.17f
                val cardW = w * 0.52f
                val cardH = h * 0.37f
                drawRoundRect(
                    color = Color(0xFFF7F5EE),
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = Color(0xFF171431),
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = 3.2f)
                )
                // Blue header bar
                drawRoundRect(
                    color = Color(0xFFD6E4F5),
                    topLeft = Offset(cardLeft + 10f, cardTop + 10f),
                    size = Size(cardW - 20f, cardH * 0.25f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Avatar box
                drawRoundRect(
                    color = Color(0xFFDCE4EE),
                    topLeft = Offset(cardLeft + 14f, cardTop + cardH * 0.36f),
                    size = Size(cardW * 0.26f, cardH * 0.50f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawCircle(
                    color = Color(0xFF283556),
                    radius = cardW * 0.065f,
                    center = Offset(cardLeft + 14f + cardW * 0.13f, cardTop + cardH * 0.54f)
                )
                // Text lines
                for (i in 0..2) {
                    drawRoundRect(
                        color = Color(0xFFB8C2CC),
                        topLeft = Offset(cardLeft + cardW * 0.42f, cardTop + cardH * (0.42f + i * 0.15f)),
                        size = Size(cardW * (0.48f - i * 0.08f), 8f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // 2. Bottom-left tilted NATIONAL ID card
            rotate(degrees = -5f, pivot = Offset(w * 0.35f, h * 0.68f)) {
                val cardLeft = w * 0.09f
                val cardTop = h * 0.49f
                val cardW = w * 0.53f
                val cardH = h * 0.37f
                drawRoundRect(
                    color = Color(0xFFFBF9F3),
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = Color(0xFF171431),
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = 3.2f)
                )
                // Mint header bar
                drawRoundRect(
                    color = Color(0xFFD5EAE0),
                    topLeft = Offset(cardLeft + 10f, cardTop + 10f),
                    size = Size(cardW - 20f, cardH * 0.24f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Avatar box
                drawRoundRect(
                    color = Color(0xFFCDE5DB),
                    topLeft = Offset(cardLeft + 14f, cardTop + cardH * 0.36f),
                    size = Size(cardW * 0.26f, cardH * 0.50f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawCircle(
                    color = Color(0xFF171431),
                    radius = cardW * 0.065f,
                    center = Offset(cardLeft + 14f + cardW * 0.13f, cardTop + cardH * 0.54f)
                )
                for (i in 0..2) {
                    drawRoundRect(
                        color = Color(0xFFBCC6C2),
                        topLeft = Offset(cardLeft + cardW * 0.42f, cardTop + cardH * (0.42f + i * 0.15f)),
                        size = Size(cardW * (0.46f - i * 0.06f), 8f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // 3. Right tilted Navy PASSPORT booklet with Gold Globe emblem
            rotate(degrees = 10f, pivot = Offset(w * 0.72f, h * 0.55f)) {
                val passLeft = w * 0.54f
                val passTop = h * 0.26f
                val passW = w * 0.36f
                val passH = h * 0.56f
                drawRoundRect(
                    color = Color(0xFF1B1938),
                    topLeft = Offset(passLeft, passTop),
                    size = Size(passW, passH),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                // Gold globe circle
                val globeCenter = Offset(passLeft + passW * 0.5f, passTop + passH * 0.48f)
                val globeR = passW * 0.22f
                val goldColor = Color(0xFFE5C168)
                drawCircle(
                    color = goldColor,
                    radius = globeR,
                    center = globeCenter,
                    style = Stroke(width = 2.8f)
                )
                drawOval(
                    color = goldColor,
                    topLeft = Offset(globeCenter.x - globeR * 0.5f, globeCenter.y - globeR),
                    size = Size(globeR, globeR * 2f),
                    style = Stroke(width = 2.2f)
                )
                drawLine(
                    color = goldColor,
                    start = Offset(globeCenter.x - globeR, globeCenter.y),
                    end = Offset(globeCenter.x + globeR, globeCenter.y),
                    strokeWidth = 2.2f
                )
                // Top and bottom gold bars representing "PASSPORT"
                drawRoundRect(
                    color = goldColor,
                    topLeft = Offset(passLeft + passW * 0.24f, passTop + passH * 0.16f),
                    size = Size(passW * 0.52f, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = goldColor,
                    topLeft = Offset(passLeft + passW * 0.22f, passTop + passH * 0.78f),
                    size = Size(passW * 0.56f, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }
    }
}

/**
 * Custom illustration reproducing Screen 5 of the SAFEAREA mockup:
 * Manila Folder with Padlock and green rays.
 */
@Composable
fun FolderWithLockIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp, 100.dp)) {
            val w = size.width
            val h = size.height

            // Soft green rays on left and right
            val rayColor = Color(0xFF9AC5AF)
            drawLine(rayColor, Offset(w * 0.08f, h * 0.22f), Offset(w * 0.18f, h * 0.30f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(rayColor, Offset(w * 0.05f, h * 0.42f), Offset(w * 0.16f, h * 0.44f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(rayColor, Offset(w * 0.92f, h * 0.22f), Offset(w * 0.82f, h * 0.30f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(rayColor, Offset(w * 0.95f, h * 0.42f), Offset(w * 0.84f, h * 0.44f), strokeWidth = 6f, cap = StrokeCap.Round)

            // Back folder tab
            val fLeft = w * 0.24f
            val fTop = h * 0.20f
            val fW = w * 0.52f
            val fH = h * 0.66f
            drawRoundRect(
                color = Color(0xFFE6C994),
                topLeft = Offset(fLeft, fTop),
                size = Size(fW * 0.45f, fH * 0.32f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // White paper peeking out
            drawRoundRect(
                color = Color(0xFFFFFDF7),
                topLeft = Offset(fLeft + 10f, fTop + 8f),
                size = Size(fW - 20f, fH * 0.4f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Front Manila folder body
            drawRoundRect(
                color = Color(0xFFF3DCAC),
                topLeft = Offset(fLeft, fTop + fH * 0.18f),
                size = Size(fW, fH * 0.82f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = Color(0xFF171431),
                topLeft = Offset(fLeft, fTop + fH * 0.18f),
                size = Size(fW, fH * 0.82f),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3.5f)
            )

            // Navy Padlock in center of folder
            val lockCx = w * 0.50f
            val lockCy = fTop + fH * 0.62f
            drawArc(
                color = Color(0xFF171431),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(lockCx - 11f, lockCy - 24f),
                size = Size(22f, 22f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            drawRoundRect(
                color = Color(0xFF171431),
                topLeft = Offset(lockCx - 16f, lockCy - 12f),
                size = Size(32f, 26f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawCircle(
                color = Color(0xFFF3DCAC),
                radius = 3.5f,
                center = Offset(lockCx, lockCy)
            )
        }
    }
}

/**
 * Rounded-square top navigation icon button matching the mockup (`←` and `•••`).
 */
@Composable
fun SafeAreaTopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    Surface(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = palette.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.3.dp, palette.border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = palette.ink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SafeAreaBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SafeAreaTopIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        onClick = onClick,
        modifier = modifier.testTag("back_button")
    )
}

@Composable
fun SafeAreaMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SafeAreaTopIconButton(
        icon = Icons.Default.MoreHoriz,
        contentDescription = "More options",
        onClick = onClick,
        modifier = modifier.testTag("more_options_button")
    )
}

/**
 * Raspberry primary pill CTA button matching the mockup.
 */
@Composable
fun SafeAreaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingArrow: Boolean = false
) {
    val palette = LocalSafeAreaPalette.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.primary,
            contentColor = Color(0xFFFFFDF7),
            disabledContainerColor = palette.primary.copy(alpha = 0.45f),
            disabledContentColor = Color(0xFFFFFDF7).copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )
            if (trailingArrow) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "▶",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Outlined navy pill button matching `+ Add Document`, `View Full Image`, and `Share Document` in the mockup.
 */
@Composable
fun SafeAreaOutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val palette = LocalSafeAreaPalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.3.dp, palette.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.surface.copy(alpha = 0.55f),
            contentColor = palette.ink
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = palette.ink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.ink
            )
        }
    }
}

/**
 * Document category icon badge matching Screen 3 of the mockup.
 */
@Composable
fun DocumentCategoryIconBox(
    iconType: DocumentIconType,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val iconVector = when (iconType) {
        DocumentIconType.DRIVING -> Icons.Default.DirectionsCar
        DocumentIconType.CITIZENSHIP -> Icons.Default.Groups
        DocumentIconType.NATIONAL_ID -> Icons.Default.Badge
        DocumentIconType.PASSPORT -> Icons.Default.FlightTakeoff
        DocumentIconType.EDUCATION -> Icons.Default.School
        DocumentIconType.FINANCE -> Icons.Outlined.AccountBalance
        DocumentIconType.HEALTH -> Icons.Default.HealthAndSafety
        DocumentIconType.ADDRESS -> Icons.Default.Home
        DocumentIconType.WORK -> Icons.Default.WorkOutline
        DocumentIconType.OTHER -> Icons.Default.Description
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(iconType.badgeHex)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = iconType.label,
            tint = Color(iconType.iconTintHex),
            modifier = Modifier.size(size * 0.54f)
        )
    }
}

/**
 * Editorial Document Row Card matching Screen 3 of the SAFEAREA mockup.
 */
@Composable
fun SafeAreaDocumentRowCard(
    title: String,
    subtitle: String,
    iconType: DocumentIconType,
    expiryBadgeText: String? = null,
    isExpiringSoon: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.1.dp, palette.border.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentCategoryIconBox(iconType = iconType)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!expiryBadgeText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExpiringSoon) palette.warningContainer else palette.successContainer
                        ) {
                            Text(
                                text = expiryBadgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isExpiringSoon) Color(0xFF8A5A00) else palette.success,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = palette.ink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Compact 5-destination Bottom Navigation Bar matching the SAFEAREA mockup and Section 4 IA:
 * Home, Documents, Add, Tools, Profile.
 */
@Composable
fun SafeAreaBottomNavigationBar(
    currentScreen: SafeAreaScreen,
    onNavigate: (SafeAreaScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    val items = listOf(
        BottomNavItem("Home", SafeAreaScreen.HOME, Icons.Default.Home, Icons.Outlined.Home),
        BottomNavItem("Documents", SafeAreaScreen.DOCUMENTS, Icons.Default.Badge, Icons.Outlined.Folder),
        BottomNavItem("Add", SafeAreaScreen.ADD_DOCUMENT, Icons.Default.Add, Icons.Default.Add),
        BottomNavItem("Tools", SafeAreaScreen.TOOLS, Icons.Default.GridView, Icons.Outlined.GridView),
        BottomNavItem("Profile", SafeAreaScreen.PROFILE, Icons.Default.PersonOutline, Icons.Outlined.Person)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.canvas,
        border = BorderStroke(0.8.dp, palette.border.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentScreen == item.screen
                val tint = if (isSelected) palette.primary else palette.ink
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigate(item.screen) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("nav_${item.label.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = tint
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val screen: SafeAreaScreen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
