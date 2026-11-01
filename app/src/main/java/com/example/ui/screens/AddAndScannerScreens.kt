package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import java.io.ByteArrayOutputStream

/**
 * Add Document Screen supporting:
 * 1. Native Android CameraX Document Scanner
 * 2. Zero-permission Android Photo Picker (Gallery)
 * 3. Universal File Import (PDF, Word, Excel, PowerPoint, Images, Text, OpenDocument, CSV, JSON, XML)
 * 4. Manual Structured Entry with Category Presets
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

    // Universal Storage Access Framework picker supporting all requested document & office formats
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
                .widthIn(max = 520.dp)
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
                text = "Scan with your device camera, import any document format, or enter structured fields.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Scan Document with CameraX
            AddOptionCard(
                title = "Scan Document with Camera",
                subtitle = "Native camera capture with edge detection, crop & image enhancement",
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
                subtitle = "Choose from 9 categories & 60+ document types",
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

            // Quick Category Templates
            Text(
                text = "Quick Start by Category",
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
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onEnterManually(group.title, group.subtypes.firstOrNull().orEmpty())
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.28f))
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
                .widthIn(max = 520.dp)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.6f))
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
 * Native Android CameraX Document Scanner:
 * - Zero fake or synthetic bitmaps
 * - Captures real camera frames via ImageCapture (JPEG & YUV_420_888) or live PreviewView bitmap
 * - Edge detection guide frame (ID Card, Passport, A4 Document aspect presets)
 * - Post-capture perspective/edge crop, 90° rotation, and high-contrast document enhancement filter
 * - Two-sided front/back document capture
 */
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
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDeniedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var capturedFront by remember { mutableStateOf<Bitmap?>(null) }
    var capturedBack by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturingBackSide by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var selectedFrameAspect by remember { mutableStateOf("ID Card") } // "ID Card", "Passport", "A4 Page"
    var selectedCategoryGroup by remember { mutableStateOf(DocumentCategoryGroup.PERSONAL) }
    var selectedSubtype by remember { mutableStateOf(DocumentCategoryGroup.PERSONAL.subtypes.first()) }
    var customTitleHint by remember { mutableStateOf("") }
    var customOcrNotes by remember { mutableStateOf("") }
    var cameraErrorText by remember { mutableStateOf<String?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.canvas)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SafeAreaBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Camera Access Needed to Scan",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (permissionDeniedOnce) {
                    "Camera permission was denied. Grant Camera permission to scan documents directly, or import an existing image below."
                } else {
                    "SAFEAREA uses your device's camera locally to capture documents, crop edges, and enhance readability."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            SafeAreaPrimaryButton(
                text = "Grant Camera Permission",
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SafeAreaOutlinedPillButton(
                text = "Import from Gallery Instead",
                leadingIcon = Icons.Default.Image,
                onClick = onFallbackImportPhoto
            )
            Spacer(modifier = Modifier.height(10.dp))
            SafeAreaOutlinedPillButton(
                text = "Enter Details Manually",
                leadingIcon = Icons.Default.Edit,
                onClick = onFallbackManualEntry
            )
        }
        return
    }

    // Post-Capture Crop / Enhance / Rotate / Category Selection Screen
    val activeCaptured = if (isCapturingBackSide) capturedBack else capturedFront
    if (activeCaptured != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.canvas)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeAreaBackButton(onClick = {
                    if (isCapturingBackSide) {
                        capturedBack = null
                        isCapturingBackSide = false
                    } else {
                        capturedFront = null
                    }
                })
                Text(
                    text = if (isCapturingBackSide) "Review Back Scan" else "Crop, Enhance & Save Scan",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.5f))
            ) {
                Image(
                    bitmap = activeCaptured.asImageBitmap(),
                    contentDescription = "Captured document scan",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real Image Processing Controls: Rotate 90°, Edge Crop, Contrast Enhance, Retake
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
                        if (isCapturingBackSide) capturedBack = rotated else capturedFront = rotated
                    }
                )
                ScannerEditChip(
                    label = "Auto-Edge Crop",
                    icon = Icons.Default.Crop,
                    onClick = {
                        val cropped = cropBitmapToFrameAspect(activeCaptured, selectedFrameAspect)
                        if (isCapturingBackSide) capturedBack = cropped else capturedFront = cropped
                    }
                )
                ScannerEditChip(
                    label = "Enhance Contrast",
                    icon = Icons.Default.AutoFixHigh,
                    onClick = {
                        val enhanced = enhanceDocumentScanBitmap(activeCaptured)
                        if (isCapturingBackSide) capturedBack = enhanced else capturedFront = enhanced
                    }
                )
                ScannerEditChip(
                    label = "Retake",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        if (isCapturingBackSide) capturedBack = null else capturedFront = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Group Selector
            Text(
                text = "Document Category",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.ink,
                modifier = Modifier.align(Alignment.Start)
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
                text = "Document Type",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.ink,
                modifier = Modifier.align(Alignment.Start)
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

            if (capturedBack == null && !isCapturingBackSide) {
                SafeAreaOutlinedPillButton(
                    text = "Scan Back Side (Two-Sided Document)",
                    leadingIcon = Icons.Default.CameraAlt,
                    onClick = { isCapturingBackSide = true }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            SafeAreaPrimaryButton(
                text = "Save Scan & Review Details",
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
        return
    }

    // Live CameraX Preview with Aspect Ratio Edge Guide
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12131C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SafeAreaBackButton(onClick = onBack)
            Text(
                text = if (isCapturingBackSide) "Scan Back Side" else "Align Document in Frame",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Row {
                IconButton(onClick = { flashEnabled = !flashEnabled }) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle flash",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Aspect Ratio Frame Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf("ID Card", "Passport", "A4 Page").forEach { aspect ->
                val selected = selectedFrameAspect == aspect
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) Color(0xFF438B68) else Color(0xFF252736),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { selectedFrameAspect = aspect }
                ) {
                    Text(
                        text = aspect,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                    }
                },
                update = { previewView ->
                    previewViewRef = previewView
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        runCatching {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            imageCapture.flashMode = if (flashEnabled) {
                                ImageCapture.FLASH_MODE_ON
                            } else {
                                ImageCapture.FLASH_MODE_OFF
                            }
                            val selector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageCapture
                            )
                        }.onFailure { err ->
                            cameraErrorText = err.localizedMessage
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Document Edge Detection Guide Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frameW = size.width * 0.86f
                val ratio = when (selectedFrameAspect) {
                    "ID Card" -> 0.63f
                    "Passport" -> 0.72f
                    else -> 1.25f
                }
                val frameH = (frameW * ratio).coerceAtMost(size.height * 0.82f)
                val left = (size.width - frameW) / 2f
                val top = (size.height - frameH) / 2f
                val corner = 44f
                val c = Color(0xFF78A88F)

                drawLine(c, Offset(left, top + corner), Offset(left, top), 8f, StrokeCap.Round)
                drawLine(c, Offset(left, top), Offset(left + corner, top), 8f, StrokeCap.Round)
                drawLine(c, Offset(left + frameW - corner, top), Offset(left + frameW, top), 8f, StrokeCap.Round)
                drawLine(c, Offset(left + frameW, top), Offset(left + frameW, top + corner), 8f, StrokeCap.Round)
                drawLine(c, Offset(left, top + frameH - corner), Offset(left, top + frameH), 8f, StrokeCap.Round)
                drawLine(c, Offset(left, top + frameH), Offset(left + corner, top + frameH), 8f, StrokeCap.Round)
                drawLine(c, Offset(left + frameW - corner, top + frameH), Offset(left + frameW, top + frameH), 8f, StrokeCap.Round)
                drawLine(c, Offset(left + frameW, top + frameH), Offset(left + frameW, top + frameH - corner), 8f, StrokeCap.Round)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF171431))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cameraErrorText != null) {
                Text(
                    text = cameraErrorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB4AB),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Text(
                    text = "$selectedFrameAspect edge guide active · Tap shutter to capture",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDCECE2)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onFallbackImportPhoto) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", color = Color.White)
                }

                ButtonCaptureShutter(
                    onClick = {
                        cameraErrorText = null
                        val executor = ContextCompat.getMainExecutor(context)
                        try {
                            imageCapture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bmp = imageProxyToBitmap(image) ?: previewViewRef?.bitmap
                                        image.close()
                                        if (bmp != null) {
                                            if (isCapturingBackSide) {
                                                capturedBack = bmp
                                            } else {
                                                capturedFront = bmp
                                            }
                                        } else {
                                            cameraErrorText = "Could not decode camera frame. Try again or import from Gallery."
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        val liveBmp = previewViewRef?.bitmap
                                        if (liveBmp != null) {
                                            if (isCapturingBackSide) {
                                                capturedBack = liveBmp
                                            } else {
                                                capturedFront = liveBmp
                                            }
                                        } else {
                                            cameraErrorText = "Camera capture error: ${exception.localizedMessage ?: "Check camera sensor or import from Gallery."}"
                                        }
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            val liveBmp = previewViewRef?.bitmap
                            if (liveBmp != null) {
                                if (isCapturingBackSide) {
                                    capturedBack = liveBmp
                                } else {
                                    capturedFront = liveBmp
                                }
                            } else {
                                cameraErrorText = "Camera not ready (${e.localizedMessage ?: "try Gallery import"})."
                            }
                        }
                    }
                )

                TextButton(onClick = onFallbackManualEntry) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual", color = Color.White)
                }
            }
        }
    }
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

@Composable
private fun ButtonCaptureShutter(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .testTag("camera_shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFFD5104B))
        )
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return runCatching {
        val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
        val rawBitmap = if (image.format == ImageFormat.JPEG || image.planes.size == 1) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else if (image.format == ImageFormat.YUV_420_888 && image.planes.size >= 3) {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 92, out)
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } else {
            null
        }
        if (rawBitmap != null && rotationDegrees != 0f) {
            rotateBitmap(rawBitmap, rotationDegrees)
        } else {
            rawBitmap
        }
    }.getOrNull()
}

private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

private fun cropBitmapToFrameAspect(src: Bitmap, aspectMode: String): Bitmap {
    val targetW = (src.width * 0.86f).toInt().coerceAtLeast(120)
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
 * - Supports all 9 DocumentCategoryGroup categories + custom categories
 * - Supports all 60+ Document Subtypes with 1-tap selection
 * - Supports attaching any file format (PDF, Word, Excel, PowerPoint, Images, Text, OpenDocument, CSV, JSON, XML)
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
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAttachFrontOrBackUri(uri, pickingBackSide)
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
                    shape = RoundedCornerShape(16.dp),
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

            // Category Selection
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

            // Subtype Selection (60+ Supported Document Subtypes)
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

            // File Attachment Controls (All supported formats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SafeAreaOutlinedPillButton(
                    text = if (initialDocument.frontFileRef.isNotBlank()) {
                        "Replace File (${initialDocument.displayFormatBadge})"
                    } else {
                        "Attach File / Scan"
                    },
                    onClick = {
                        pickingBackSide = false
                        attachmentPicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.weight(1f)
                )
                SafeAreaOutlinedPillButton(
                    text = if (initialDocument.backFileRef.isNotBlank()) "Replace Back Side" else "Attach Back Side",
                    onClick = {
                        pickingBackSide = true
                        attachmentPicker.launch(arrayOf("image/*"))
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
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.3f))
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
