package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.DocumentCategoryGroup
import com.example.data.DocumentEntity
import com.example.data.DocumentFieldEntity
import com.example.data.FieldSource
import com.example.ui.components.DocumentCategoryIconBox
import com.example.ui.components.FolderWithLockIllustration
import com.example.ui.components.SafeAreaBackButton
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaPrimaryButton
import com.example.ui.theme.LocalSafeAreaPalette
import java.io.File

/**
 * Add Document Screen supporting:
 * 1. Native Android Camera App (`ActivityResultContracts.TakePicture`) for smooth, full-resolution photo capture
 * 2. Zero-permission Android Photo Picker (Gallery)
 * 3. Universal File Import (PDF, Word, Excel, PowerPoint, Images, Text, OpenDocument, CSV, JSON, XML)
 * 4. Manual Structured Entry across all 15 Category Groups & 100+ Document Subtypes
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDocumentScreen(
    onBack: () -> Unit,
    onOpenCameraScanner: () -> Unit,
    onImportUri: (Uri) -> Unit,
    onEnterManually: (presetCategory: String, presetSubtype: String) -> Unit
) {
    val palette = LocalSafeAreaPalette.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportUri(uri)
    }

    val universalFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportUri(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeAreaBackButton(onClick = onBack)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add Document",
                style = MaterialTheme.typography.headlineLarge,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Click photos with your phone's native Android camera app, import any file format, or enter structured fields.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Native Android Camera App
            AddOptionCard(
                title = "Click Photo with Native Camera",
                subtitle = "Launches your phone's native Android camera app with post-capture crop, rotate & enhance",
                customIcon = { ScanFrameCardIcon(color = palette.ink) },
                onClick = onOpenCameraScanner,
                testTag = "add_scan_document_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Choose from Gallery (Images)
            AddOptionCard(
                title = "Choose Image from Gallery",
                subtitle = "JPG, JPEG, PNG, WEBP, HEIC, BMP, TIFF",
                icon = Icons.Default.Image,
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                testTag = "add_choose_gallery_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Universal File Import (PDF, Word, Excel, PowerPoint, Text, OpenDocument, CSV, JSON, XML)
            AddOptionCard(
                title = "Import Document File (All Formats)",
                subtitle = "PDF, Word (.doc/.docx), Excel (.xls/.xlsx), PPT (.ppt/.pptx), TXT, RTF, MD, ODT/ODS/ODP, CSV, JSON, XML",
                icon = Icons.Default.Description,
                onClick = {
                    universalFilePickerLauncher.launch(arrayOf("*/*"))
                },
                testTag = "add_import_file_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Enter Manually
            AddOptionCard(
                title = "Create Structured Record Manually",
                subtitle = "Choose from 15 categories & 100+ document types",
                icon = Icons.Default.Edit,
                onClick = {
                    onEnterManually(
                        DocumentCategoryGroup.PERSONAL.title,
                        DocumentCategoryGroup.PERSONAL.subtypes.first()
                    )
                },
                testTag = "add_enter_manually_button"
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Category Templates (All 15 Category Groups)
            Text(
                text = "Quick Start by Category (15 Categories)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = palette.ink
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                DocumentCategoryGroup.entries.forEach { group ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(palette.cardCornerRadius))
                            .clickable {
                                onEnterManually(group.title, group.subtypes.firstOrNull().orEmpty())
                            },
                        shape = RoundedCornerShape(palette.cardCornerRadius),
                        color = palette.surface,
                        border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DocumentCategoryIconBox(categoryGroup = group, size = 34.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.shortTitle,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.ink,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .padding(top = 18.dp, bottom = 12.dp)
        ) {
            FolderWithLockIllustration()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Encrypted on-device with Android Keystore AES-256-GCM.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null
) {
    val palette = LocalSafeAreaPalette.current
    val shape = RoundedCornerShape(palette.cardCornerRadius)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = shape,
        color = palette.surface,
        border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (customIcon != null) {
                    customIcon()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.ink,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = palette.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted
                )
            }
        }
    }
}

@Composable
private fun ScanFrameCardIcon(color: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width
            val len = s * 0.28f
            val stroke = 4.5f
            drawLine(color, Offset(0f, len), Offset(0f, 0f), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
            drawLine(color, Offset(s - len, 0f), Offset(s, 0f), stroke, StrokeCap.Round)
            drawLine(color, Offset(s, 0f), Offset(s, len), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, s - len), Offset(0f, s), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, s), Offset(len, s), stroke, StrokeCap.Round)
            drawLine(color, Offset(s - len, s), Offset(s, s), stroke, StrokeCap.Round)
            drawLine(color, Offset(s, s - len), Offset(s, s), stroke, StrokeCap.Round)
        }
        Icon(
            imageVector = Icons.Default.TagFaces,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Native Android System Camera App Document Capture & Studio:
 * - Launches the phone's genuine Native Android Camera App (`ActivityResultContracts.TakePicture` with `FileProvider` URI)
 *   so clicking images is smooth, fast, and uses the device's full camera hardware ISP, autofocus, HDR & flash.
 * - Includes a 1-tap Quick Camera Snapshot fallback (`ActivityResultContracts.TakePicturePreview`) and Gallery Import.
 * - Post-capture Document Studio supports:
 *   - 90° Rotation
 *   - Aspect-Ratio Edge Cropping (ID Card, Passport, A4 Page)
 *   - High-Contrast Document Enhancement
 *   - Two-sided (Front & Back) native camera capture
 *   - 15 Categories & 100+ Subtypes selection
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraScannerScreen(
    onBack: () -> Unit,
    onFallbackImportPhoto: () -> Unit,
    onFallbackManualEntry: () -> Unit,
    onScanCompleted: (
        frontBitmap: Bitmap,
        backBitmap: Bitmap?,
        titleHint: String,
        categoryHint: String,
        subtypeHint: String,
        rawOcrText: String
    ) -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var capturedFront by remember { mutableStateOf<Bitmap?>(null) }
    var capturedBack by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturingBackSide by remember { mutableStateOf(false) }
    var selectedFrameAspect by remember { mutableStateOf("ID Card") } // "ID Card", "Passport", "A4 Page"
    var selectedCategoryGroup by remember { mutableStateOf(DocumentCategoryGroup.PERSONAL) }
    var selectedSubtype by remember { mutableStateOf(DocumentCategoryGroup.PERSONAL.subtypes.first()) }
    var customTitleHint by remember { mutableStateOf("") }
    var customOcrNotes by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var hasAutoLaunchedOnce by remember { mutableStateOf(false) }

    // 1. Full-resolution Native Android Camera App launcher via FileProvider
    val nativeCameraTakePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCaptureFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            val decoded = decodeSampledBitmapFromFile(file, maxDimension = 2200)
            if (decoded != null) {
                if (isCapturingBackSide) {
                    capturedBack = decoded
                } else {
                    capturedFront = decoded
                }
                statusMessage = null
            } else {
                statusMessage = "Could not decode captured photo. Please try again or pick from Gallery."
            }
        } else {
            if (capturedFront == null) {
                statusMessage = "Tap 'Open Native Camera App' below to click a photo."
            }
        }
    }

    // 2. Quick Native Camera Bitmap Preview launcher (fallback for devices/ROMs without FileProvider output)
    val quickCameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            if (isCapturingBackSide) {
                capturedBack = bitmap
            } else {
                capturedFront = bitmap
            }
            statusMessage = null
        }
    }

    fun launchNativeCameraApp(forBackSide: Boolean) {
        isCapturingBackSide = forBackSide
        statusMessage = null
        runCatching {
            val outFile = createCameraOutputFile(context)
            pendingCaptureFile = outFile
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            nativeCameraTakePictureLauncher.launch(uri)
        }.onFailure {
            runCatching {
                quickCameraPreviewLauncher.launch(null)
            }.onFailure { err ->
                statusMessage = "No camera app available (${err.localizedMessage ?: "use Gallery import"})."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted && !hasAutoLaunchedOnce && capturedFront == null) {
            hasAutoLaunchedOnce = true
            launchNativeCameraApp(forBackSide = false)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else if (!hasAutoLaunchedOnce && capturedFront == null) {
            hasAutoLaunchedOnce = true
            launchNativeCameraApp(forBackSide = false)
        }
    }

    // Post-Capture Document Studio (Rotate, Crop, Enhance, Back Side & Category Selection)
    val activeCaptured = if (isCapturingBackSide && capturedBack != null) capturedBack else capturedFront
    if (activeCaptured != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.canvas)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SafeAreaBackButton(onClick = {
                        if (isCapturingBackSide && capturedBack != null) {
                            capturedBack = null
                            isCapturingBackSide = false
                        } else {
                            capturedFront = null
                        }
                    })
                    Text(
                        text = if (isCapturingBackSide && capturedBack != null) {
                            "Review Back Side Photo"
                        } else {
                            "Review & Enhance Camera Photo"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.ink
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    color = palette.surface,
                    border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = 0.5f))
                ) {
                    Image(
                        bitmap = activeCaptured.asImageBitmap(),
                        contentDescription = "Captured camera photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Aspect Preset Selector for Auto-Edge Crop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crop Preset:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.inkMuted
                    )
                    listOf("ID Card", "Passport", "A4 Page").forEach { aspect ->
                        FilterChip(
                            selected = selectedFrameAspect == aspect,
                            onClick = { selectedFrameAspect = aspect },
                            label = { Text(aspect) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Real Image Processing Controls: Rotate 90°, Auto-Edge Crop, Contrast Enhance, Retake in Native Camera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScannerEditChip(
                        label = "Rotate 90°",
                        icon = Icons.Default.RotateRight,
                        onClick = {
                            val rotated = rotateBitmap(activeCaptured, 90f)
                            if (isCapturingBackSide && capturedBack != null) capturedBack = rotated else capturedFront = rotated
                        }
                    )
                    ScannerEditChip(
                        label = "Crop ($selectedFrameAspect)",
                        icon = Icons.Default.Crop,
                        onClick = {
                            val cropped = cropBitmapToFrameAspect(activeCaptured, selectedFrameAspect)
                            if (isCapturingBackSide && capturedBack != null) capturedBack = cropped else capturedFront = cropped
                        }
                    )
                    ScannerEditChip(
                        label = "Enhance Contrast",
                        icon = Icons.Default.AutoFixHigh,
                        onClick = {
                            val enhanced = enhanceDocumentScanBitmap(activeCaptured)
                            if (isCapturingBackSide && capturedBack != null) capturedBack = enhanced else capturedFront = enhanced
                        }
                    )
                    ScannerEditChip(
                        label = "Retake Photo",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            launchNativeCameraApp(forBackSide = isCapturingBackSide && capturedBack != null)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Group Selector (All 15 Categories)
                Text(
                    text = "Document Category (15 Categories)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = palette.ink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocumentCategoryGroup.entries.forEach { group ->
                        FilterChip(
                            selected = selectedCategoryGroup == group,
                            onClick = {
                                selectedCategoryGroup = group
                                selectedSubtype = group.subtypes.firstOrNull().orEmpty()
                            },
                            label = { Text(group.shortTitle) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subtype Selector
                Text(
                    text = "Document Type (${selectedCategoryGroup.shortTitle})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = palette.ink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCategoryGroup.subtypes.forEach { sub ->
                        FilterChip(
                            selected = selectedSubtype == sub,
                            onClick = { selectedSubtype = sub },
                            label = { Text(sub) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.ink,
                                selectedLabelColor = palette.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customTitleHint,
                    onValueChange = { customTitleHint = it },
                    label = { Text("Document Title (defaults to $selectedSubtype)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customOcrNotes,
                    onValueChange = { customOcrNotes = it },
                    label = { Text("Optional Key:Value Fields (e.g. ID No: 12345)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (capturedBack == null) {
                    SafeAreaOutlinedPillButton(
                        text = "Click Back Side with Native Camera",
                        leadingIcon = Icons.Default.CameraAlt,
                        onClick = { launchNativeCameraApp(forBackSide = true) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.successContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Front & Back photos attached (2 pages)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = palette.onSuccessContainer
                            )
                            TextButton(onClick = { isCapturingBackSide = !isCapturingBackSide }) {
                                Text(if (isCapturingBackSide) "View Front" else "View Back")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                SafeAreaPrimaryButton(
                    text = "Save Photo & Continue to Details",
                    leadingIcon = Icons.Default.AutoAwesome,
                    onClick = {
                        val front = capturedFront ?: return@SafeAreaPrimaryButton
                        onScanCompleted(
                            front,
                            capturedBack,
                            customTitleHint.ifBlank { selectedSubtype },
                            selectedCategoryGroup.title,
                            selectedSubtype,
                            customOcrNotes
                        )
                    },
                    modifier = Modifier.testTag("confirm_scan_ocr_button")
                )
            }
        }
        return
    }

    // Native Android System Camera Launcher Hub
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeAreaBackButton(onClick = onBack)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = palette.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, palette.primary.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Native Android Camera",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Uses your phone's built-in Android camera app for smooth autofocus, HDR, flash, and crisp document photos. After clicking a photo, you can crop, rotate, and enhance it before encrypted storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Pre-select category before or after clicking image
            Surface(
                shape = RoundedCornerShape(palette.cardCornerRadius),
                color = palette.surface,
                border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Document Category Preset",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocumentCategoryGroup.entries.forEach { group ->
                            FilterChip(
                                selected = selectedCategoryGroup == group,
                                onClick = {
                                    selectedCategoryGroup = group
                                    selectedSubtype = group.subtypes.firstOrNull().orEmpty()
                                },
                                label = { Text(group.shortTitle) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Document Subtype",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedCategoryGroup.subtypes.forEach { sub ->
                            FilterChip(
                                selected = selectedSubtype == sub,
                                onClick = { selectedSubtype = sub },
                                label = { Text(sub) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.ink,
                                    selectedLabelColor = palette.surface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SafeAreaPrimaryButton(
                text = "Open Native Android Camera",
                leadingIcon = Icons.Default.CameraAlt,
                onClick = {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        launchNativeCameraApp(forBackSide = false)
                    }
                },
                modifier = Modifier.testTag("camera_shutter_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SafeAreaOutlinedPillButton(
                    text = "Quick Snapshot",
                    leadingIcon = Icons.Default.PhotoCamera,
                    onClick = {
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            runCatching { quickCameraPreviewLauncher.launch(null) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                SafeAreaOutlinedPillButton(
                    text = "Pick Gallery",
                    leadingIcon = Icons.Default.Image,
                    onClick = onFallbackImportPhoto,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SafeAreaOutlinedPillButton(
                text = "Enter Details Manually",
                leadingIcon = Icons.Default.Edit,
                onClick = onFallbackManualEntry
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun createCameraOutputFile(context: Context): File {
    val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
    return File(dir, "native_camera_${System.currentTimeMillis()}.jpg")
}

private fun decodeSampledBitmapFromFile(file: File, maxDimension: Int): Bitmap? {
    return runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDimension || boundsOpts.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
    }.getOrNull()
}

@Composable
private fun ScannerEditChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.6f)),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = palette.ink, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = palette.ink)
        }
    }
}

private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

private fun cropBitmapToFrameAspect(src: Bitmap, aspectMode: String): Bitmap {
    val targetW = (src.width * 0.88f).toInt().coerceAtLeast(120)
    val aspect = when (aspectMode) {
        "ID Card" -> 0.63f
        "Passport" -> 0.72f
        else -> 1.25f
    }
    val desiredH = (targetW * aspect).toInt().coerceIn(100, src.height)
    val x = ((src.width - targetW) / 2).coerceAtLeast(0)
    val y = ((src.height - desiredH) / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(
        src,
        x,
        y,
        targetW.coerceAtMost(src.width - x),
        desiredH.coerceAtMost(src.height - y)
    )
}

private fun enhanceDocumentScanBitmap(src: Bitmap): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    val contrast = 1.28f
    val translate = -28f
    val cm = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}

/**
 * Document Editor & Metadata Configuration Screen:
 * - Supports all 15 DocumentCategoryGroup categories + custom categories
 * - Supports all 100+ Document Subtypes with 1-tap selection
 * - Supports attaching any file format or clicking a photo with the Native Android Camera App
 * - Supports Issue Date, Expiry Date, Custom Reminder Lead Days, Description, Tags, Notes, and Structured Fields
 */
@Composable
fun DocumentEditorScreen(
    initialDocument: DocumentEntity,
    initialFields: List<DocumentFieldEntity>,
    allCategories: List<String>,
    isOcrReviewMode: Boolean,
    onAttachFrontOrBackUri: (Uri, Boolean) -> Unit,
    onSave: (DocumentEntity, List<DocumentFieldEntity>, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val context = LocalContext.current

    var title by remember(initialDocument) { mutableStateOf(initialDocument.title) }
    var category by remember(initialDocument) { mutableStateOf(initialDocument.category) }
    var documentSubtype by remember(initialDocument) { mutableStateOf(initialDocument.documentSubtype) }
    var description by remember(initialDocument) { mutableStateOf(initialDocument.description) }
    var issuer by remember(initialDocument) { mutableStateOf(initialDocument.issuer) }
    var country by remember(initialDocument) { mutableStateOf(initialDocument.country) }
    var tagsCsv by remember(initialDocument) { mutableStateOf(initialDocument.tagsCsv) }
    var issueDate by remember(initialDocument) { mutableStateOf(initialDocument.issueDate) }
    var expiryDate by remember(initialDocument) { mutableStateOf(initialDocument.expiryDate) }
    var reminderLeadDays by remember(initialDocument) { mutableStateOf(initialDocument.reminderLeadDays) }
    var notes by remember(initialDocument) { mutableStateOf(initialDocument.notes) }
    var confirmOcrChecked by remember(isOcrReviewMode) { mutableStateOf(true) }

    val activeCategoryGroup = remember(category) {
        DocumentCategoryGroup.fromCategoryName(category)
    }

    val fieldsState = remember(initialFields) {
        mutableStateListOf<DocumentFieldEntity>().apply { addAll(initialFields) }
    }

    var pickingBackSide by remember { mutableStateOf(false) }
    var editorCameraFile by remember { mutableStateOf<File?>(null) }
    var editorCameraUri by remember { mutableStateOf<Uri?>(null) }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAttachFrontOrBackUri(uri, pickingBackSide)
    }

    val editorCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = editorCameraUri
        val file = editorCameraFile
        if (success && uri != null && file != null && file.exists() && file.length() > 0L) {
            onAttachFrontOrBackUri(uri, pickingBackSide)
        }
    }

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
                .widthIn(max = 540.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeAreaBackButton(onClick = onCancel)
                Text(
                    text = if (isOcrReviewMode) "Review Extracted Details" else if (initialDocument.id == 0L) "Add Document Details" else "Edit Document",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isOcrReviewMode) {
                Surface(
                    shape = RoundedCornerShape(palette.cardCornerRadius),
                    color = palette.warningContainer,
                    border = BorderStroke(1.dp, palette.warning),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Verify extracted fields before saving",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF5C3B00)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Edit any fields below and confirm when accurate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A4F00)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Category Selection (15 Categories + Custom)
            Text(
                text = "1. Document Category",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.ink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allCategories.forEach { cat ->
                    FilterChip(
                        selected = category.equals(cat, ignoreCase = true),
                        onClick = {
                            category = cat
                            val grp = DocumentCategoryGroup.fromCategoryName(cat)
                            if (documentSubtype.isBlank() || documentSubtype !in grp.subtypes) {
                                documentSubtype = grp.subtypes.firstOrNull().orEmpty()
                            }
                        },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtype Selection (100+ Supported Document Subtypes)
            Text(
                text = "2. Document Type (${activeCategoryGroup.shortTitle})",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.ink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeCategoryGroup.subtypes.forEach { sub ->
                    FilterChip(
                        selected = documentSubtype.equals(sub, ignoreCase = true),
                        onClick = {
                            documentSubtype = sub
                            if (title.isBlank() || title in activeCategoryGroup.subtypes) {
                                title = sub
                            }
                        },
                        label = { Text(sub) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.ink,
                            selectedLabelColor = palette.surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Document Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_title_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Short Description (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country / Region") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Issuing Authority") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = issueDate,
                    onValueChange = { issueDate = it },
                    label = { Text("Issue Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (expiryDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Expiry Reminder Lead Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30, 60).forEach { days ->
                        FilterChip(
                            selected = reminderLeadDays == days,
                            onClick = { reminderLeadDays = days },
                            label = { Text("$days days before") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tagsCsv,
                onValueChange = { tagsCsv = it },
                label = { Text("Tags (comma-separated, e.g. official, tax, renewal)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // File Attachment & Native Camera Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SafeAreaOutlinedPillButton(
                    text = if (initialDocument.frontFileRef.isNotBlank()) {
                        "Replace File (${initialDocument.displayFormatBadge})"
                    } else {
                        "Attach File"
                    },
                    onClick = {
                        pickingBackSide = false
                        attachmentPicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.weight(1f)
                )
                SafeAreaOutlinedPillButton(
                    text = "Click Camera Photo",
                    leadingIcon = Icons.Default.CameraAlt,
                    onClick = {
                        pickingBackSide = false
                        runCatching {
                            val f = createCameraOutputFile(context)
                            val u = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            editorCameraFile = f
                            editorCameraUri = u
                            editorCameraLauncher.launch(u)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SafeAreaOutlinedPillButton(
                    text = if (initialDocument.backFileRef.isNotBlank()) "Replace Back File" else "Attach Back File",
                    onClick = {
                        pickingBackSide = true
                        attachmentPicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.weight(1f)
                )
                SafeAreaOutlinedPillButton(
                    text = "Camera Back Side",
                    leadingIcon = Icons.Default.PhotoCamera,
                    onClick = {
                        pickingBackSide = true
                        runCatching {
                            val f = createCameraOutputFile(context)
                            val u = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            editorCameraFile = f
                            editorCameraUri = u
                            editorCameraLauncher.launch(u)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Structured Fields Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Structured Fields",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.ink
                )
                TextButton(
                    onClick = {
                        fieldsState.add(
                            DocumentFieldEntity(
                                documentId = initialDocument.id,
                                label = "Field ${fieldsState.size + 1}",
                                value = "",
                                iconKey = "info"
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Field", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                fieldsState.forEachIndexed { idx, field ->
                    Surface(
                        shape = RoundedCornerShape(palette.cardCornerRadius),
                        color = palette.surface,
                        border = BorderStroke(palette.borderWidth, palette.border.copy(alpha = palette.borderAlpha))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = field.label,
                                    onValueChange = { fieldsState[idx] = field.copy(label = it) },
                                    label = { Text("Label") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.42f)
                                )
                                OutlinedTextField(
                                    value = field.value,
                                    onValueChange = { fieldsState[idx] = field.copy(value = it) },
                                    label = { Text("Value") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.58f)
                                )
                                IconButton(
                                    onClick = { fieldsState.removeAt(idx) }
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remove field",
                                        tint = palette.danger
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = field.isSensitive && field.isMaskedByDefault,
                                    onCheckedChange = { checked ->
                                        fieldsState[idx] = field.copy(
                                            isSensitive = checked,
                                            isMaskedByDefault = checked
                                        )
                                    }
                                )
                                Text(
                                    text = "Mask sensitive value by default",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Optional Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            if (isOcrReviewMode || fieldsState.any { it.source == FieldSource.OCR.name }) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirmOcrChecked = !confirmOcrChecked }
                ) {
                    Checkbox(
                        checked = confirmOcrChecked,
                        onCheckedChange = { confirmOcrChecked = it }
                    )
                    Text(
                        text = "I have reviewed and confirmed the extracted fields above.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SafeAreaPrimaryButton(
                text = "Save to Vault",
                leadingIcon = Icons.Default.Check,
                onClick = {
                    val updatedDoc = initialDocument.copy(
                        title = title.trim().ifBlank { documentSubtype.ifBlank { "Untitled Document" } },
                        category = category,
                        documentSubtype = documentSubtype,
                        description = description.trim(),
                        issuer = issuer.trim(),
                        country = country.trim(),
                        tagsCsv = tagsCsv.trim(),
                        issueDate = issueDate.trim(),
                        expiryDate = expiryDate.trim(),
                        reminderLeadDays = reminderLeadDays,
                        notes = notes.trim()
                    )
                    onSave(updatedDoc, fieldsState.toList(), confirmOcrChecked)
                },
                modifier = Modifier.testTag("save_document_button")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
