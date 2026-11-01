package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AuditEventEntity
import com.example.data.DocumentWithFields
import com.example.ui.VaultUiState
import com.example.ui.components.SafeAreaOutlinedPillButton
import com.example.ui.components.SafeAreaPrimaryButton
import com.example.ui.daysUntilExpiry
import com.example.ui.theme.AppearanceMode
import com.example.ui.theme.LocalSafeAreaPalette
import com.example.ui.theme.VaultThemeId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun buildRequiredPermissionsArray(): Array<String> {
    val list = mutableListOf(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
        list.add(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        list.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }
    return list.toTypedArray()
}

/**
 * Built-in Vault Tools Screen implementing Section 12 of the PRD:
 * Document Scanner, Field Copier, Expiry Tracker & Calculator, QR/MRZ Inspector,
 * Storage Manager & Duplicate Detector, and Secure Export.
 */
@Composable
fun ToolsScreen(
    uiState: VaultUiState,
    documents: List<DocumentWithFields>,
    onOpenScanner: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onCopyCustomSelection: (String, String) -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    var activeToolModal by remember { mutableStateOf<String?>(null) }

    val duplicatePairs = remember(documents) {
        val groupedByTitle = documents.groupBy { it.document.title.trim().lowercase() }
            .filter { it.value.size > 1 }
        groupedByTitle.keys.toList()
    }

    val expiringDocs = remember(documents) {
        documents.filter { it.document.expiryDate.isNotBlank() }
            .sortedBy { it.document.expiryDate }
    }

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
                .widthIn(max = 540.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Vault Tools",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink
            )
            Text(
                text = "On-device utilities for scanning, copying fields, tracking expiries, and managing encrypted storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            ToolActionCard(
                title = "Document Scanner & Crop",
                subtitle = "Open CameraX scanner with edge frame, rotate, crop & OCR review",
                icon = Icons.Default.CameraAlt,
                badge = "CameraX",
                onClick = onOpenScanner
            )

            ToolActionCard(
                title = "Multi-Field Copier",
                subtitle = "Select specific fields across your documents & copy with 30s auto-clear",
                icon = Icons.Default.ContentCopy,
                badge = "${documents.sumOf { it.fields.size }} fields",
                onClick = { activeToolModal = "FIELD_COPIER" }
            )

            ToolActionCard(
                title = "Expiry Tracker & Calculator",
                subtitle = "View expiring IDs/passports and calculate renewal reminder dates",
                icon = Icons.Default.EventRepeat,
                badge = "${expiringDocs.size} tracked",
                onClick = { activeToolModal = "EXPIRY_TRACKER" }
            )

            ToolActionCard(
                title = "OCR & MRZ / QR Inspector",
                subtitle = "Parse machine-readable ID zones or QR text locally without network upload",
                icon = Icons.Default.QrCodeScanner,
                badge = "Local",
                onClick = { activeToolModal = "QR_INSPECTOR" }
            )

            ToolActionCard(
                title = "Storage Manager & Duplicate Check",
                subtitle = "Vault: ${formatBytes(uiState.vaultStorageBytes)} · Cache: ${formatBytes(uiState.cacheStorageBytes)} · ${duplicatePairs.size} duplicate group(s)",
                icon = Icons.Default.Storage,
                badge = "${documents.size} docs",
                onClick = { activeToolModal = "STORAGE_MANAGER" }
            )

            ToolActionCard(
                title = "Secure Backup & Export",
                subtitle = "Export a user-controlled vault metadata archive with privacy warning",
                icon = Icons.Default.Backup,
                badge = "JSON",
                onClick = onExportBackup
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (activeToolModal == "FIELD_COPIER") {
        val selectedFieldKeys = remember { mutableStateMapOf<String, Boolean>() }
        AlertDialog(
            onDismissRequest = { activeToolModal = null },
            containerColor = palette.surface,
            title = { Text("Multi-Field Copier") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select the exact fields you want to copy together. Clipboard auto-clears after 30 seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                    documents.forEach { docWithFields ->
                        Text(
                            text = docWithFields.document.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        docWithFields.fields.forEach { field ->
                            val key = "${docWithFields.document.title} - ${field.label}: ${field.value}"
                            val checked = selectedFieldKeys[key] == true
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFieldKeys[key] = !checked }
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { selectedFieldKeys[key] = it }
                                )
                                Text(
                                    text = "${field.label}: ${field.value}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.ink
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosen = selectedFieldKeys.filterValues { it }.keys.toList()
                        if (chosen.isNotEmpty()) {
                            onCopyCustomSelection(
                                "${chosen.size} Selected Fields",
                                chosen.joinToString("\n")
                            )
                        }
                        activeToolModal = null
                    }
                ) {
                    Text("Copy Selected", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeToolModal = null }) {
                    Text("Close", color = palette.inkMuted)
                }
            }
        )
    }

    if (activeToolModal == "EXPIRY_TRACKER") {
        var issueYearInput by remember { mutableStateOf("2024") }
        var validityYearsInput by remember { mutableIntStateOf(5) }
        val calculatedExpiry = remember(issueYearInput, validityYearsInput) {
            val baseYear = issueYearInput.toIntOrNull() ?: 2024
            "${baseYear + validityYearsInput}-01-01 (Reminder: ${baseYear + validityYearsInput - 1}-12-01)"
        }

        AlertDialog(
            onDismissRequest = { activeToolModal = null },
            containerColor = palette.surface,
            title = { Text("Expiry Tracker & Calculator") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tracked Document Expiries:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink
                    )
                    if (expiringDocs.isEmpty()) {
                        Text("No documents have an expiry date set.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        expiringDocs.forEach { item ->
                            val d = daysUntilExpiry(item.document.expiryDate) ?: 0L
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (d <= 45) palette.warningContainer else palette.successContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeToolModal = null
                                        onOpenDocument(item.document.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.document.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = palette.ink
                                    )
                                    Text(
                                        text = "${item.document.expiryDate} (${if (d < 0) "Expired" else "${d}d left"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.ink
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Quick Validity Calculator",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink
                    )
                    OutlinedTextField(
                        value = issueYearInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) issueYearInput = it },
                        label = { Text("Issue Year (YYYY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 5, 10).forEach { yrs ->
                            FilterChip(
                                selected = validityYearsInput == yrs,
                                onClick = { validityYearsInput = yrs },
                                label = { Text("+$yrs yrs") }
                            )
                        }
                    }
                    Text(
                        text = "Estimated Renewal: $calculatedExpiry",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeToolModal = null }) {
                    Text("Done", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (activeToolModal == "QR_INSPECTOR") {
        var rawCodePayload by remember { mutableStateOf("IDNPL1234567890<<9805218M3301095NPL<<<<<<04\nSHAH<<RAHUL<<<<<<<<<<<<<<<<<<<<<<<<<<<<") }
        AlertDialog(
            onDismissRequest = { activeToolModal = null },
            containerColor = palette.surface,
            title = { Text("QR / MRZ Code Inspector") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Inspect decoded QR/barcode or Passport/ID Machine Readable Zone (MRZ) strings locally. Note: Decoded content is never assumed authentic or government-verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                    OutlinedTextField(
                        value = rawCodePayload,
                        onValueChange = { rawCodePayload = it },
                        label = { Text("Decoded QR / MRZ Payload") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.canvas
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Parsed Local Summary:",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.ink
                            )
                            Text(
                                text = "Length: ${rawCodePayload.length} chars · Contains MRZ chevrons: ${if (rawCodePayload.contains("<<")) "Yes" else "No"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkMuted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCopyCustomSelection("QR/MRZ Payload", rawCodePayload)
                        activeToolModal = null
                    }
                ) {
                    Text("Copy Payload", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeToolModal = null }) {
                    Text("Close", color = palette.inkMuted)
                }
            }
        )
    }

    if (activeToolModal == "STORAGE_MANAGER") {
        val pdfCount = documents.count { it.document.mimeType.contains("pdf", ignoreCase = true) }
        val imageCount = documents.size - pdfCount
        AlertDialog(
            onDismissRequest = { activeToolModal = null },
            containerColor = palette.surface,
            title = { Text("Storage Manager") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Encrypted Vault Files: ${formatBytes(uiState.vaultStorageBytes)}")
                    Text("• Temporary Export Cache: ${formatBytes(uiState.cacheStorageBytes)}")
                    Text("• Breakdown: $imageCount ID/Image record(s), $pdfCount PDF document(s)")
                    Text(
                        text = if (duplicatePairs.isEmpty()) {
                            "• Duplicate Check: No duplicate document titles detected."
                        } else {
                            "• Duplicate Candidates Found: ${duplicatePairs.joinToString(", ")}"
                        },
                        color = if (duplicatePairs.isEmpty()) palette.success else Color(0xFF8A5A00),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Clearing cache removes temporary shared copies without touching your original encrypted documents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearCache()
                        activeToolModal = null
                    }
                ) {
                    Text("Clear Temp Cache", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeToolModal = null }) {
                    Text("Close", color = palette.inkMuted)
                }
            }
        )
    }
}

@Composable
private fun ToolActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    onClick: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.canvas),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.successContainer
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onSuccessContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted
                )
            }
        }
    }
}

/**
 * Profile & Settings Screen implementing:
 * - Appearance: Light / Dark / System toggle + 10 Pastel & Dark Theme Picker
 * - Typography & Font Options: Use System Font option + Font Size / Scale selector
 * - Permissions & Full Access: Request All Permissions (Camera, Notifications, Gallery/Media) + Grant Full Folder/Files Access
 * - Security, Lock, Storage, Backup & Delete All Data
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    uiState: VaultUiState,
    auditEvents: List<AuditEventEntity>,
    onSelectTheme: (VaultThemeId) -> Unit,
    onSelectAppearanceMode: (AppearanceMode) -> Unit,
    onToggleUseSystemFont: (Boolean) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onPermissionsUpdated: (Map<String, Boolean>) -> Unit,
    onFolderAccessGranted: (String) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onConfigureLockMethod: () -> Unit,
    onLockVaultNow: () -> Unit,
    onChangeAutoLockTimeout: (Int) -> Unit,
    onToggleLockOnBackground: (Boolean) -> Unit,
    onToggleHideInAppSwitcher: (Boolean) -> Unit,
    onToggleExpiryNotifications: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onWipeAllData: () -> Unit
) {
    val palette = LocalSafeAreaPalette.current
    val context = LocalContext.current

    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showAuditLogDialog by remember { mutableStateOf(false) }

    // Multi-permission launcher for Camera, Notifications, and Gallery/Media/Storage
    val requestAllPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        onPermissionsUpdated(resultMap)
    }

    // Full Folder / Files Access via Storage Access Framework OpenDocumentTree
    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onFolderAccessGranted(treeUri.toString())
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onToggleExpiryNotifications(true)
        onPermissionsUpdated(mapOf("POST_NOTIFICATIONS" to true))
    }

    val hasCameraPerm = remember(uiState.permissionsRefreshTick) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotifPerm = remember(uiState.permissionsRefreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    val hasGalleryPerm = remember(uiState.permissionsRefreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED)
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

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
                .widthIn(max = 540.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Profile & Vault Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink
            )

            // 1. Permissions, Gallery, Files & Notifications Full Access Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = palette.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permissions, Gallery & Files Access",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.ink
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionStatusRow("Camera Access (Scanner)", hasCameraPerm)
                    PermissionStatusRow("Notifications (Expiry Alerts)", hasNotifPerm)
                    PermissionStatusRow("Gallery / Photos Access", hasGalleryPerm)
                    PermissionStatusRow(
                        "Files & Folder Access",
                        uiState.grantedFolderUri.isNotBlank(),
                        customStatus = if (uiState.grantedFolderUri.isNotBlank()) "Full Directory Granted" else "Tap below to grant folder"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SafeAreaPrimaryButton(
                        text = "Grant All Permissions (Camera, Notifs & Gallery)",
                        leadingIcon = Icons.Default.CheckCircle,
                        onClick = {
                            requestAllPermissionsLauncher.launch(buildRequiredPermissionsArray())
                        },
                        modifier = Modifier.testTag("grant_all_permissions_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SafeAreaOutlinedPillButton(
                            text = "Full Files / Folder",
                            leadingIcon = Icons.Default.FolderOpen,
                            onClick = { folderTreeLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("grant_folder_access_button")
                        )
                        SafeAreaOutlinedPillButton(
                            text = "App Settings",
                            onClick = {
                                runCatching {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Document Expiry Reminders", style = MaterialTheme.typography.bodyMedium, color = palette.ink)
                            Text("Local alerts for expiring documents", style = MaterialTheme.typography.bodySmall, color = palette.inkMuted)
                        }
                        Switch(
                            checked = uiState.expiryNotificationsEnabled,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleExpiryNotifications(wantEnabled)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary)
                        )
                    }
                }
            }

            // 2. Typography & Font Options ("Use System Font" + Font Size Scale)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = palette.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Typography & Font Options",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.ink
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use System Font",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.ink
                            )
                            Text(
                                text = if (uiState.useSystemFont) {
                                    "Using Android device system font"
                                } else {
                                    "Using SAFEAREA editorial font (Plus Jakarta Sans + Devanagari)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkMuted
                            )
                        }
                        Switch(
                            checked = uiState.useSystemFont,
                            onCheckedChange = onToggleUseSystemFont,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.primary
                            ),
                            modifier = Modifier.testTag("use_system_font_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented Font Family selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.canvas)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            false to "Editorial Font",
                            true to "System Font"
                        ).forEach { (isSys, label) ->
                            val selected = uiState.useSystemFont == isSys
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) palette.primary else Color.Transparent)
                                    .clickable { onToggleUseSystemFont(isSys) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else palette.ink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Text Size & Scaling",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.inkMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0.90f to "Small",
                            1.00f to "Default",
                            1.12f to "Large",
                            1.22f to "XL"
                        ).forEach { (scaleVal, label) ->
                            val selected = kotlin.math.abs(uiState.fontScale - scaleVal) < 0.04f
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectFontScale(scaleVal) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 3. Appearance & Coordinated Pastel Theme Picker (Section 3 of PRD)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = palette.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance & Theme Library",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.ink
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Display Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.inkMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.canvas)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppearanceMode.entries.forEach { mode ->
                            val selected = uiState.appearanceMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) palette.primary else Color.Transparent)
                                    .clickable { onSelectAppearanceMode(mode) }
                                    .padding(vertical = 8.dp)
                                    .testTag("appearance_mode_${mode.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else palette.ink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Pastel & Dark Theme Presets (10 Themes)",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.inkMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2
                    ) {
                        VaultThemeId.entries.forEach { theme ->
                            val isSelected = uiState.selectedTheme == theme
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectTheme(theme) }
                                    .testTag("theme_preset_${theme.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(theme.canvasHex),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(theme.primaryHex) else Color(theme.inkHex).copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(theme.primaryHex))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(theme.secondaryHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = theme.displayName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = Color(theme.inkHex),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected theme",
                                            tint = Color(theme.primaryHex),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Security & App Lock Settings
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = palette.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security & Vault Lock",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.ink
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable App Lock", style = MaterialTheme.typography.bodyLarge, color = palette.ink)
                            Text(
                                "Current method: ${uiState.primaryLockMethod.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkMuted
                            )
                        }
                        Switch(
                            checked = uiState.appLockEnabled,
                            onCheckedChange = onToggleAppLock,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Lock When Backgrounded", style = MaterialTheme.typography.bodyMedium, color = palette.ink)
                            Text("Lock immediately when switching apps", style = MaterialTheme.typography.bodySmall, color = palette.inkMuted)
                        }
                        Switch(
                            checked = uiState.lockOnBackground,
                            onCheckedChange = onToggleLockOnBackground,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inactivity Auto-Lock Timer",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.inkMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15 to "15s", 60 to "1m", 300 to "5m", 900 to "15m").forEach { (sec, label) ->
                            FilterChip(
                                selected = uiState.autoLockTimeoutSeconds == sec,
                                onClick = { onChangeAutoLockTimeout(sec) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SafeAreaOutlinedPillButton(
                            text = "Change Lock / PIN",
                            onClick = onConfigureLockMethod,
                            modifier = Modifier.weight(1f)
                        )
                        SafeAreaPrimaryButton(
                            text = "Lock Vault",
                            leadingIcon = Icons.Default.Lock,
                            onClick = onLockVaultNow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Privacy, Storage & Destructive Data Controls
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.2.dp, palette.border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Data Location & Privacy Controls",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.ink
                    )
                    Text(
                        text = "Storage Location: App-private storage + Android Keystore (AES-256-GCM).\nUsed Space: ${formatBytes(uiState.vaultStorageBytes)} · Temp Cache: ${formatBytes(uiState.cacheStorageBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SafeAreaOutlinedPillButton(
                            text = "Export Backup",
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f)
                        )
                        SafeAreaOutlinedPillButton(
                            text = "Security Log (${auditEvents.size})",
                            onClick = { showAuditLogDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TextButton(
                        onClick = { showWipeConfirmDialog = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("delete_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = palette.danger)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete All Local Vault Data",
                            color = palette.danger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "SAFEAREA v1.0 · Personal Document Vault · On-Device Only",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAuditLogDialog) {
        AlertDialog(
            onDismissRequest = { showAuditLogDialog = false },
            containerColor = palette.surface,
            title = { Text("Privacy-Safe Security Audit Log") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Raw document values and secrets are never logged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                    auditEvents.forEach { ev ->
                        val ts = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ev.timestamp))
                        Text(
                            text = "[$ts] ${ev.eventType}: ${ev.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditLogDialog = false }) {
                    Text("Close", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            containerColor = palette.surface,
            title = { Text("Delete All Local Vault Data?") },
            text = {
                Text(
                    "This action will permanently delete all stored documents, encrypted scans, PDFs, structured fields, and PIN/Pattern credentials from this device. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirmDialog = false
                        onWipeAllData()
                    }
                ) {
                    Text("Delete Everything", color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Cancel", color = palette.inkMuted)
                }
            }
        )
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    isGranted: Boolean,
    customStatus: String? = null
) {
    val palette = LocalSafeAreaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.ink
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGranted) palette.successContainer else palette.warningContainer
        ) {
            Text(
                text = customStatus ?: if (isGranted) "Granted" else "Not Granted",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isGranted) palette.success else Color(0xFF8A5A00),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
