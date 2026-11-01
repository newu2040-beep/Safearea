package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WorkOutline
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentCategoryGroup
import com.example.ui.SafeAreaScreen
import com.example.ui.theme.LocalSafeAreaPalette
import java.util.Locale

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

        // Green accent ribbon curve underneath bottom-right of shield
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
 * Custom vector illustration for Welcome / Onboarding screen.
 */
@Composable
fun WelcomeDocumentsHeroIllustration(
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(270.dp, 180.dp)) {
            val w = size.width
            val h = size.height

            drawOval(
                color = palette.ink.copy(alpha = 0.07f),
                topLeft = Offset(w * 0.12f, h * 0.84f),
                size = Size(w * 0.76f, h * 0.12f)
            )

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
            drawSparkle(w * 0.10f, h * 0.12f, 10f)
            drawSparkle(w * 0.22f, h * 0.07f, 6f)
            drawSparkle(w * 0.85f, h * 0.12f, 11f)

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
                drawRoundRect(
                    color = Color(0xFFD6E4F5),
                    topLeft = Offset(cardLeft + 10f, cardTop + 10f),
                    size = Size(cardW - 20f, cardH * 0.25f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
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
                for (i in 0..2) {
                    drawRoundRect(
                        color = Color(0xFFB8C2CC),
                        topLeft = Offset(cardLeft + cardW * 0.42f, cardTop + cardH * (0.42f + i * 0.15f)),
                        size = Size(cardW * (0.48f - i * 0.08f), 8f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

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
                drawRoundRect(
                    color = Color(0xFFD5EAE0),
                    topLeft = Offset(cardLeft + 10f, cardTop + 10f),
                    size = Size(cardW - 20f, cardH * 0.24f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
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
 * Custom Manila Folder with Padlock illustration.
 */
@Composable
fun FolderWithLockIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp, 90.dp)) {
            val w = size.width
            val h = size.height

            val rayColor = Color(0xFF9AC5AF)
            drawLine(rayColor, Offset(w * 0.08f, h * 0.22f), Offset(w * 0.18f, h * 0.30f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(rayColor, Offset(w * 0.05f, h * 0.42f), Offset(w * 0.16f, h * 0.44f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(rayColor, Offset(w * 0.92f, h * 0.22f), Offset(w * 0.82f, h * 0.30f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(rayColor, Offset(w * 0.95f, h * 0.42f), Offset(w * 0.84f, h * 0.44f), strokeWidth = 6f, cap = StrokeCap.Round)

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
            drawRoundRect(
                color = Color(0xFFFFFDF7),
                topLeft = Offset(fLeft + 10f, fTop + 8f),
                size = Size(fW - 20f, fH * 0.4f),
                cornerRadius = CornerRadius(8f, 8f)
            )
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
 * Rounded-square top navigation icon button (48dp touch target).
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
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = palette.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.65f))
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
 * Responsive primary pill CTA button.
 * Uses `heightIn(min = 50.dp)` instead of fixed height so multi-line or scaled text never clips.
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
            .heightIn(min = 50.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        shape = RoundedCornerShape(palette.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.primary,
            contentColor = Color(0xFFFFFDF7),
            disabledContainerColor = palette.primary.copy(alpha = 0.45f),
            disabledContentColor = Color(0xFFFFFDF7).copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )
            if (trailingArrow) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "▶",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Responsive outlined pill button.
 * Uses `heightIn(min = 48.dp)` and adaptive padding so text never clips on compact screens or large font scales.
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
            .heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        shape = RoundedCornerShape(palette.buttonCornerRadius),
        border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = 0.8f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.surface.copy(alpha = 0.65f),
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
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                ),
                color = palette.ink,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Maps each of the 15 `DocumentCategoryGroup` entries to a distinctive vector icon.
 */
fun categoryGroupIcon(group: DocumentCategoryGroup): ImageVector {
    return when (group) {
        DocumentCategoryGroup.PERSONAL -> Icons.Default.Badge
        DocumentCategoryGroup.EDUCATION -> Icons.Default.School
        DocumentCategoryGroup.FINANCIAL -> Icons.Default.AccountBalance
        DocumentCategoryGroup.TAX_ACCOUNTING -> Icons.Default.ReceiptLong
        DocumentCategoryGroup.MEDICAL -> Icons.Default.HealthAndSafety
        DocumentCategoryGroup.INSURANCE -> Icons.Default.Shield
        DocumentCategoryGroup.EMPLOYMENT -> Icons.Default.WorkOutline
        DocumentCategoryGroup.BUSINESS -> Icons.Default.BusinessCenter
        DocumentCategoryGroup.LEGAL -> Icons.Default.Gavel
        DocumentCategoryGroup.PROPERTY -> Icons.Default.Apartment
        DocumentCategoryGroup.TRAVEL -> Icons.Default.FlightTakeoff
        DocumentCategoryGroup.VEHICLE -> Icons.Default.DirectionsCar
        DocumentCategoryGroup.HOME_UTILITIES -> Icons.Default.HomeRepairService
        DocumentCategoryGroup.DIGITAL_LICENSES -> Icons.Default.VpnKey
        DocumentCategoryGroup.OTHER -> Icons.Default.Description
    }
}

/**
 * Document category icon badge for all 15 category groups.
 */
@Composable
fun DocumentCategoryIconBox(
    categoryGroup: DocumentCategoryGroup,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val iconVector = categoryGroupIcon(categoryGroup)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(categoryGroup.badgeHex)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = categoryGroup.title,
            tint = Color(categoryGroup.iconTintHex),
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/**
 * Format & Size helper used across cards.
 */
fun formatCompactFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

/**
 * Apple-inspired responsive Document Row Card supporting:
 * - Category icon badge (15 categories)
 * - Format badge (PDF, DOCX, XLSX, JPG, etc.)
 * - File size & subtype
 * - Expiry status badge
 * - Favourite indicator
 * - Multi-select mode checkbox & long-press bulk selection
 * - Customizable corner radius, density, and border contrast
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SafeAreaDocumentRowCard(
    title: String,
    subtitle: String,
    categoryGroup: DocumentCategoryGroup,
    formatBadge: String? = null,
    fileSizeText: String? = null,
    expiryBadgeText: String? = null,
    isExpiringSoon: Boolean = false,
    isFavourite: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalSafeAreaPalette.current
    val cardShape = RoundedCornerShape(palette.cardCornerRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = cardShape,
        color = if (isSelected) palette.primary.copy(alpha = 0.10f) else palette.surface,
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else palette.borderWidth,
            color = if (isSelected) palette.primary else palette.border.copy(alpha = palette.borderAlpha)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = palette.cardVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) palette.primary else palette.inkMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            DocumentCategoryIconBox(categoryGroup = categoryGroup)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavourite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favourite",
                            tint = Color(0xFFE09F1F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!formatBadge.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = palette.canvas,
                            border = BorderStroke(0.8.dp, palette.border.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = formatBadge,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.ink,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!fileSizeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = palette.canvas
                        ) {
                            Text(
                                text = fileSizeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.inkMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!expiryBadgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isExpiringSoon) palette.warningContainer else palette.successContainer
                        ) {
                            Text(
                                text = expiryBadgeText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isExpiringSoon) Color(0xFF8A5A00) else palette.success,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = palette.inkMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Responsive 5-destination Bottom Navigation Bar.
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
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentScreen == item.screen
                val tint = if (isSelected) palette.primary else palette.ink
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigate(item.screen) }
                        .padding(vertical = 6.dp)
                        .testTag("nav_${item.label.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
