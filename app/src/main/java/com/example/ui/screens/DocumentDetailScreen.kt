package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CryptoStorageManager
import com.example.data.DocumentEntity
import com.example.data.DocumentWithFields
import com.example.data.FieldSource
import com.example.data.SupportedFileFormatGroup
import com.example.data.VaultRepository
import com.example.ui.components.DocumentCategoryIconBox
import com.example.ui.components.SafeAreaBackButton
import com.example.ui.components.SafeAreaMoreButton
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaPrimaryButton
import com.example.ui.components.formatCompactFileSize
import com.example.ui.daysUntilExpiry
import com.example.ui.theme.LocalSafeAreaPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Universal Document Detail & Viewer Screen:
 * - Zero fake or mock cards
 * - Real PDF multi-page rendering via Android PdfRenderer
 * - Real decrypted image preview with rotation & pinch-to-zoom
 * - Real in-app text/content preview for Word (.docx/.doc), Excel (.xlsx/.xls), PowerPoint (.pptx/.ppt),
 *   OpenDocument (.odt/.ods/.odp), Text (.txt/.rtf/.md), CSV, JSON, and XML
 * - Rename, Duplicate, Move Category, Edit Details, Open in External Viewer, Archive/Restore, Share, Delete
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentDetailScreen(
    docWithFields: DocumentWithFields?,
    allCategories: List<String>,
    cryptoStorage: CryptoStorageManager,
    repository: VaultRepository,
    onBack: () -> Unit,
    onEditDocument: (DocumentWithFields) -> Unit,
    onRenameDocument: (DocumentEntity, String) -> Unit,
    onDuplicateDocument: (DocumentWithFields) -> Unit,
    onMoveCategory: (DocumentEntity, String) -> Unit,
    onToggleFavourite: (DocumentEntity) -> Unit,
    onToggleArchive: (DocumentEntity) -> Unit,
    onRotateAttachment: (DocumentEntity, Boolean) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onConfirmOcrFields: (Long) -> Unit,
    onCopySingleField: (String, String) -> Unit,
    onCopyAllFields: (DocumentWithFields) -> Unit,
    onOpenExternalViewer: (DocumentEntity) -> Unit,
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
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember(doc.title) { mutableStateOf(doc.title) }
    var showMoveCategoryDialog by remember { mutableStateOf(false) }
    var showBackSide by remember { mutableStateOf(false) }

    val revealedFields = remember { mutableStateMapOf<Long, Boolean>() }

    val activeFileRef = if (showBackSide && doc.backFileRef.isNotBlank()) doc.backFileRef else doc.frontFileRef
    var previewBitmap by remember(activeFileRef) { mutableStateOf<Bitmap?>(null) }
    var extractedTextPreview by remember(activeFileRef) { mutableStateOf<String?>(null) }
    var pdfPageCount by remember(activeFileRef) { mutableIntStateOf(1) }
    var currentPdfPage by remember(activeFileRef) { mutableIntStateOf(0) }

    LaunchedEffect(activeFileRef, currentPdfPage, doc.mimeType, doc.fileExtension) {
        if (activeFileRef.isBlank()) {
            previewBitmap = null
            extractedTextPreview = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val formatGroup = doc.formatGroup
            if (formatGroup == SupportedFileFormatGroup.PDF || doc.mimeType.contains("pdf", ignoreCase = true)) {
                extractedTextPreview = null
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
            } else if (formatGroup == SupportedFileFormatGroup.IMAGE || doc.mimeType.startsWith("image/", ignoreCase = true)) {
                extractedTextPreview = null
                val bytes = cryptoStorage.readDecryptedFile(activeFileRef)
                if (bytes != null) {
                    previewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } else {
                previewBitmap = null
                extractedTextPreview = repository.readDocumentTextPreview(doc)
            }
        }
    }

    val hasUnconfirmedOcr = fields.any { !it.userConfirmed }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeAreaBackButton(onClick = onBack)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleFavourite(doc) }) {
                        Icon(
                            imageVector = if (doc.favourite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (doc.favourite) "Remove from favourites" else "Add to favourites",
                            tint = if (doc.favourite) Color(0xFFE09F1F) else palette.ink
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
                                text = { Text("Rename Document", color = palette.ink) },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = palette.ink) },
                                onClick = {
                                    showMenu = false
                                    renameInput = doc.title
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Category", color = palette.ink) },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = palette.ink) },
                                onClick = {
                                    showMenu = false
                                    showMoveCategoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate Document", color = palette.ink) },
                                leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null, tint = palette.ink) },
                                onClick = {
                                    showMenu = false
                                    onDuplicateDocument(docWithFields)
                                }
                            )
                            if (activeFileRef.isNotBlank() && doc.formatGroup == SupportedFileFormatGroup.IMAGE) {
                                DropdownMenuItem(
                                    text = { Text("Rotate Image 90°", color = palette.ink) },
                                    leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null, tint = palette.ink) },
                                    onClick = {
                                        showMenu = false
                                        onRotateAttachment(doc, showBackSide)
                                    }
                                )
                            }
                            if (doc.frontFileRef.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Open in External Viewer", color = palette.ink) },
                                    leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = palette.ink) },
                                    onClick = {
                                        showMenu = false
                                        onOpenExternalViewer(doc)
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
                                text = { Text("Delete Permanently", color = palette.danger, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real Document Preview Card (PDF / Image / Office / Text / Structured Record)
            val bmp = previewBitmap
            val textContent = extractedTextPreview

            if (bmp != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showFullViewer = true }
                        .testTag("document_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surface,
                    border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Document preview for ${doc.title}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 190.dp, max = 260.dp)
                                .background(palette.canvas)
                                .padding(8.dp)
                        )
                        if (doc.formatGroup == SupportedFileFormatGroup.PDF && pdfPageCount > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.surface)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
                                    text = "Page ${currentPdfPage + 1} of $pdfPageCount",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
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
            } else if (!textContent.isNullOrBlank()) {
                // In-app Office / OpenDocument / Text / CSV / JSON / XML Viewer Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("document_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surface,
                    border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DocumentCategoryIconBox(categoryGroup = doc.categoryGroup, size = 40.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "In-App ${doc.displayFormatBadge} Content Preview",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.ink
                                    )
                                    Text(
                                        text = doc.originalFileName.ifBlank { doc.title },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.inkMuted
                                    )
                                }
                            }
                            TextButton(onClick = { showFullViewer = true }) {
                                Text("Expand", color = palette.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.canvas,
                            border = BorderStroke(0.8.dp, palette.border.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = textContent,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = palette.ink
                                )
                            }
                        }
                    }
                }
            } else {
                // Clean Real Document Header Summary Card (when no binary image/text preview is attached)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("document_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surface,
                    border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DocumentCategoryIconBox(categoryGroup = doc.categoryGroup, size = 56.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.category,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = palette.primary
                            )
                            if (doc.documentSubtype.isNotBlank()) {
                                Text(
                                    text = doc.documentSubtype,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.ink
                                )
                            }
                            Text(
                                text = if (doc.originalFileName.isNotBlank()) {
                                    "Encrypted File: ${doc.originalFileName} (${formatCompactFileSize(doc.fileSize)})"
                                } else {
                                    "Structured Vault Record · On-Device Encrypted"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkMuted
                            )
                        }
                    }
                }
            }

            // Front / Back Side Toggle if Two-Sided Scan
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

            // Document Title & Badges
            Text(
                text = doc.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.ink,
                modifier = Modifier.align(Alignment.Start)
            )

            if (doc.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doc.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (hasUnconfirmedOcr) palette.warningContainer else palette.successContainer,
                    modifier = Modifier.clickable(enabled = hasUnconfirmedOcr) { onConfirmOcrFields(doc.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (hasUnconfirmedOcr) palette.warning else palette.success),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasUnconfirmedOcr) "OCR Review Needed · Tap to Confirm" else "Encrypted in Local Vault",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasUnconfirmedOcr) Color(0xFF7A4F00) else palette.success
                        )
                    }
                }

                // Format Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = buildString {
                            append(doc.displayFormatBadge)
                            val s = formatCompactFileSize(doc.fileSize)
                            if (s.isNotBlank()) append(" · $s")
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Expiry Badge
                val daysLeft = daysUntilExpiry(doc.expiryDate)
                if (daysLeft != null) {
                    val isUrgent = daysLeft <= 45L
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isUrgent) palette.warningContainer else palette.successContainer
                    ) {
                        Text(
                            text = when {
                                daysLeft < 0L -> "Expired (${doc.expiryDate})"
                                daysLeft == 0L -> "Expires Today"
                                else -> "Expires in ${daysLeft}d (${doc.expiryDate})"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isUrgent) Color(0xFF8A5A00) else palette.success,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Structured Fields Card
            if (fields.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    border = BorderStroke(1.1.dp, palette.border.copy(alpha = 0.32f))
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
                                Column(modifier = Modifier.weight(0.42f)) {
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
                                    text = displayVal.ifBlank { "—" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.ink,
                                    modifier = Modifier.weight(0.58f)
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Document Metadata & Timestamps Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Category: ${doc.category}${if (doc.documentSubtype.isNotBlank()) " · ${doc.documentSubtype}" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.ink
                    )
                    if (doc.issuer.isNotBlank() || doc.country.isNotBlank()) {
                        Text(
                            text = "Issuer / Region: ${listOf(doc.issuer, doc.country).filter { it.isNotBlank() }.joinToString(" · ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
                        )
                    }
                    if (doc.issueDate.isNotBlank() || doc.expiryDate.isNotBlank()) {
                        Text(
                            text = buildString {
                                if (doc.issueDate.isNotBlank()) append("Issued: ${doc.issueDate}   ")
                                if (doc.expiryDate.isNotBlank()) append("Expires: ${doc.expiryDate}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
                        )
                    }
                    if (doc.tagsCsv.isNotBlank()) {
                        Text(
                            text = "Tags: ${doc.tagsCsv}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
                        )
                    }
                    if (doc.notes.isNotBlank()) {
                        Text(
                            text = "Notes: ${doc.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink
                        )
                    }
                    Text(
                        text = "Added: ${dateFormatter.format(Date(doc.createdAt))} · Modified: ${dateFormatter.format(Date(doc.updatedAt))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action Buttons
            SafeAreaPrimaryButton(
                text = "Copy All Info",
                leadingIcon = Icons.Default.ContentCopy,
                onClick = { onCopyAllFields(docWithFields) },
                modifier = Modifier.testTag("copy_all_info_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bmp != null || !textContent.isNullOrBlank()) {
                    SafeAreaOutlinedPillButton(
                        text = "Full Viewer",
                        leadingIcon = Icons.Default.Image,
                        onClick = { showFullViewer = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_full_image_button")
                    )
                }
                if (doc.frontFileRef.isNotBlank()) {
                    SafeAreaOutlinedPillButton(
                        text = "External App",
                        leadingIcon = Icons.Default.OpenInNew,
                        onClick = { onOpenExternalViewer(doc) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_external_viewer_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SafeAreaOutlinedPillButton(
                    text = "Edit Details",
                    leadingIcon = Icons.Default.Edit,
                    onClick = { onEditDocument(docWithFields) },
                    modifier = Modifier.weight(1f)
                )
                SafeAreaOutlinedPillButton(
                    text = "Share / Export",
                    leadingIcon = Icons.Default.Share,
                    onClick = { showShareWarning = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_document_button")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Full-screen zoomable / scrollable viewer dialog
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
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            if (previewBitmap != null) {
                                IconButton(onClick = { scale = (scale + 0.4f).coerceAtMost(3.5f) }) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                                }
                                IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate view", tint = Color.White)
                                }
                            }
                            IconButton(onClick = { showFullViewer = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close viewer", tint = Color.White)
                            }
                        }
                    }

                    val fullBmp = previewBitmap
                    val fullText = extractedTextPreview
                    if (fullBmp != null) {
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
                            Image(
                                bitmap = fullBmp.asImageBitmap(),
                                contentDescription = doc.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        rotationZ = rotation,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        }
                    } else if (!fullText.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1C1E2B)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = fullText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFF3EFE6)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = palette.surface,
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameDocument(doc, renameInput)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Save", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    // Move Category Dialog
    if (showMoveCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showMoveCategoryDialog = false },
            containerColor = palette.surface,
            title = { Text("Move \"${doc.title}\" To Category") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCategories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (doc.category.equals(cat, ignoreCase = true)) palette.successContainer else palette.canvas,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMoveCategoryDialog = false
                                    onMoveCategory(doc, cat)
                                }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.ink,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveCategoryDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }

    // Sensitive Share Warning Dialog
    if (showShareWarning) {
        AlertDialog(
            onDismissRequest = { showShareWarning = false },
            containerColor = palette.surface,
            title = { Text("Share or Export Document?") },
            text = {
                Text(
                    "Sharing exports decrypted text or document files outside SAFEAREA. Make sure you trust the recipient application."
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

    // Explicit Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = palette.surface,
            title = { Text("Delete \"${doc.title}\"?") },
            text = {
                Text(
                    "This will permanently delete \"${doc.title}\", its structured fields, encrypted files, and expiry reminders from your device."
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
