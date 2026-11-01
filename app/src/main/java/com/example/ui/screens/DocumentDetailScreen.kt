package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CryptoStorageManager
import com.example.data.DocumentEntity
import com.example.data.DocumentFieldEntity
import com.example.data.DocumentWithFields
import com.example.data.FieldSource
import com.example.ui.components.SafeAreaBackButton
import com.example.ui.components.SafeAreaMoreButton
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaPrimaryButton
import com.example.ui.theme.LocalSafeAreaPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Screen 4 of the SAFEAREA 5-screen mockup: Document Details.
 */
@Composable
fun DocumentDetailScreen(
    docWithFields: DocumentWithFields?,
    cryptoStorage: CryptoStorageManager,
    onBack: () -> Unit,
    onEditDocument: (DocumentWithFields) -> Unit,
    onToggleFavourite: (DocumentEntity) -> Unit,
    onToggleArchive: (DocumentEntity) -> Unit,
    onRotateAttachment: (DocumentEntity, Boolean) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onConfirmOcrFields: (Long) -> Unit,
    onCopySingleField: (String, String) -> Unit,
    onCopyAllFields: (DocumentWithFields) -> Unit,
    onShareDocument: (DocumentWithFields, Boolean) -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    if (docWithFields == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.canvas),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Document not found", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Spacer(modifier = Modifier.height(12.dp))
                SafeAreaBackButton(onClick = onBack)
            }
        }
        return
    }

    val doc = docWithFields.document
    val fields = docWithFields.fields

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareWarning by remember { mutableStateOf(false) }
    var showFullViewer by remember { mutableStateOf(false) }
    var showBackSide by remember { mutableStateOf(false) }

    // Track which sensitive fields have been explicitly revealed by user
    val revealedFields = remember { mutableStateMapOf<Long, Boolean>() }

    // Load decrypted bitmap or PDF page if attached
    val activeFileRef = if (showBackSide && doc.backFileRef.isNotBlank()) doc.backFileRef else doc.frontFileRef
    var previewBitmap by remember(activeFileRef) { mutableStateOf<Bitmap?>(null) }
    var pdfPageCount by remember(activeFileRef) { mutableIntStateOf(1) }
    var currentPdfPage by remember(activeFileRef) { mutableIntStateOf(0) }

    LaunchedEffect(activeFileRef, currentPdfPage) {
        if (activeFileRef.isBlank()) {
            previewBitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            if (doc.mimeType.contains("pdf", ignoreCase = true)) {
                val pdfFile = cryptoStorage.writeDecryptedTempFileForPreviewOrShare(activeFileRef, "pdf")
                if (pdfFile != null && pdfFile.exists()) {
                    runCatching {
                        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                            PdfRenderer(pfd).use { renderer ->
                                pdfPageCount = renderer.pageCount.coerceAtLeast(1)
                                val safePage = currentPdfPage.coerceIn(0, pdfPageCount - 1)
                                renderer.openPage(safePage).use { page ->
                                    val bmp = Bitmap.createBitmap(
                                        (page.width * 2).coerceAtMost(1600),
                                        (page.height * 2).coerceAtMost(2200),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    bmp.eraseColor(android.graphics.Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    previewBitmap = bmp
                                }
                            }
                        }
                    }
                }
            } else {
                val bytes = cryptoStorage.readDecryptedFile(activeFileRef)
                if (bytes != null) {
                    previewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        }
    }

    val hasUnconfirmedOcr = fields.any { !it.userConfirmed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar matching Screen 4 of the mockup (`←` and `•••`)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeAreaBackButton(onClick = onBack)

                Box {
                    SafeAreaMoreButton(onClick = { showMenu = true })
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(palette.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Details & Fields", color = palette.ink) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = palette.ink) },
                            onClick = {
                                showMenu = false
                                onEditDocument(docWithFields)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (doc.favourite) "Remove from Favourites" else "Add to Favourites",
                                    color = palette.ink
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (doc.favourite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = palette.primary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavourite(doc)
                            }
                        )
                        if (activeFileRef.isNotBlank() && !doc.mimeType.contains("pdf")) {
                            DropdownMenuItem(
                                text = { Text("Rotate Image 90°", color = palette.ink) },
                                leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null, tint = palette.ink) },
                                onClick = {
                                    showMenu = false
                                    onRotateAttachment(doc, showBackSide)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (doc.archived) "Restore from Archive" else "Archive Document",
                                    color = palette.ink
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = palette.ink) },
                            onClick = {
                                showMenu = false
                                onToggleArchive(doc)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Document", color = palette.danger, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Preview Card (Real Decrypted Image/PDF or National ID Card Illustration from Screen 4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showFullViewer = true }
                    .testTag("document_preview_card")
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.surface,
                        border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.4f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Document preview for ${doc.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                            )
                            if (doc.mimeType.contains("pdf", ignoreCase = true) && pdfPageCount > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.surface)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        enabled = currentPdfPage > 0,
                                        onClick = { currentPdfPage-- }
                                    ) {
                                        Text("◀ Prev")
                                    }
                                    Text(
                                        text = "PDF Page ${currentPdfPage + 1} of $pdfPageCount",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.ink
                                    )
                                    TextButton(
                                        enabled = currentPdfPage < pdfPageCount - 1,
                                        onClick = { currentPdfPage++ }
                                    ) {
                                        Text("Next ▶")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    MockupNationalIdCardVisual(
                        document = doc,
                        holderName = fields.find { it.label.contains("Name", ignoreCase = true) }?.value ?: "Rahul Shah"
                    )
                }
            }

            // Front / Back toggle if two-sided ID
            if (doc.backFileRef.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!showBackSide) palette.ink else palette.surface,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.clickable { showBackSide = false }
                    ) {
                        Text(
                            text = "Front Side",
                            color = if (!showBackSide) palette.surface else palette.ink,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (showBackSide) palette.ink else palette.surface,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.clickable { showBackSide = true }
                    ) {
                        Text(
                            text = "Back Side",
                            color = if (showBackSide) palette.surface else palette.ink,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Document Title matching Screen 4 of the mockup
            Text(
                text = doc.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.ink,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Pill matching Screen 4 of the mockup while respecting Section 0/1 non-goals
            // (Clearly labels OCR needing review, Sample Data, or User-Confirmed Vault Record)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (hasUnconfirmedOcr) palette.warningContainer else palette.successContainer,
                modifier = Modifier
                    .clickable(enabled = hasUnconfirmedOcr) { onConfirmOcrFields(doc.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (hasUnconfirmedOcr) palette.warning else palette.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            hasUnconfirmedOcr -> "OCR Extracted · Tap to Confirm"
                            doc.isSamplePlaceholder -> "Confirmed · Sample Preview"
                            else -> "Confirmed · Local Vault"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (hasUnconfirmedOcr) Color(0xFF7A4F00) else palette.success
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Structured Fields Card matching Screen 4 of the mockup
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                border = BorderStroke(1.1.dp, palette.border.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    fields.forEachIndexed { idx, field ->
                        val isRevealed = revealedFields[field.id] == true
                        val shouldMask = field.isSensitive && field.isMaskedByDefault && !isRevealed
                        val displayVal = if (shouldMask) maskSensitiveValue(field.value) else field.value

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onCopySingleField(field.label, field.value)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (field.iconKey) {
                                    "person" -> Icons.Default.PersonOutline
                                    "calendar" -> Icons.Default.CalendarToday
                                    "id" -> Icons.Default.Badge
                                    "gender" -> Icons.Default.PersonOutline
                                    "location" -> Icons.Default.LocationOn
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = palette.ink,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.width(100.dp)) {
                                Text(
                                    text = field.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.inkMuted
                                )
                                if (field.source == FieldSource.OCR.name && !field.userConfirmed) {
                                    Text(
                                        text = "OCR unconfirmed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF8A5A00)
                                    )
                                }
                            }
                            Text(
                                text = displayVal,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.ink,
                                modifier = Modifier.weight(1f)
                            )
                            if (field.isSensitive && field.isMaskedByDefault) {
                                IconButton(
                                    onClick = { revealedFields[field.id] = !isRevealed },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isRevealed) "Mask field" else "Reveal field",
                                        tint = palette.inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onCopySingleField(field.label, field.value) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy ${field.label}",
                                    tint = palette.inkMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        if (idx < fields.lastIndex) {
                            HorizontalDivider(color = palette.border.copy(alpha = 0.12f))
                        }
                    }
                }
            }

            // Metadata summary row (Category, Issue Date, Expiry Date, Notes)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Category: ${doc.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted
                )
                if (doc.expiryDate.isNotBlank()) {
                    Text(
                        text = "Expires: ${doc.expiryDate}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Three stacked action buttons matching Screen 4 of the SAFEAREA mockup:
            // 1. Copy All Info (Raspberry primary pill)
            SafeAreaPrimaryButton(
                text = "Copy All Info",
                leadingIcon = Icons.Default.ContentCopy,
                onClick = { onCopyAllFields(docWithFields) },
                modifier = Modifier.testTag("copy_all_info_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. View Full Image (Outlined navy pill)
            SafeAreaOutlinedPillButton(
                text = "View Full Image",
                leadingIcon = Icons.Default.Image,
                onClick = { showFullViewer = true },
                modifier = Modifier.testTag("view_full_image_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Share Document (Outlined navy pill)
            SafeAreaOutlinedPillButton(
                text = "Share Document",
                leadingIcon = Icons.Default.Share,
                onClick = { showShareWarning = true },
                modifier = Modifier.testTag("share_document_button")
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Full-screen zoomable viewer dialog
    if (showFullViewer) {
        var scale by remember { mutableFloatStateOf(1f) }
        var rotation by remember { mutableFloatStateOf(0f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Dialog(
            onDismissRequest = { showFullViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF12131C)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${doc.title} · Pinch to Zoom",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Row {
                            IconButton(onClick = { scale = (scale + 0.4f).coerceAtMost(3.5f) }) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                            }
                            IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate view", tint = Color.White)
                            }
                            IconButton(onClick = { showFullViewer = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close viewer", tint = Color.White)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    rotationZ = rotation,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        ) {
                            val bmp = previewBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = doc.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                MockupNationalIdCardVisual(
                                    document = doc,
                                    holderName = fields.find { it.label.contains("Name", ignoreCase = true) }?.value ?: "Rahul Shah"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sensitive Share Warning Dialog required by Section 6 of PRD
    if (showShareWarning) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showShareWarning = false },
            containerColor = palette.surface,
            title = { Text("Share Sensitive Document?") },
            text = {
                Text(
                    "Sharing exports unencrypted text or document files outside SAFEAREA. Make sure you trust the recipient app."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareWarning = false
                        onShareDocument(docWithFields, doc.frontFileRef.isNotBlank())
                    }
                ) {
                    Text("Share Now", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareWarning = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    // Explicit Delete Confirmation Dialog required by Section 13 of PRD
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = palette.surface,
            title = { Text("Delete \"${doc.title}\"?") },
            text = {
                Text(
                    "This will permanently delete \"${doc.title}\", its structured fields, encrypted scan files, and expiry reminders from your device."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteDocument(doc)
                    }
                ) {
                    Text("Delete Permanently", color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }
}

private fun maskSensitiveValue(raw: String): String {
    if (raw.length <= 4) return "••••"
    val visibleSuffix = raw.takeLast(4)
    return "••••••$visibleSuffix"
}

/**
 * Custom Canvas & Compose reproduction of the National ID Card shown in Screen 4 of the SAFEAREA mockup:
 * Light icy-blue card with Himalayan peaks, Government of Nepal header, crimson double-pennant flag,
 * avatar silhouette, data bars, signature ("Rahul"), and gold smart-card chip.
 */
@Composable
fun MockupNationalIdCardVisual(
    document: DocumentEntity,
    holderName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(205.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, Color(0xFFB5C4D8)),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFDCE7F5),
                            Color(0xFFEAF1F9),
                            Color(0xFFD3E1F2)
                        )
                    )
                )
        ) {
            // Subtle Himalayan mountain silhouette + emblem + flag + chip in Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // White mountain peak silhouette on right half
                val mountainPath = Path().apply {
                    moveTo(w * 0.38f, h)
                    lineTo(w * 0.62f, h * 0.42f)
                    lineTo(w * 0.72f, h * 0.56f)
                    lineTo(w * 0.84f, h * 0.34f)
                    lineTo(w, h * 0.60f)
                    lineTo(w, h)
                    close()
                }
                drawPath(mountainPath, color = Color.White.copy(alpha = 0.55f))

                // Top-left circular national emblem
                val emblemCx = w * 0.11f
                val emblemCy = h * 0.20f
                val emblemR = h * 0.11f
                drawCircle(
                    color = Color(0xFFB8293D),
                    radius = emblemR,
                    center = Offset(emblemCx, emblemCy),
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = Color(0xFF2E5C9A),
                    radius = emblemR * 0.68f,
                    center = Offset(emblemCx, emblemCy)
                )

                // Top-right Nepal double-pennant flag
                val flagLeft = w * 0.85f
                val flagTop = h * 0.10f
                val flagW = w * 0.08f
                val flagH = h * 0.22f
                val flagPath = Path().apply {
                    moveTo(flagLeft, flagTop)
                    lineTo(flagLeft + flagW, flagTop + flagH * 0.45f)
                    lineTo(flagLeft + flagW * 0.35f, flagTop + flagH * 0.45f)
                    lineTo(flagLeft + flagW, flagTop + flagH)
                    lineTo(flagLeft, flagTop + flagH)
                    close()
                }
                drawPath(flagPath, color = Color(0xFFD5104B))
                drawPath(flagPath, color = Color(0xFF171431), style = Stroke(width = 3f))

                // Avatar portrait box at bottom-left
                val avLeft = w * 0.06f
                val avTop = h * 0.40f
                val avW = w * 0.26f
                val avH = h * 0.48f
                drawRoundRect(
                    color = Color(0xFFE7EEF7),
                    topLeft = Offset(avLeft, avTop),
                    size = Size(avW, avH),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Head & shoulders silhouette inside avatar box
                val headCenter = Offset(avLeft + avW * 0.5f, avTop + avH * 0.38f)
                drawCircle(
                    color = Color(0xFF495468),
                    radius = avW * 0.22f,
                    center = headCenter
                )
                drawArc(
                    color = Color(0xFF495468),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(avLeft + avW * 0.12f, avTop + avH * 0.62f),
                    size = Size(avW * 0.76f, avH * 0.65f)
                )

                // Placeholder text bars in middle-right
                val barLeft = w * 0.37f
                drawRoundRect(
                    color = Color(0xFFAEC0D6),
                    topLeft = Offset(barLeft, h * 0.44f),
                    size = Size(w * 0.34f, 12f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFFBCCCE0),
                    topLeft = Offset(barLeft, h * 0.55f),
                    size = Size(w * 0.25f, 11f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                drawRoundRect(
                    color = Color(0xFFBCCCE0),
                    topLeft = Offset(barLeft, h * 0.65f),
                    size = Size(w * 0.30f, 11f),
                    cornerRadius = CornerRadius(5f, 5f)
                )

                // Gold IC Chip at bottom-right matching Screen 4 of mockup
                val chipLeft = w * 0.82f
                val chipTop = h * 0.72f
                val chipW = w * 0.11f
                val chipH = h * 0.16f
                drawRoundRect(
                    color = Color(0xFFE5BA63),
                    topLeft = Offset(chipLeft, chipTop),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = Color(0xFFB58932),
                    topLeft = Offset(chipLeft, chipTop),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 2f)
                )
            }

            // Header text at top center
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = document.issuer.ifBlank { "GOVERNMENT OF NEPAL" }.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp
                    ),
                    color = Color(0xFF171431)
                )
                Text(
                    text = if (document.title.equals("National ID", ignoreCase = true)) {
                        "NATIONAL ID CARD"
                    } else {
                        document.title.uppercase()
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF171431)
                )
            }

            // Signature style text at bottom-right next to chip
            Text(
                text = holderName.substringBefore(" "),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF171431),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 78.dp, bottom = 18.dp)
            )
        }
    }
}
